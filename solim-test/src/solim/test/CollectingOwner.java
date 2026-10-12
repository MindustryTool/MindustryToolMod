package solim.test;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Disposable;
import solim.core.DisposableOwner;

/**
 * An ownership scope that keeps whatever is registered with it instead of handing it to a component.
 *
 * <p>Lets a test install itself as the ambient owner and then inspect what was registered, which is
 * what the ownership contract is really about.
 */
public final class CollectingOwner implements DisposableOwner {

	private final List<Disposable> collected = new ArrayList<>();
	private boolean disposed = false;

	/** Everything registered with this owner so far, in registration order. */
	public List<Disposable> collected() {
		return collected;
	}

	@Override
	public void adopt(@Nullable Disposable resource) {
		if (resource != null && !disposed) {
			collected.add(resource);
		}
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		for (int i = collected.size() - 1; i >= 0; i--) {
			collected.get(i).dispose();
		}
		collected.clear();
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}
}
