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

	public interface Hook {
		void registerPending(Component component);
	}

	private static volatile @Nullable Hook hook;

	private AttachmentBridge() {}

	public static void install(@Nullable Hook hook) {
		AttachmentBridge.hook = hook;
	}

	static void registerPending(Component component) {
		Hook current = hook;
		if (current != null) {
			current.registerPending(component);
		}
	}
}
