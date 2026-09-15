package mindustrytool.features.browser.schematic;

import arc.Core;
import arc.Events;
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
                .keybind(BrowserKeybinds.schematicBrowser)
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
    public @Nullable SolimDialog getSettingDialog() {
        return null;
    }

    @Override
    public @Nullable SolimDialog getMainDialog() {
        if (dialog == null) {
            dialog = new SchematicBrowserDialog();
        }
        return dialog;
    }

    public void showDialog() {
        if (dialog == null) {
            dialog = new SchematicBrowserDialog();
        }
        dialog.show();
    }

    private void updateKeybind() {
        if (!isEnabled() || !BrowserKeybinds.noInputFocused()) {
            return;
        }
        if (Core.input.keyRelease(BrowserKeybinds.schematicBrowser)) {
            Core.app.post(this::showDialog);
        }
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
