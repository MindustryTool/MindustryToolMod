package solim.core;

import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class AttachmentBridge {

    public interface Hook {
        void registerPending(Component component);
    }

    private static final List<Hook> hooks = new ArrayList<>(2);

    private AttachmentBridge() {
    }

    public static void install(@Nullable Hook hook) {
        SolimAssert.checkMainThread();
        hooks.clear();
        if (hook != null) {
            hooks.add(hook);
        }
    }

    public static void add(@Nullable Hook hook) {
        SolimAssert.checkMainThread();
        if (hook != null && !hooks.contains(hook)) {
            hooks.add(hook);
        }
    }

    public static boolean remove(@Nullable Hook hook) {
        SolimAssert.checkMainThread();
        return hook != null && hooks.remove(hook);
    }

    public static int hookCount() {
        return hooks.size();
    }

    static void registerPending(Component component) {
        if (hooks.isEmpty()) {
            return;
        }
        RuntimeException failure = null;
        for (int i = 0; i < hooks.size(); i++) {
            try {
                hooks.get(i).registerPending(component);
            } catch (Throwable t) {
                Log.err("[Solim] Attachment hook failed", t);
                if (failure == null && t instanceof RuntimeException) {
                    failure = (RuntimeException) t;
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}