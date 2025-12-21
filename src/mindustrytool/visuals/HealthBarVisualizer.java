package mindustrytool.visuals;

import arc.Core;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;

import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;

import static mindustry.Vars.*;

import arc.graphics.g2d.TextureRegion;
import arc.func.Cons;

public class HealthBarVisualizer {

    private static TextureRegion barRegion;
    private final Cons<Unit> drawUnitRef = this::drawCheck;

    // Min Zoom Threshold: Skip rendering when zoomed out beyond this level
    private float zoomThreshold;

    private mindustry.ui.dialogs.BaseDialog dialog;

    public HealthBarVisualizer() {
        arc.util.Log.info("[HealthBarVisualizer] INITIALIZED.");

        // Load saved zoom threshold (default 0.5 = auto-disable when zoomed out
        // significantly)
        zoomThreshold = Core.settings.getFloat("visualizer.healthbar.zoomThreshold", 0.5f);

        // Cache the region once
        if (barRegion == null) {
            barRegion = Core.atlas.find("white-ui");
            if (barRegion == null || !barRegion.found()) {
                barRegion = Core.atlas.white(); // Fallback
            }
        }
    }

    public void draw() {
        if (!state.isGame())
            return;

        // Min Zoom Check: Skip rendering when zoomed out too far
        float zoom = renderer.getScale();
        if (zoomThreshold > 0 && zoom < zoomThreshold)
            return;

        // Draw on overlay layer (above units, below UI)
        Draw.z(mindustry.graphics.Layer.shields + 5f);

        // Access camera directly to avoid Rect allocation if possible,
        // but Core.camera.bounds(Rect) reuses the Rect passed in.
        // We can just use the camera fields directly for the Quadtree query.
        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float cw = Core.camera.width;
        float ch = Core.camera.height;

        // Quadtree Query
        Groups.unit.intersect(cx - cw / 2f, cy - ch / 2f, cw, ch, drawUnitRef);

        Draw.reset();
    }

    private void drawCheck(Unit unit) {
        if (!unit.isValid())
            return;

        // LOGIC: Filter Damaged/Shielded
        boolean damaged = unit.health < unit.maxHealth;
        boolean shielded = unit.shield > 0;

        if (!damaged && !shielded)
            return;

        drawBar(unit);
    }

    private void drawBar(Unit unit) {
        float x = unit.x;
        float y = unit.y + unit.hitSize * 0.8f + 3f;

        // Dimensions: Adjusted for visibility (Wider/Thinner)
        float w = unit.hitSize * 2.5f;
        float h = 2f;

        // Background (Black with transparency)
        Draw.color(Color.black, 0.6f);
        Draw.rect(barRegion, x, y, w + 2f, h + 2f);

        // Health Calculation
        float hpPercent = unit.health / unit.maxHealth;

        // Draw Health Bar using Team Color with 75% Opacity
        Draw.color(unit.team.color, 0.75f);

        float left = x - w / 2f;

        if (hpPercent > 0) {
            float filledW = w * hpPercent;
            float fillCenterX = left + filledW / 2f;
            Draw.rect(barRegion, fillCenterX, y, filledW, h);
        }

        // Shield Overlay
        if (unit.shield > 0) {
            float shieldPercent = Math.min(unit.shield / unit.maxHealth, 1f);
            float shieldW = w * shieldPercent;
            float shieldCenterX = left + shieldW / 2f;

            Draw.color(Pal.shield, 0.5f);
            Draw.rect(barRegion, shieldCenterX, y, shieldW, h);
        }
    }

    /** Opens a settings dialog for this visualizer. */
    public void showSettings() {
        if (dialog == null) {
            dialog = new mindustry.ui.dialogs.BaseDialog("Health Bar Settings");
            dialog.addCloseButton();
            // Standard reset button with @settings.reset bundle key
            dialog.buttons.button("@settings.reset", mindustry.gen.Icon.refresh, () -> {
                zoomThreshold = 0.5f;
                Core.settings.put("visualizer.healthbar.zoomThreshold", zoomThreshold);
                rebuild(); // Update UI without closing
            }).size(250, 64);
            dialog.shown(this::rebuild);
        }
        dialog.show();
    }

    private void rebuild() {
        arc.scene.ui.layout.Table cont = dialog.cont;
        cont.clear();
        cont.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        // --- Zoom Threshold Slider ---
        arc.scene.ui.Slider zoomSlider = new arc.scene.ui.Slider(0f, 2f, 0.1f, false);
        zoomSlider.setValue(zoomThreshold);

        arc.scene.ui.Label zoomValue = new arc.scene.ui.Label(
                zoomThreshold <= 0.01f ? "Off" : String.format("%.1fx", zoomThreshold),
                mindustry.ui.Styles.outlineLabel);
        zoomValue.setColor(zoomThreshold <= 0.01f ? arc.graphics.Color.gray : arc.graphics.Color.lightGray);

        arc.scene.ui.layout.Table zoomContent = new arc.scene.ui.layout.Table();
        zoomContent.touchable = arc.scene.event.Touchable.disabled;
        zoomContent.margin(3f, 33f, 3f, 33f);
        zoomContent.add("Min Zoom", mindustry.ui.Styles.outlineLabel).left().growX();
        zoomContent.add(zoomValue).padLeft(10f).right();

        zoomSlider.changed(() -> {
            zoomThreshold = zoomSlider.getValue();
            zoomValue.setText(zoomThreshold <= 0.01f ? "Off" : String.format("%.1fx", zoomThreshold));
            zoomValue.setColor(zoomThreshold <= 0.01f ? arc.graphics.Color.gray : arc.graphics.Color.lightGray);
            Core.settings.put("visualizer.healthbar.zoomThreshold", zoomThreshold);
        });

        cont.stack(zoomSlider, zoomContent).width(width).left().padTop(4f).row();
    }
}
