package solim.runtime;

import arc.Core;
import arc.Input;
import arc.input.KeyCode;
import arc.util.Log;
import arc.util.Nullable;

/**
 * Profiling-enabled {@link SignalDispatcher} variant that brackets each frame flush in a
 * {@code flush} flame span and polls for the {@code Ctrl + D} profiling toggle.
 *
 * <p>Input polling lives here rather than in {@code Perf} because this class only exists while debug
 * services are installed, which is what keeps {@code Core.input} out of the normal flush path
 * entirely.
 */
public class DebugSignalDispatcher extends SignalDispatcher {

	/** Whether the {@code Ctrl + D} profiling keybind is polled for. */
	private boolean keybindPolling = false;

	/** Invoked when {@code Ctrl + D} is tapped; wired by {@code Perf} to the profiling toggle. */
	private @Nullable Runnable toggleAction = null;

	public DebugSignalDispatcher() {
	}

	/** Enables or disables {@code Ctrl + D} polling. */
	public void setKeybindPolling(boolean enabled) {
		keybindPolling = enabled;
	}

	public boolean isKeybindPolling() {
		return keybindPolling;
	}

	/** Sets the action invoked when the profiling keybind is tapped, or {@code null} to clear it. */
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

	/**
	 * Polls {@code Ctrl + D} once per frame. Guarded against a null {@link Core#input} so headless
	 * and standalone-Solim environments — where no input backend is installed — behave normally.
	 */
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