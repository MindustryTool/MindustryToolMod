package solim.core;

import arc.util.Nullable;

/** Lazily supplies a value when it is needed. */
@FunctionalInterface
public interface Provider<T> {
    @Nullable T get();
}
