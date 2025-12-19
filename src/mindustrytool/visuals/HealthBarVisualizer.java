package mindustrytool.visuals;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;

import static mindustry.Vars.*;

import arc.graphics.g2d.TextureRegion;
import arc.func.Cons;

public class HealthBarVisualizer {

    private static boolean enabled = false;
    private static HealthBarVisualizer instance;
    private static TextureRegion barRegion;

    // Cached callback to avoid allocation per frame
    private final Cons<Unit> drawUnitRef = this::drawCheck;

    static {
        Events.run(EventType.Trigger.draw, () -> {
            if (enabled && instance != null) {
                instance.draw();
            }
        });
    }

    public HealthBarVisualizer() {
        arc.util.Log.info("[HealthBarVisualizer] INITIALIZED.");
        instance = this;
        enabled = true;
        // Cache the region once
        if (barRegion == null) {
            barRegion = Core.atlas.find("white-ui");
            if (barRegion == null || !barRegion.found()) {
                barRegion = Core.atlas.white(); // Fallback
            }
        }
    }

    public void dispose() {
        arc.util.Log.info("[HealthBarVisualizer] DISPOSED.");
        enabled = false;
        instance = null;
    }

    private void draw() {
        if (!state.isGame())
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
}
