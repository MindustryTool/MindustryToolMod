package mindustrytool.features.freecamera;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Unit;
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

    public FreeCameraFeature() {
        super(FeatureMetadata.builder()
                .id("free-camera")
                .icon(FileIcon.of("camera.png"))
                .order(6)
                .enabledByDefault(true)
                .quickAccessByDefault(true)
                .build());

        bindToggle("freeCameraToggle", KeyCode.unset);
        bindAction("freeCameraSnap", KeyCode.unset, this::snapToPlayer, true);
    }

    @Override
    public void onDisable() {
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

        if (Vars.control != null && Vars.control.input instanceof ModMobileInput) {
            ((ModMobileInput) Vars.control.input).cancelPanDelay();
        }
    }

    /**
     * Checks whether Free Camera is currently active.
     */
    public static boolean isFreeCam() {
        FreeCameraFeature feat = FeatureManager.getFeature(FreeCameraFeature.class);
        return feat != null && feat.isEnabled();
    }
}
