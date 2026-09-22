package solim.runtime;

import arc.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import solim.core.Component;

/**
 * Internal multi-root ownership unit for dynamic structural components with void factories.
 *
 * <p>Wraps a list of components captured from a void factory block into a single ownable and
 * disposable unit. A fragment with zero roots is empty and is treated by mount sites exactly
 * like a value factory returning {@code null} (collapse). A fragment with one or more roots
 * mounts every root's element, each with its own cell configuration.
 *
 * <p>This is an internal engine type: it is not part of the public Solim UI API.
 */
public final class Fragment implements Component {
	private List<Component> roots;
	private boolean disposed = false;

	private Fragment(List<Component> roots) {
		this.roots = roots;
	}

	/**
	 * Captures components created inside the given block as a single fragment. Components are
	 * captured in creation order, in an isolated parent-stack context, without being attached to
	 * a live parent. If the block throws, all partially created components are disposed, ambient
	 * state is restored, and the exception propagates.
	 */
	public static Fragment capture(Runnable block) {
		List<Component> captured = ParentStack.capture(block, ParentStack.takeCaptureList());
		return new Fragment(captured);
	}

	/** The captured root components, in creation order. Never null. */
	public List<Component> roots() {
		return disposed ? Collections.emptyList() : Collections.unmodifiableList(roots);
	}

	/** Returns whether this fragment has no captured roots (collapse case). */
	public boolean isEmpty() {
		return roots.isEmpty();
	}

	/** Returns the number of captured roots. */
	public int size() {
		return roots.size();
	}

	@Override
	public arc.scene.Element element() {
		throw new UnsupportedOperationException(
				"Fragment is a multi-root ownership unit; mount roots individually via roots()");
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		List<Component> roots = this.roots;
		this.roots = Collections.emptyList();
		for (Component root : roots) {
			try {
				root.dispose();
			} catch (Throwable t) {
				Log.err("Error disposing fragment root", t);
			}
		}
		ParentStack.releaseCaptureList(roots);
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	/** Creates a fragment over explicitly provided roots (used by tests and internal mount sites). */
	public static Fragment of(List<Component> roots) {
		return new Fragment(new ArrayList<>(roots));
	}
}
