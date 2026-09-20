package mindustrytool.features.freecamera;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.GlyphLayout;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.util.Align;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.input.DesktopInput;
import mindustry.ui.Fonts;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.input.ModMobileInput;

/**
 * Provides an unconstrained free camera that stays detached from the player unit.
 * On desktop, decouples WASD movement from camera snapping so players can scout
 * the map freely. On mobile, coordinates with the virtual joystick.
 * While enabled, a centered status label is drawn at the top of the view.
 */
public class FreeCameraFeature extends Feature {


    // TODO: Draw a line from center of the screen to player unit (optional)
    private static final float SCALE_DIVISOR = 1000f;
    private static final float TOP_MARGIN = 48f;
    private static final float PAD_X = 16f;
    private static final float PAD_Y = 8f;
    private static final float BACKDROP_ALPHA = 0.7f;

    private final GlyphLayout layout = new GlyphLayout();

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

        Events.run(Trigger.draw, this::draw);
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

    /**
     * Checks whether Free Camera is currently active.
     */
    public static boolean isFreeCam() {
        FreeCameraFeature feat = FeatureManager.getFeature(FreeCameraFeature.class);
        return feat != null && feat.isEnabled();
    }

    private void draw() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame()) {
            return;
        }
        if (Vars.ui == null || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
            return;
        }
        if (Core.camera == null || Fonts.outline == null || Core.bundle == null) {
            return;
        }

        String label = Core.bundle.get("status.free-camera.enabled");
        float scale = Core.camera.height / SCALE_DIVISOR;
        if (scale <= 0f) {
            return;
        }

        layout.setText(Fonts.outline, label);
        float textWidth = layout.width * scale;
        float textHeight = layout.height * scale;
        float centerX = Core.camera.position.x;
        float baseline = Core.camera.position.y + Core.camera.height / 2f - TOP_MARGIN * scale - textHeight;

        float z = Draw.z();
        Draw.z(Layer.overlayUI);
        Draw.color(Color.black, BACKDROP_ALPHA);
        Fill.rect(centerX, baseline + textHeight / 2f,
                textWidth + PAD_X * 2f * scale, textHeight + PAD_Y * 2f * scale);
        // v160.1 Font.draw(str, x, y, color, scale, integer, halign): centers on x with Align.center.
        Fonts.outline.draw(label, centerX, baseline, Pal.accent, scale, false, Align.center);
        Draw.z(z);
        Draw.reset();
    }
}
