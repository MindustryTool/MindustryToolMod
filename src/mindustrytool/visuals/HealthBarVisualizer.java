package mindustrytool.visuals;

import arc.Core;
import arc.util.Log;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.*;
import arc.scene.ui.*;
import arc.scene.event.*;

import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.Styles;

import static mindustry.Vars.*;

import arc.graphics.g2d.TextureRegion;
import arc.func.Cons;

public class HealthBarVisualizer {

    private static TextureRegion barRegion;
    private final Cons<Unit> drawUnitRef = this::drawCheck;

    // Min Zoom Threshold: Skip rendering when zoomed out beyond this level
    private float zoomThreshold;
    // Zoom Inverted: Skip rendering when zoomed in instead of out
    private boolean zoomInverted = false;

    private BaseDialog dialog;

    public HealthBarVisualizer() {
        Log.info("[HealthBarVisualizer] INITIALIZED.");

        // Load saved zoom threshold (default 2 = auto-disable when zoomed out
        // significantly)
        zoomThreshold = Core.settings.getFloat("visualizer.healthbar.zoomThreshold", 2f);
        zoomInverted = Core.settings.getBool("visualizer.healthbar.zoomInverted", false);

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

        // Min Zoom Check
        float zoom = renderer.getScale();
        if (zoomThreshold > 0) {
            if (!zoomInverted && zoom < zoomThreshold) {
                // Skip rendering when zoomed in beyond threshold
                Draw.reset();
                return;

            }
            if (zoomInverted && zoom > zoomThreshold) {
                // Skip rendering when zoomed out beyond threshold
                Draw.reset();
                return;
            }
        }

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
            dialog = new BaseDialog("Health Bar Settings");
            dialog.addCloseButton();
            // Standard reset button with @settings.reset bundle key
            dialog.buttons.button("@settings.reset", mindustry.gen.Icon.refresh, this::resetToDefaults).size(250, 64);
            dialog.shown(this::rebuild);
        }
        dialog.show();
    }

    private void resetToDefaults() {
        zoomThreshold = 2f;
        zoomInverted = false;

        Core.settings.put("visualizer.healthbar.zoomInverted", zoomInverted);
        Core.settings.put("visualizer.healthbar.zoomThreshold", zoomThreshold);

        rebuild(); // Update UI without closing
    }

    private void rebuild() {
        Table cont = dialog.cont;
        cont.clear();
        cont.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        // --- Zoom Threshold Slider ---
        Slider zoomSlider = new Slider(1.5f, 6f, 0.1f, false);
        zoomSlider.setValue(zoomThreshold);

        Label zoomValue = new Label(
                zoomThreshold <= 1.5f ? "Off" : String.format("%.1fx", zoomThreshold),
                Styles.outlineLabel);
        zoomValue.setColor(zoomThreshold <= 1.5f ? Color.gray : Color.lightGray);

        Table zoomContent = new Table();
        zoomContent.touchable = Touchable.disabled;
        zoomContent.margin(3f, 33f, 3f, 33f);
        zoomContent.add("Set Zoom", Styles.outlineLabel).left().growX();
        zoomContent.add(zoomValue).padLeft(10f).right();

        zoomSlider.changed(() -> {
            zoomThreshold = zoomSlider.getValue();
            zoomValue.setText(zoomThreshold <= 1.5f ? "Off" : String.format("%.1fx", zoomThreshold));
            zoomValue.setColor(zoomThreshold <= 1.5f ? Color.gray : Color.lightGray);
            Core.settings.put("visualizer.healthbar.zoomThreshold", zoomThreshold);
        });

        cont.stack(zoomSlider, zoomContent).width(width).left().padTop(4f).row();

        // ---Checkboxes ---
        // Condition for displaying health bar when zoom (default: false = hide health
        // bar when zoom in)

        cont.check(!zoomInverted ? "Zoom in" : "Zoom out", zoomInverted, b -> {
            zoomInverted = b;
            Core.settings.put("visualizer.healthbar.zoomInverted", zoomInverted);
        }).width(width).left().padTop(8f).row();
    }
}
