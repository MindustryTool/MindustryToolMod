package mindustrytool.features.browser.schematic;

import arc.Core;
import arc.Events;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustrytool.MdtKeybinds;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;

public class SchematicBrowserFeature implements Feature {
    private SchematicDialog schematicDialog;
    private Button browseButton;

    @Override
    public FeatureMetadata getMetadata() {
        return new FeatureMetadata("Schematic Browser", "Browse and download schematics.", Iconc.paste, 2);
    }

    @Override
    public void init() {
        schematicDialog = new SchematicDialog();

        Events.on(ClientLoadEvent.class, e -> {
            if (FeatureManager.getInstance().isEnabled(this)) {
                addBrowseButton();
            }
        });

        Events.run(Trigger.update, () -> {
            boolean noInputFocused = !Core.scene.hasField();
            boolean enabled = FeatureManager.getInstance().isEnabled(this);

            if (enabled && noInputFocused && Core.input.keyRelease(MdtKeybinds.schematicBrowserKb)) {
                schematicDialog.show();
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
        if (browseButton != null) {
            browseButton.remove();
            browseButton = null;
        }
    }

    public SchematicDialog getDialog() {
        return schematicDialog;
    }
}
