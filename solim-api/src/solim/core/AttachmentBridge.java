package solim.core;

import arc.scene.Element;
import arc.util.Nullable;

/**
 * The attachment engine as a {@link Component} sees it.
 *
 * <p>Attachment is runtime machinery and lives a module above this one, but a component has to
 * announce itself to the ambient parent scope from its own constructor, and a lazy build has to
 * report its span to the tracer. This is the seam between the two: the runtime installs an
 * implementation once, and {@link Component} calls through it.
 *
 * <p>Until an implementation is installed every call here is a no-op, which is exactly the
 * behaviour of constructing a component while no parent is on the stack — the only situation in
 * which these calls have nothing to do. That invariant is what makes the bridge safe: whenever a
 * parent <em>is</em> open, the runtime class has been loaded and has installed itself.
 */
public final class AttachmentBridge {

	/** The operations a component performs against the attachment engine. */
	public interface Hook {

		/** Announces a freshly constructed component to the innermost open parent scope. */
		void registerPending(Component component);

		/** Whether build tracing is on. Gates the timestamp reads taken around a build. */
		boolean isTracing();

		/** Records one {@code build} span covering {@code [startNanos, endNanos)}. */
		void traceLeaf(@Nullable String name, long startNanos, long endNanos);
	}

	private static volatile @Nullable Hook hook;

	private AttachmentBridge() {}

	/** Installs the engine the runtime provides; {@code null} detaches it again. */
	public static void install(@Nullable Hook hook) {
		AttachmentBridge.hook = hook;
	}

	static void registerPending(Component component) {
		Hook current = hook;
		if (current != null) {
			current.registerPending(component);
		}
	}

	static boolean isTracing() {
		Hook current = hook;
		return current != null && current.isTracing();
	}

	static void traceLeaf(@Nullable String name, long startNanos, long endNanos) {
		Hook current = hook;
		if (current != null) {
			current.traceLeaf(name, startNanos, endNanos);
		}
	}
}
