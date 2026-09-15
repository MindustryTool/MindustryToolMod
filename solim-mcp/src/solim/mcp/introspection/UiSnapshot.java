package solim.mcp.introspection;

import arc.Core;
import arc.scene.Element;
import arc.scene.Group;
import arc.util.Log;
import arc.util.Nullable;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Captures a read-only snapshot of the current Solim/Arc UI state.
 *
 * <p>Assembled from:
 *
 * <ul>
 *   <li>the Arc element tree (public Scene2D API),
 *   <li>reactive instances discovered by reflectively scanning each element plus the ambient static
 *       contexts ({@code ComponentContext}, {@code ParentStack}, {@code ReactiveContext}),
 *   <li>layout metrics for each element.
 * </ul>
 *
 * <p>{@link #capture(Element)} runs the walk on the UI thread inside Mindustry, and falls back to
 * direct execution when no {@code Core.app} is present (headless tests).
 */
public final class UiSnapshot {
	public final ObjectNode tree;
	public final List<ReactiveRef> reactiveRefs;

	private UiSnapshot(ObjectNode tree, List<ReactiveRef> reactiveRefs) {
		this.tree = tree;
		this.reactiveRefs = Collections.unmodifiableList(reactiveRefs);
	}

	/** Captures a snapshot rooted at the given element (pass {@code null} for the scene root). */
	public static UiSnapshot capture(@Nullable Element root) {
		UiResult result = runOnUiThread(() -> captureDirect(root));
		return new UiSnapshot(result.tree, result.refs);
	}

	/**
	 * Captures the component tree only, scoped to a matching element subtree when {@code target} is
	 * non-empty. Matching is by element name, simple class name, or fully qualified class name.
	 */
	public static ObjectNode tree(@Nullable Element root, @Nullable String target) {
		return runOnUiThread(() -> {
			if (target == null || target.isEmpty()) {
				Element rootEl = root != null ? root : (Core.scene != null ? Core.scene.root : null);
				return ComponentTreeInspector.tree(rootEl, 32);
			}
			Element scoped = find(root, target);
			if (scoped == null) {
				throw new IllegalArgumentException("No element matches '" + target + "'");
			}
			return ComponentTreeInspector.tree(scoped, 32);
		});
	}

	/** All discovered signals/computeds with their live values. */
	public ArrayNode signalsJson() {
		ArrayNode out = JsonNodeFactory.instance.arrayNode();
		for (ReactiveRef ref : reactiveRefs) {
			out.add(SignalIntrospector.snapshot(ref, JsonNodeFactory.instance.objectNode()));
		}
		return out;
	}

	/** All discovered reactive effects, presented as bindings. */
	public ArrayNode bindingsJson() {
		ArrayNode out = JsonNodeFactory.instance.arrayNode();
		for (ReactiveRef ref : reactiveRefs) {
			if (ref.kind == ReactiveKind.EFFECT) {
				out.add(BindingInspector.snapshot(ref));
			}
		}
		return out;
	}

	/** Deterministic signature used to detect changes for subscription notifications. */
	public String signature() {
		StringBuilder sb = new StringBuilder();
		sb.append(tree.toString());
		for (ReactiveRef ref : reactiveRefs) {
			sb.append('|').append(ref.kind).append(':').append(ref.location);
		}
		return sb.toString();
	}

	/** Depth-first lookup of an element by name, simple class name, or fully qualified class name. */
	public static @Nullable Element find(@Nullable Element root, String target) {
		return findElement(root, target, 0, new IdentityHashMap<>());
	}

	/** Thread-safe variant of {@link #find} that runs on the UI thread when a game is present. */
	public static @Nullable Element lookup(@Nullable Element root, String target) {
		return runOnUiThread(() -> find(root, target));
	}

	private static @Nullable Element findElement(@Nullable Element element, String target, int depth,
			Map<Element, Boolean> visited) {
		if (element == null || visited.put(element, Boolean.TRUE) != null || depth > 32) return null;
		String name = element.name != null ? element.name : element.getClass().getSimpleName();
		if (target.equals(name) || target.equals(element.getClass().getSimpleName())
			|| target.equals(element.getClass().getName())) {
			return element;
		}
		if (element instanceof Group) {
			for (Element child : ((Group) element).getChildren()) {
				Element found = findElement(child, target, depth + 1, visited);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static class UiResult {
		final ObjectNode tree;
		final List<ReactiveRef> refs;

		UiResult(ObjectNode tree, List<ReactiveRef> refs) {
			this.tree = tree;
			this.refs = refs;
		}
	}

	private static UiResult captureDirect(@Nullable Element root) {
		Element rootEl = root;
		if (rootEl == null && Core.scene != null) {
			rootEl = Core.scene.root;
		}
		List<ReactiveRef> refs = new ArrayList<>();
		ObjectNode tree = ComponentTreeInspector.tree(rootEl, 32);
		scanElementRecursive(rootEl, refs);
		scanAmbientContexts(refs);
		return new UiResult(tree, refs);
	}

	private static void scanElementRecursive(@Nullable Element element, List<ReactiveRef> refs) {
		if (element == null) return;
		refs.addAll(ObjectGraphScanner.scanElement(element));
		if (element instanceof Group) {
			for (Element child : ((Group) element).getChildren()) {
				scanElementRecursive(child, refs);
			}
		}
	}

	/** Scans ambient static context stacks for in-flight components, computeds, and effects. */
	private static void scanAmbientContexts(List<ReactiveRef> refs) {
		scanDeque("solim.runtime.ComponentContext", "stack", refs);
		scanDeque("solim.runtime.ReactiveContext", "stack", refs);
		scanPendingComponents(refs);
	}

	private static void scanDeque(String className, String fieldName, List<ReactiveRef> refs) {
		try {
			Class<?> type = Class.forName(className);
			Object fieldValue = ReflectAccess.read(ReflectAccess.field(type, fieldName), null);
			if (fieldValue instanceof Iterable) {
				for (Object entry : (Iterable<?>) fieldValue) {
					if (entry != null) {
						refs.addAll(ObjectGraphScanner.scan(entry, 3));
					}
				}
			}
		} catch (ClassNotFoundException | RuntimeException e) {
			Log.warn("[solim-mcp] ambient context {0} unavailable; skipped", className);
		}
	}

	private static void scanPendingComponents(List<ReactiveRef> refs) {
		try {
			Class<?> type = Class.forName("solim.runtime.ParentStack");
			Object value = ReflectAccess.read(ReflectAccess.field(type, "pendingComponents"), null);
			if (value instanceof Map) {
				for (Object component : ((Map<?, ?>) value).values()) {
					if (component instanceof Iterable) {
						for (Object pending : (Iterable<?>) component) {
							refs.addAll(ObjectGraphScanner.scan(pending, 3));
						}
					}
				}
			}
		} catch (ClassNotFoundException | RuntimeException e) {
			Log.warn("[solim-mcp] ambient context {0} unavailable; skipped", "solim.runtime.ParentStack");
		}
	}

	private static <T> T runOnUiThread(final Callable<T> action) {
		try {
			if (Core.app == null) {
				return action.call();
			}
			final CountDownLatch latch = new CountDownLatch(1);
			final AtomicReference<T> result = new AtomicReference<>();
			final AtomicReference<Throwable> error = new AtomicReference<>();
			Core.app.post(() -> {
				try {
					result.set(action.call());
				} catch (Throwable t) {
					error.set(t);
				} finally {
					latch.countDown();
				}
			});
			if (!latch.await(5, TimeUnit.SECONDS)) {
				throw new RuntimeException("Timed out waiting for UI thread to capture snapshot");
			}
			if (error.get() != null) {
				Throwable t = error.get();
				if (t instanceof RuntimeException) throw (RuntimeException) t;
				if (t instanceof Error) throw (Error) t;
				throw new RuntimeException(t);
			}
			return result.get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to capture UI snapshot", e);
		}
	}
}