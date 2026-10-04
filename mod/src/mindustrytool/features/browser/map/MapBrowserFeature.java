package mindustrytool.features.browser.map;

import arc.Core;
import arc.func.Prov;
import arc.input.KeyCode;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import arc.scene.Element;

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
                .build());

        bindDialog("mapBrowser", KeyCode.unset, this::showDialog);
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

    @Override
    public void onQuickAccessClick(@Nullable Element anchor) {
        showDialog();
    }

    @Override
    public void onQuickAccessClick() {
        showDialog();
    }

    @Override
    public Readable<Boolean> quickAccessHighlight() {
        return Readable.of(true);
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
