package mindustrytool.features.browser.schematic;

import arc.Events;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;

public class SchematicBrowserFeature implements Feature {
    private SchematicDialog schematicDialog;
    private Button browseButton;

    @Override
    public FeatureMetadata getMetadata() {
        return new FeatureMetadata("Schematic Browser", "Browse and download schematics.", "schematic", 2);
    }

    @Override
    public void init() {
        schematicDialog = new SchematicDialog();

        Events.on(ClientLoadEvent.class, e -> {
            if (FeatureManager.getInstance().isEnabled(this)) {
                addBrowseButton();
            }
        });
    }

    private void addBrowseButton() {
        if (Vars.ui == null || Vars.ui.schematics == null)
            return;

        Table buttons = Vars.ui.schematics.buttons;
        if (browseButton == null || browseButton.parent == null) {
            browseButton = buttons.button("Browse", Icon.menu, () -> {
                Vars.ui.schematics.hide();
                schematicDialog.show();
            }).get();
        }
    }

    @Override
    public void onEnable() {
        addBrowseButton();
    }

    @Override
    public void onDisable() {
        if (schematicDialog != null) {
            schematicDialog.hide();
        }
        if (browseButton != null) {
            browseButton.remove();
            browseButton = null;
        }
    }

    public SchematicDialog getDialog() {
        return schematicDialog;
    }
}
