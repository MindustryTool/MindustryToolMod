package solim.core;

import arc.util.Nullable;

/**
 * Thread-safety verification for Solim runtime operations. Validates that
 * mutating operations happen on the registered main/render thread.
 */
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

    /**
     * Asserts that a component is owned, either having an explicit parent or being registered with SessionRoot.
     */
    public static void assertOwned(@Nullable Component component) {
        if (component == null) {
            throw new IllegalArgumentException("[Solim] Component cannot be null");
        }
        if (component.owner() == null && !SessionRoot.get().contains(component)) {
            throw new AssertionError("[Solim] Component " + component.getClass().getSimpleName()
                    + " is unowned (no parent and not in SessionRoot)");
        }
    }
}
