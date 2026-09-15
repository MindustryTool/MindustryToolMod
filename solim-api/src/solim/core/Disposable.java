package solim.core;

/** Disposable handle for subscriptions, computeds and effects. */
public interface Disposable {
	void dispose();

	default boolean isDisposed() {
		return false;
	}
}
