package mindustrytool.features;

import java.util.Optional;

import arc.Core;
import arc.scene.ui.Dialog;

public interface Feature {
    FeatureMetadata getMetadata();

    default void init() {
    };

    default void onEnable() {
    };

    default void onDisable() {
    };

    default void onEnableChange(boolean enabled) {
    }

    default Optional<Dialog> setting() {
        return Optional.empty();
    }

    default Optional<Dialog> dialog() {
        return Optional.empty();
    }

    default boolean isEnabled() {
        var metadata = getMetadata();

        return Core.settings.getBool("mindustrytool.feature." + metadata.name() + ".enabled",
                metadata.enabledByDefault());
    }

    default void setEnabled(boolean enabled) {
        boolean current = isEnabled();

        if (current == enabled) {
            return;
        }

        Core.settings.put("mindustrytool.feature." + getMetadata().name() + ".enabled", enabled);

        Core.app.post(() -> {
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
            onEnableChange(enabled);
        });
    }

    default void toggle() {
        setEnabled(!isEnabled());
    }
}
