package mindustrytool.features.freecamera;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Unit;
import mindustry.input.DesktopInput;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.input.ModMobileInput;

public class FreeCameraFeature extends Feature {

    private @Nullable FreeCameraHudView hudView;

    public FreeCameraFeature() {
        super(FeatureMetadata.builder()
                .id("free-camera")
                .icon(FileIcon.of("move.png"))
                .order(6)
                .enabledByDefault(false)
                .quickAccessByDefault(true)
                .build());

        bindToggle("freecameratoggle", KeyCode.unset);
        bindAction("freecamerasnap", KeyCode.unset, this::snapToPlayer, true);

        Events.on(ClientLoadEvent.class, e -> {
            if (isEnabled()) {
                ensureHud();
            }
        });
    }

    @Override
    public void onEnable() {
        ensureHud();
    }

    @Override
    public void onDisable() {
        removeHud();
        snapToPlayer();
    }

    @Override
    public void onQuickAccessClick(@Nullable Element anchor) {
        setEnabled(!isEnabled());
    }

    @Override
    public void onQuickAccessLongClick(@Nullable Element anchor) {
        snapToPlayer();
    }

    public void ensureHud() {
        if (hudView != null) {
            return;
        }
        hudView = new FreeCameraHudView(this);
        Element el = hudView.element();
        el.name = "free-camera-hud";
        el.visible(() -> isEnabled()
                && Vars.ui != null
                && Vars.ui.hudfrag != null
                && Vars.ui.hudfrag.shown
                && Vars.state != null
                && Vars.state.isGame());

        Core.app.post(() -> {
            if (hudView != null && Vars.ui != null && Vars.ui.hudGroup != null) {
                Vars.ui.hudGroup.addChild(el);
            }
        });
    }

    public void removeHud() {
        if (hudView == null) {
            return;
        }
        FreeCameraHudView view = hudView;
        hudView = null;
        Core.app.post(() -> {
            view.element().remove();
            view.dispose();
        });
    }

    public @Nullable FreeCameraHudView getHudView() {
        return hudView;
    }

    public void snapToPlayer() {
        if (Core.camera == null || Core.camera.position == null
                || Vars.player == null || Vars.player.dead()) {
            return;
        }

        Unit unit = Vars.player.unit();
        if (unit != null && !unit.dead) {
            Core.camera.position.set(unit.x, unit.y);
        } else {
            Core.camera.position.set(Vars.player.x, Vars.player.y);
        }

        if (Vars.control != null) {
            if (Vars.control.input instanceof ModMobileInput) {
                ((ModMobileInput) Vars.control.input).cancelPanDelay();
                ((ModMobileInput) Vars.control.input).spectating = null;
            } else if (Vars.control.input instanceof DesktopInput) {
                ((DesktopInput) Vars.control.input).panning = false;
                ((DesktopInput) Vars.control.input).spectating = null;
            }
        }
    }

    public static boolean isFreeCam() {
        FreeCameraFeature feat = FeatureManager.getFeature(FreeCameraFeature.class);
        return feat != null && feat.isEnabled();
    }
}
