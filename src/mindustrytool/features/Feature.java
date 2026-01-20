package mindustrytool.features;

import java.util.Optional;

import arc.scene.ui.Dialog;

public interface Feature {
    FeatureMetadata getMetadata();

    /** Called when game is loaded */
    void init();

    /** Called when feature is just enabled */
    void onEnable();

    /** Called when feature is just disabled */
    void onDisable();

    /** Return a dialog for that feature setting */
    default Optional<Dialog> setting() {
        return Optional.empty();
    }
   
    default Optional<Dialog> dialog() {
        return Optional.empty();
    }
}
