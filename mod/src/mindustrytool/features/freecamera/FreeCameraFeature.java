package mindustrytool.features.freecamera;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Unit;
import mindustry.input.DesktopInput;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.input.ModMobileInput;

/**
 * Provides an unconstrained free camera that stays detached from the player unit.
 * On desktop, decouples WASD movement from camera snapping so players can scout
 * the map freely. On mobile, coordinates with the virtual joystick.
 */
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

        bindToggle("freeCameraToggle", KeyCode.unset);
        bindAction("freeCameraSnap", KeyCode.unset, this::snapToPlayer, true);
    }

    @Override
    public void onEnable() {
        mountHud();
    }

    @Override
    public void onDisable() {
        unmountHud();
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

    /**
     * Centers the camera immediately onto the player unit's coordinates.
     */
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
            } else if (Vars.control.input instanceof DesktopInput) {
                ((DesktopInput) Vars.control.input).panning = false;
            }
        }
    }

    /**
     * Checks whether Free Camera is currently active.
     */
    public static boolean isFreeCam() {
        FreeCameraFeature feat = FeatureManager.getFeature(FreeCameraFeature.class);
        return feat != null && feat.isEnabled();
    }

    private void mountHud() {
        if (hudView != null) {
            return;
        }
        hudView = new FreeCameraHudView(this);
        Element el = hudView.element();
        el.name = "free-camera-indicator-hud";
        el.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown
                && Vars.state != null && Vars.state.isGame());

        Core.app.post(() -> {
            if (hudView != null && Vars.ui != null && Vars.ui.hudGroup != null) {
                Vars.ui.hudGroup.addChild(el);
            }
        });
    }

    private void unmountHud() {
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
}
