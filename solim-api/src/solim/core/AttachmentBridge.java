package solim.core;

import arc.util.Nullable;

public final class AttachmentBridge {

    public interface Hook {
        void registerPending(Component component);
    }

    private static volatile @Nullable Hook hook;

    private AttachmentBridge() {
    }

    public static void install(@Nullable Hook hook) {
        AttachmentBridge.hook = hook;
    }

    static void registerPending(Component component) {
        Hook current = hook;
        if (current != null) {
            current.registerPending(component);
        }
    }
}
