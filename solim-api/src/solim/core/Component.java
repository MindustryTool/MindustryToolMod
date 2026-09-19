package solim.core;

import arc.scene.Element;

/** Minimal component abstraction. A component is a lifecycle resource that owns an Arc Element. */
public interface Component extends Disposable {
	Element element();

	@Override
	default void dispose() {}

	@Override
	default boolean isDisposed() {
		return false;
	}
}
