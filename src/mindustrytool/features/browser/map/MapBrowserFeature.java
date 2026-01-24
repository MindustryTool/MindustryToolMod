package mindustrytool.features.browser.map;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.scene.ui.Dialog;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustrytool.MdtKeybinds;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;

public class MapBrowserFeature implements Feature {
    private MapDialog mapDialog;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.map-browser.name")
                .description("@feature.map-browser.description")
                .icon(Icon.map)
                .order(1)
                .build();
    }

    @Override
    public void init() {
        mapDialog = new MapDialog();

        Events.run(Trigger.update, () -> {
            boolean noInputFocused = !Core.scene.hasField();
            boolean enabled = FeatureManager.getInstance().isEnabled(this);

            if (enabled && noInputFocused && Core.input.keyRelease(MdtKeybinds.mapBrowserKb)) {
                Core.app.post(() -> mapDialog.show());
            }
        });
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

    @Override
    public Optional<Dialog> dialog() {
        return Optional.of(mapDialog);
    }
}
