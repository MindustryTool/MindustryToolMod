package solim.runtime;

import arc.Core;
import arc.Input;
import arc.input.KeyCode;
import arc.util.Log;
import arc.util.Nullable;

public class DebugSignalDispatcher extends SignalDispatcher {

    private boolean keybindPolling = false;

    private @Nullable Runnable toggleAction = null;

    public DebugSignalDispatcher() {
    }

    public void setKeybindPolling(boolean enabled) {
        keybindPolling = enabled;
    }

    public boolean isKeybindPolling() {
        return keybindPolling;
    }

    public void setToggleAction(@Nullable Runnable action) {
        toggleAction = action;
    }

    @Override
    protected void doFlush() {
        pollProfilingKeybind();
        if (queue.isEmpty() || flushing) {
            return;
        }
        AttachmentStack.beginSpan("flush", "flush");
        try {
            super.doFlush();
        } finally {
            AttachmentStack.endSpan();
        }
    }

    private void pollProfilingKeybind() {
        if (!keybindPolling) {
            return;
        }
        Input input = Core.input;
        if (input == null) {
            return;
        }
        try {
            if (input.ctrl() && input.keyTap(KeyCode.d)) {
                Runnable action = toggleAction;
                if (action != null) {
                    action.run();
                }
            }
        } catch (Throwable t) {
            Log.err("[Solim] Profiling keybind poll failed", t);
            keybindPolling = false;
        }
    }
}
