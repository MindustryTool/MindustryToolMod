package mindustrytool.visuals;

import arc.Core;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.Tmp;
import arc.math.geom.Point2;
import mindustry.ai.Pathfinder;

import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.Tile;

import static mindustry.Vars.*;

public class PathfindingVisualizer {

    private final int MAX_STEPS = 250;

    // Optimization: Staggered Tile Cache with Team Support
    private static class PathCache {
        float[] data; // Interleaved x, y
        int size; // Number of points * 2
        float lastUpdateTime;
    }

    // Map: (Packed Pos << 32 | CostType << 8 | Team ID) -> Path Data
    private final arc.struct.LongMap<PathCache> pathCache = new arc.struct.LongMap<>();
    // Use LongMap as Set since LongSet is missing
    private final arc.struct.LongMap<Object> activeTiles = new arc.struct.LongMap<>();

    public PathfindingVisualizer() {
        arc.util.Log.info("[PathfindingVisualizer] INITIALIZED. Constructor called.");
    }

    public void draw() {
        if (!state.isGame())
            return;

        Draw.z(mindustry.graphics.Layer.overlayUI);

        activeTiles.clear();

        int totalUnits = Groups.unit.size();

        // Adaptive Config
        int maxSteps = (totalUnits > 2000) ? 50 : (totalUnits > 1000) ? 100 : (totalUnits > 500) ? 150 : 250;
        boolean useCulling = totalUnits > 300;
        arc.math.geom.Rect cullBounds = useCulling ? Core.camera.bounds(Tmp.r1).grow(500f) : null;

        float now = Time.time;
        float updateInterval = 15f;

        for (Unit unit : Groups.unit) {
            if (unit.team == player.team())
                continue;
            // Removed isGrounded check to allow flying unit visualization
            // if (!unit.isGrounded()) continue;

            if (useCulling && !cullBounds.contains(unit.x, unit.y))
                continue;

            // CACHE KEY FIX: Pos(32) + CostType(8) + Team(8)
            long pos = Point2.pack(unit.tileX(), unit.tileY());
            long cacheKey = (((long) pos) << 32) | ((long) unit.type.flowfieldPathType << 8) | (long) unit.team.id;

            // Mark as active
            if (activeTiles.containsKey(cacheKey)) {
                continue;
            }
            activeTiles.put(cacheKey, null);

            // Check Cache
            PathCache entry = pathCache.get(cacheKey);

            // Recalculate if needed
            if (entry == null || (now - entry.lastUpdateTime) > updateInterval) {
                if (entry == null) {
                    entry = new PathCache();
                    entry.data = new float[MAX_STEPS * 2];
                    pathCache.put(cacheKey, entry);
                }
                recalculatePath(unit, entry, maxSteps);
                entry.lastUpdateTime = now + Mathf.random(5f);
            }

            // CRITICAL FIX: Always update start point
            if (entry.size >= 2) {
                entry.data[0] = unit.x;
                entry.data[1] = unit.y;
            }

            // Draw from Cache
            drawFromCache(entry, unit.team.color, maxSteps);
        }

        // Garbage Collection
        if (Time.time % 60 == 0) {
            arc.struct.LongSeq keysToRemove = new arc.struct.LongSeq();
            for (arc.struct.LongMap.Entry<PathCache> e : pathCache.entries()) {
                if (!activeTiles.containsKey(e.key)) {
                    keysToRemove.add(e.key);
                }
            }
            for (int i = 0; i < keysToRemove.size; i++)
                pathCache.remove(keysToRemove.get(i));
        }
    }

    private void recalculatePath(Unit unit, PathCache entry, int maxSteps) {
        if (pathfinder == null)
            return;
        Tile tile = unit.tileOn();
        if (tile == null)
            return;

        int costType = unit.type.flowfieldPathType;
        int fieldType = Pathfinder.fieldCore;
        var field = pathfinder.getField(unit.team, costType, fieldType);
        if (field == null) {
            entry.size = 0;
            return;
        }

        Tile current = tile;
        // Start point
        entry.data[0] = unit.x;
        entry.data[1] = unit.y;
        int idx = 2;

        for (int i = 0; i < maxSteps; i++) {
            Tile next = pathfinder.getTargetTile(current, field);
            if (next == null || next == current)
                break;

            if (idx >= entry.data.length - 2)
                break;

            entry.data[idx++] = next.worldx();
            entry.data[idx++] = next.worldy();

            current = next;
        }
        entry.size = idx;
    }

    private void drawFromCache(PathCache entry, Color col, int maxSteps) {
        if (entry.size < 4)
            return;

        Lines.stroke(1f);

        // Iterate through cached points
        float cx = entry.data[0];
        float cy = entry.data[1];

        int totalSegments = (entry.size / 2) - 1;
        if (totalSegments <= 0)
            return;

        for (int i = 0; i < totalSegments; i++) {
            float nx = entry.data[(i + 1) * 2];
            float ny = entry.data[(i + 1) * 2 + 1];

            Draw.color(col);
            Draw.alpha(1f - ((float) i / maxSteps));

            Lines.line(cx, cy, nx, ny);

            cx = nx;
            cy = ny;
        }
        Draw.reset();
    }
}
