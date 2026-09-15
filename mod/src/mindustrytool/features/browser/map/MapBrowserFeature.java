package mindustrytool.features.browser.map;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.browser.common.BrowserKeybinds;

import solim.overlay.SolimDialog;

/**
 * Feature for browsing, searching, and downloading maps from the online
 * library. Supports direct download into custom maps and instant play.
 */
public class MapBrowserFeature extends Feature {

    private @Nullable MapBrowserDialog dialog;
    private @Nullable Button browseButton;

    public MapBrowserFeature() {
        super(FeatureMetadata.builder()
                .id("map-browser")
                .icon(Icon.map)
                .order(35)
                .enabledByDefault(true)
                .quickAccess(false)
                .keybind(BrowserKeybinds.mapBrowser)
                .build());

        Events.run(Trigger.update, this::updateKeybind);
    }

    @Override
    public void onEnable() {
        injectBrowseButton();
    }

    @Override
    public void onDisable() {
        removeBrowseButton();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return null;
    }

    @Override
    public @Nullable Prov<SolimDialog> getMainDialog() {
        return () -> {
            if (dialog == null) {
                dialog = new MapBrowserDialog();
            }
            return dialog;
        };
    }

    public void showDialog() {
        if (dialog == null) {
            dialog = new MapBrowserDialog();
        }
        dialog.show();
    }

    private void updateKeybind() {
        if (!isEnabled() || !BrowserKeybinds.noInputFocused()) {
            return;
        }
        if (Core.input.keyRelease(BrowserKeybinds.mapBrowser)) {
            Core.app.post(this::showDialog);
        }
    }

    private void injectBrowseButton() {
        Core.app.post(() -> {
            try {
                if (Vars.ui.maps == null) {
                    return;
                }
                Table buttons = Vars.ui.maps.buttons;
                if (browseButton == null || browseButton.parent == null) {
                    browseButton = buttons.button(
                            Core.bundle.get("browser.map.browse-button"),
                            Icon.menu,
                            () -> {
                                Vars.ui.maps.hide();
                                showDialog();
                            }).get();
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void removeBrowseButton() {
        Core.app.post(() -> {
            try {
                if (browseButton != null) {
                    browseButton.remove();
                    browseButton = null;
                }
            } catch (Exception ignored) {
            }
        });
    }
}
