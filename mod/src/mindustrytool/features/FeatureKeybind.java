package mindustrytool.features;

import arc.input.KeyBind;
import arc.input.KeyCode;
import solim.reactive.Signal;

/**
 * Descriptor for a single feature hotkey binding.
 * Holds the Arc {@link KeyBind}, the action to run when triggered,
 * and whether the parent feature must be enabled for the action to run.
 */
public class FeatureKeybind {
    private final KeyBind bind;
    private final Runnable action;
    private final boolean requireEnabled;
    private final Signal<KeyCode> keySignal;

    public FeatureKeybind(KeyBind bind, Runnable action, boolean requireEnabled) {
        if (bind == null) {
            throw new IllegalArgumentException("bind must not be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        this.bind = bind;
        this.action = action;
        this.requireEnabled = requireEnabled;
        this.keySignal = Signal.of(extractKeyCode(bind));
    }

    public KeyBind getBind() {
        return bind;
    }

    public Runnable getAction() {
        return action;
    }

    public boolean isRequireEnabled() {
        return requireEnabled;
    }

    public KeyCode getKeyCode() {
        return keySignal.get();
    }

    public Signal<KeyCode> keySignal() {
        return keySignal;
    }

    public void rebind(KeyCode newKey) {
        KeyCode target = newKey != null ? newKey : KeyCode.unset;
        bind.value = new KeyBind.Axis(target);
        bind.save();
        keySignal.set(target);
    }

    public void resetToDefault() {
        bind.resetToDefault();
        keySignal.set(extractKeyCode(bind));
    }

    public boolean isDefault() {
        return bind.isDefault();
    }

    public static KeyCode extractKeyCode(KeyBind bind) {
        if (bind.value == null) {
            return KeyCode.unset;
        }
        return bind.value.key != null ? bind.value.key : KeyCode.unset;
    }
}
