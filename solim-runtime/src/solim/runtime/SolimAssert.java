package solim.runtime;

import arc.util.Log;
import arc.util.Nullable;

/**
 * Thread-safety verification for Solim runtime operations. Validates that
 * mutating operations happen on the registered main/render thread.
 */
public final class SolimAssert {
    private static volatile @Nullable Thread mainThread;
    public static boolean throww = false;

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
            if (throww) {
                throw new IllegalStateException(
                        "[Solim] Must be called on main thread (" + main.getName()
                                + "), but was called on: " + Thread.currentThread().getName());
            }
            Log.err("[Solim] Must be called on main thread (" + main.getName()
                    + "), but was called on: " + Thread.currentThread().getName());
        }
    }
}
