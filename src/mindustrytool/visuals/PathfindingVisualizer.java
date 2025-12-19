package mindustrytool.visuals;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.ai.Pathfinder;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.Tile;

import static mindustry.Vars.*;

public class PathfindingVisualizer {

    private final int MAX_STEPS = 250; // Base length

    private static boolean enabled = false;
    private static PathfindingVisualizer instance;

    static {
        // Register ONCE globally
        Events.run(EventType.Trigger.draw, () -> {
            if (enabled && instance != null) {
                instance.draw();
            }
        });
    }

    public PathfindingVisualizer() {
        arc.util.Log.info("[PathfindingVisualizer] INITIALIZED. Constructor called.");
        instance = this;
        enabled = true;
    }

    public void dispose() {
        arc.util.Log.info("[PathfindingVisualizer] DISPOSED.");
        enabled = false;
        instance = null;
    }

    private void draw() {
        if (!state.isGame())
            return;

        Draw.z(mindustry.graphics.Layer.overlayUI); // Draw on top to be sure

        // ADAPTIVE OPTIMIZATION
        int totalUnits = Groups.unit.size();

        // 1. Dynamic Step Reduction: Shorten paths if too many units
        // < 500 units: 250 steps (Full Quality)
        // > 1000 units: 100 steps
        // > 2000 units: 50 steps (Max Performance)
        int currentMaxSteps = (totalUnits > 2000) ? 50 : (totalUnits > 1000) ? 100 : (totalUnits > 500) ? 150 : 250;

        // 2. Dynamic Culling: Enable Wide Culling only if density is high
        boolean useCulling = totalUnits > 300;
        // Wide bounds: Screen + 500 padding (allows seeing paths slightly off-screen)
        arc.math.geom.Rect cullBounds = useCulling ? Core.camera.bounds(Tmp.r1).grow(500f) : null;

        for (Unit unit : Groups.unit) {
            // FILTER: Don't draw for player's own team
            if (unit.team == player.team())
                continue;

            // Only draw if grounded
            if (!unit.isGrounded())
                continue;

            // CULLING CHECK
            if (useCulling && !cullBounds.contains(unit.x, unit.y))
                continue;

            drawUnitPath(unit, currentMaxSteps);
        }
    }

    private void drawUnitPath(Unit unit, int maxSteps) {
        if (pathfinder == null)
            return;
        Tile tile = unit.tileOn();
        if (tile == null)
            return;

        int costType = unit.type.flowfieldPathType;
        int fieldType = Pathfinder.fieldCore;
        var field = pathfinder.getField(unit.team, costType, fieldType);
        if (field == null)
            return;

        // ULTRA-PERFORMANCE MODE with Dynamic Steps

        Lines.stroke(1f);
        Tile current = tile;
        float currentX = unit.x;
        float currentY = unit.y;

        Color col = unit.team.color;

        for (int i = 0; i < maxSteps; i++) {
            Tile next = pathfinder.getTargetTile(current, field);
            if (next == null || next == current)
                break;

            float nextX = next.worldx();
            float nextY = next.worldy();

            // Fade out
            Draw.color(col);
            Draw.alpha(1f - ((float) i / maxSteps));

            Lines.line(currentX, currentY, nextX, nextY);

            current = next;
            currentX = nextX;
            currentY = nextY;
        }

        Draw.reset();
    }
}
