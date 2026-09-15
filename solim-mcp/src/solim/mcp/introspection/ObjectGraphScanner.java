package solim.mcp.introspection;

import arc.scene.Element;
import arc.util.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import solim.signal.Computed;
import solim.signal.Effect;
import solim.signal.Signal;
import solim.signal.Subscription;
import java.lang.reflect.Array;

/**
 * Cycle-safe reflective scan of an object graph for reactive Solim instances.
 *
 * <p>Traversal rules:
 *
 * <ul>
 *   <li>Identity tracking prevents infinite loops on cyclic graphs.
 *   <li>Primitives, strings, numbers, enums, and classes are leaves.
 *   <li>{@link Element} instances are shallow-scanned: their own reactive fields are recorded but
 *       the graph is never descended into (Arc internals are explosive and the element subtree is
 *       walked separately by the tree inspector).
 *   <li>Collections, arrays, and iterables are descended into.
 *   <li>Everything else is descended into field by field.
 * </ul>
 */
public final class ObjectGraphScanner {
	private final int maxDepth;
	private final Map<Object, Boolean> visited = new IdentityHashMap<>();
	private final List<ReactiveRef> found = new ArrayList<>();

	private ObjectGraphScanner(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	/** Scans the given root object and returns every reachable reactive instance. */
	public static List<ReactiveRef> scan(@Nullable Object root, int maxDepth) {
		return scan(root, null, null, maxDepth);
	}

	/**
	 * Scans an element and its immediate reactive fields without descending into other {@link
	 * Element} objects. The small depth budget allows the user-object graph ({@code
	 * Element.userObject}) to be walked a few levels deep.
	 */
	public static List<ReactiveRef> scanElement(@Nullable Element element) {
		ObjectGraphScanner s = new ObjectGraphScanner(4);
		if (element == null) return s.found;
		s.visit(element, 0, elementName(element), elementName(element));
		return s.found;
	}

	private static List<ReactiveRef> scan(@Nullable Object root, @Nullable String sourceElement,
			@Nullable String fieldName, int depth) {
		ObjectGraphScanner s = new ObjectGraphScanner(depth);
		if (root == null) return s.found;
		s.visit(root, 0, sourceElement != null ? sourceElement : "<root>", fieldName != null ? fieldName : "root");
		return s.found;
	}

	private static String elementName(Element element) {
		return element.name != null && !element.name.isEmpty() ? element.name : element.getClass().getSimpleName();
	}

	private void visit(Object value, int depth, String location, @Nullable String sourceElement) {
		if (value == null || depth > maxDepth || isLeaf(value)) return;
		if (visited.put(value, Boolean.TRUE) != null) return;

		ReactiveKind kind = kindOf(value);
		if (kind != null) {
			found.add(new ReactiveRef(value, kind, location, sourceElement, locationField(location)));
			// still descend into reactive instances so nested sources are discovered
		}

		if (value instanceof Element) {
			shallowScanElement((Element) value, depth, location);
			return;
		}

		if (value instanceof Iterable) {
			int i = 0;
			for (Object child : (Iterable<?>) value) {
				visit(child, depth + 1, location + "/[" + i++ + "]", sourceElement);
			}
			return;
		}

		if (value.getClass().isArray()) {
			int len = Array.getLength(value);
			for (int i = 0; i < len; i++) {
				visit(Array.get(value, i), depth + 1, location + "/[" + i + "]", sourceElement);
			}
			return;
		}

		if (value instanceof Map) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
				visit(entry.getKey(), depth + 1, location + "/key", sourceElement);
				visit(entry.getValue(), depth + 1, location + "/value", sourceElement);
			}
			return;
		}

		scanFields(value, depth, location, sourceElement);
	}

	private void shallowScanElement(Element element, int depth, String location) {
		for (Field f : instanceFields(element.getClass())) {
			Object fieldValue = ReflectAccess.read(f, element);
			if (fieldValue == null) continue;
			String fieldName = f.getName();
			if (f.getType() == Element.class || Element.class.isAssignableFrom(f.getType())) continue;
			ReactiveKind fieldKind = kindOf(fieldValue);
			if (fieldKind != null) {
				found.add(new ReactiveRef(fieldValue, fieldKind,
					location + "/" + fieldName, elementName(element), fieldName));
			}
		}
		Object userObject = element.userObject;
		if (userObject != null && !isLeaf(userObject)) {
			visit(userObject, depth + 1, location + "/userObject", elementName(element));
		}
	}

	private void scanFields(Object value, int depth, String location, @Nullable String sourceElement) {
		for (Field f : instanceFields(value.getClass())) {
			Object fieldValue = ReflectAccess.read(f, value);
			if (fieldValue == null || isLeaf(fieldValue)) continue;
			visit(fieldValue, depth + 1, location + "/" + f.getName(), sourceElement);
		}
	}

	private static List<Field> instanceFields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
			Collections.addAll(fields, c.getDeclaredFields());
		}
		return fields;
	}

	private static String locationField(String location) {
		int idx = location.lastIndexOf('/');
		return idx >= 0 ? location.substring(idx + 1) : location;
	}

	@Nullable
	private static ReactiveKind kindOf(Object value) {
		if (value instanceof Signal) return ReactiveKind.SIGNAL;
		if (value instanceof Computed) return ReactiveKind.COMPUTED;
		if (value instanceof Effect) return ReactiveKind.EFFECT;
		if (value instanceof Subscription) return ReactiveKind.SUBSCRIPTION;
		return null;
	}

	private static boolean isLeaf(Object value) {
		return value instanceof CharSequence
			|| value instanceof Number
			|| value instanceof Boolean
			|| value instanceof Character
			|| value instanceof Class
			|| value.getClass().isEnum()
			|| value.getClass().isPrimitive();
	}
}
