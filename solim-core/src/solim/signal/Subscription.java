package solim.signal;

import solim.core.Disposable;

/**
 * Handle returned by {@code Signal.subscribe} and {@code Computed.subscribe}. Idempotent dispose.
 */
public interface Subscription extends Disposable {}
