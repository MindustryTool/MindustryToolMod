package mindustrytool.features;

import arc.input.KeyBind;

/**
 * Descriptor for a single feature hotkey binding.
 * Holds the Arc {@link KeyBind}, the action to run when triggered,
 * and whether the parent feature must be enabled for the action to run.
 */
public class FeatureKeybind {
    private final KeyBind bind;
    private final Runnable action;
    private final boolean requireEnabled;

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
}
