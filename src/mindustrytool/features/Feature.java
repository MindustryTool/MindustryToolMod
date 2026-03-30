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

    default String getSettingKey() {
        return "mindustrytool." + getMetadata().name() + ".enabled";
    }

    default boolean isEnabled() {
        var metadata = getMetadata();

        return Core.settings.getBool(getSettingKey(), metadata.enabledByDefault());
    }
}
