package mindustrytool.features.browser.map;

import arc.Core;
import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustrytool.MdtKeybinds;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;

public class MapBrowserFeature implements Feature {
    private MapDialog mapDialog;

    @Override
    public FeatureMetadata getMetadata() {
        return new FeatureMetadata("Map Browser", "Browse and download maps from the community.", Iconc.map, 1);
    }

    @Override
    public void init() {
        mapDialog = new MapDialog();

        // We hook into ClientLoadEvent to add the button initially
        Events.on(ClientLoadEvent.class, e -> {
            if (FeatureManager.getInstance().isEnabled(this)) {
                addButton();
            }
        });

        Events.run(Trigger.update, () -> {
            boolean noInputFocused = !Core.scene.hasField();
            boolean enabled = FeatureManager.getInstance().isEnabled(this);

            if (enabled && noInputFocused && Core.input.keyRelease(MdtKeybinds.mapBrowserKb)) {
                mapDialog.show();
            }
        });
    }

    private void addButton() {
        // Since we cannot easily remove buttons from MenuFragment,
        // we only add it. If disabled, the action will be blocked or we accept it
        // stays.
        // Ideally we would manage a custom menu.

        if (Vars.mobile) {
            Vars.ui.menufrag.addButton(Core.bundle.format("message.map-browser.title"), Icon.map, () -> {
                if (FeatureManager.getInstance().isEnabled(this)) {
                    mapDialog.show();
                } else {
                    Vars.ui.showInfo("Feature is disabled.");
                }
            });
        } else {
            // For desktop, Main.java had a complex structure merging buttons.
            // We might need to coordinate with other features if we want a single "Tools"
            // menu.
            // For now, let's add a standalone button or rely on Main to orchestrate the
            // "Tools" menu.
        }
    }

    @Override
    public void onEnable() {
        // If we could dynamically add to menu, we would.
        // But MenuFragment is static-ish.
        // We rely on the button checking the enabled state.
    }

    @Override
    public void onDisable() {
        if (mapDialog != null) {
            mapDialog.hide();
        }
    }

    public MapDialog getDialog() {
        return mapDialog;
    }
}
