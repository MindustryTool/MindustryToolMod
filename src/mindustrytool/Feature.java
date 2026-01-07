package mindustrytool;

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
    default Dialog setting() {
        return null;
    }
}
