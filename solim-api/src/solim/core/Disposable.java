package solim.core;

/** Disposable handle for subscriptions, computeds and effects. */
public interface Disposable {
	void dispose();

    // TODO: SHould not be default 
	default boolean isDisposed() {
		return false;
	}
}
