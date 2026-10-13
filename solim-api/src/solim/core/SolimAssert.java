package solim.core;

import arc.util.Nullable;

public final class SolimAssert {
    private static volatile @Nullable Thread mainThread;

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
            Thread.dumpStack();
            throw new IllegalStateException(
                    "[Solim] Must be called on main thread (" + main.getName()
                            + "), but was called on: " + Thread.currentThread().getName());
        }
    }
}
