package mindustrytool.features.music;

import arc.Core;
import solim.overlay.SolimDialog;

/**
 * Settings dialog for the Custom Music feature.
 */
public class MusicSettingsDialog extends SolimDialog {

    public MusicSettingsDialog(MusicFeature feature) {
        super(Core.bundle.get("feature.music.settings.title"));

        name("musicSettingDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(800f);

        children(() -> new MusicSettingsView(feature));
    }
}
