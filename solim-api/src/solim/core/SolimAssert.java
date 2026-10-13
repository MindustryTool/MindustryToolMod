package solim.core;

import arc.util.Nullable;

public final class SolimAssert {
    private static volatile @Nullable Thread mainThread;
    private static volatile boolean checksEnabled = false;

    private SolimAssert() {
    }

    public static void setMainThread(@Nullable Thread thread) {
        mainThread = thread;
    }

    public static @Nullable Thread getMainThread() {
        return mainThread;
    }

    public static void checkMainThread() {
        Thread main = mainThread;
        if (main != null && Thread.currentThread() != main) {
            throw new IllegalStateException(
                    "[Solim] Must be called on main thread (" + main.getName()
                            + "), but was called on: " + Thread.currentThread().getName());
        }
    }

    public static void setChecksEnabled(boolean enabled) {
        checksEnabled = enabled;
    }

    public static boolean checksEnabled() {
        return checksEnabled;
    }

    public static void checkLifo(@Nullable Object popped, @Nullable Object expected) {
        if (!checksEnabled || popped == expected) {
            return;
        }
        throw new IllegalStateException("[Solim] Unbalanced ambient frame: expected "
                + name(expected) + " to be popped, but popped " + name(popped));
    }

    private static String name(@Nullable Object value) {
        return value == null ? "<empty>" : value.getClass().getSimpleName();
    }
}