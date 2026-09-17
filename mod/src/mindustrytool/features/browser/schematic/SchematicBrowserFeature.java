package mindustrytool.features.browser.schematic;

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

/**
 * Feature for browsing, searching, and downloading schematics from the online
 * library. Supports in-game placement, clipboard copying, and local library
 * saving.
 */
public class SchematicBrowserFeature extends Feature {

    private @Nullable SchematicBrowserDialog dialog;
    private @Nullable Button browseButton;

    public SchematicBrowserFeature() {
        super(FeatureMetadata.builder()
                .id("schematic-browser")
                .icon(Icon.paste)
                .order(30)
                .enabledByDefault(true)
                .quickAccess(false)
                .build());

        bindDialog("schematicBrowser", KeyCode.unset, this::showDialog);
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
                dialog = new SchematicBrowserDialog();
            }
            return dialog;
        };
    }

    public void showDialog() {
        if (dialog == null) {
            dialog = new SchematicBrowserDialog();
        }
        dialog.show();
    }

    private void injectBrowseButton() {
        Core.app.post(() -> {
            try {
                if (Vars.ui.schematics == null) {
                    return;
                }
                Table buttons = Vars.ui.schematics.buttons;
                if (browseButton == null || browseButton.parent == null) {
                    browseButton = buttons.button(
                            Core.bundle.get("browser.schematic.browse-button"),
                            Icon.menu,
                            () -> {
                                Vars.ui.schematics.hide();
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
