package mindustrytool.features.modmanager;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;

public final class ModManagerFeature extends Feature {

    private @Nullable ModManagerDialog dialog;
    private @Nullable Button switchButton;

    public ModManagerFeature() {
        super(FeatureMetadata.builder()
                .id("mod-manager")
                .icon(Icon.book)
                .order(28)
                .enabledByDefault(true)
                .quickAccess(true)
                .build());

        bindDialog("mod-manager", KeyCode.unset, this::showDialog);
        Events.on(ClientLoadEvent.class, e -> installModsDialogHook());
    }

    @Override
    public void onEnable() {
        installModsDialogHook();
    }

    @Override
    public void onDisable() {
        removeVanillaSwitchButton();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return null;
    }

    @Override
    public @Nullable Prov<SolimDialog> getMainDialog() {
        return this::getOrCreateDialog;
    }

    public void showDialog() {
        getOrCreateDialog().show();
    }

    public void openVanillaModsDialog() {
        if (Vars.ui == null || Vars.ui.mods == null) {
            return;
        }
        AttachableDialogBridge.openFallback(Vars.ui.mods);
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

    private ModManagerDialog getOrCreateDialog() {
        if (dialog == null || dialog.isDisposed()) {
            dialog = new ModManagerDialog(this::openVanillaModsDialog);
        }
        return dialog;
    }

    private void installModsDialogHook() {
        Runnable attachTask = () -> {
            try {
                if (Vars.ui == null || Vars.ui.mods == null) {
                    return;
                }
                Drawable switchIcon = resolveSwitchIcon();
                String switchLabel = Core.bundle != null
                        ? Core.bundle.get("mod-manager.switch-from-vanilla", "Mindustry Tool")
                        : "Mindustry Tool";

                AttachableDialogBridge.attach(
                        Vars.ui.mods,
                        getOrCreateDialog().dialog(),
                        switchIcon,
                        switchLabel,
                        this::isEnabled);

                Table buttons = Vars.ui.mods.buttons;
                if (buttons != null && isEnabled() && (switchButton == null || switchButton.parent == null)) {
                    switchButton = buttons.button(
                            switchLabel,
                            switchIcon,
                            () -> {
                                Vars.ui.mods.hide(null);
                                showDialog();
                            }).size(210f, 64f).get();
                }
            } catch (Exception ignored) {
            }
        };

        attachTask.run();
        if (Core.app != null) {
            Core.app.post(attachTask);
        }
    }

    private Drawable resolveSwitchIcon() {
        try {
            return FileIcon.of("mindustrytool.png");
        } catch (Throwable ignored) {
            return Icon.book;
        }
    }

    private void removeVanillaSwitchButton() {
        if (Core.app == null) {
            return;
        }
        Core.app.post(() -> {
            try {
                if (switchButton != null) {
                    switchButton.remove();
                    switchButton = null;
                }
            } catch (Exception ignored) {
            }
        });
    }
}
