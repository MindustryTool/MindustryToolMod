package mindustrytool.features.display.pathfinding;

import arc.Core;

public class PathfindingConfig {
    private static final String ZOOM_THRESHOLD_KEY = "mindustrytool.pathfinding.zoomThreshold";
    private static final String OPACITY_KEY = "mindustrytool.pathfinding.opacity";
    private static final String DRAW_UNIT_PATH_KEY = "mindustrytool.pathfinding.drawUnitPath";
    private static final String DRAW_SPAWN_POINT_PATH_KEY = "mindustrytool.pathfinding.drawSpawnPointPath";
    private static final String COST_TYPE_PREFIX = "mindustrytool.pathfinding.costType.";

    private float zoomThreshold;
    private float opacity;
    private boolean drawUnitPath;
    private boolean drawSpawnPointPath;
    private final boolean[] costTypesEnabled = new boolean[6];

    public void load() {
        zoomThreshold = Core.settings.getFloat(ZOOM_THRESHOLD_KEY, 0.5f);
        opacity = Core.settings.getFloat(OPACITY_KEY, 1f);
        drawUnitPath = Core.settings.getBool(DRAW_UNIT_PATH_KEY, true);
        drawSpawnPointPath = Core.settings.getBool(DRAW_SPAWN_POINT_PATH_KEY, true);
        for (int i = 0; i < costTypesEnabled.length; i++) {
            costTypesEnabled[i] = Core.settings.getBool(COST_TYPE_PREFIX + i, true);
        }
    }

    public void save() {
        Core.settings.put(ZOOM_THRESHOLD_KEY, zoomThreshold);
        Core.settings.put(OPACITY_KEY, opacity);
        Core.settings.put(DRAW_UNIT_PATH_KEY, drawUnitPath);
        Core.settings.put(DRAW_SPAWN_POINT_PATH_KEY, drawSpawnPointPath);
        for (int i = 0; i < costTypesEnabled.length; i++) {
            Core.settings.put(COST_TYPE_PREFIX + i, costTypesEnabled[i]);
        }
    }

    public boolean isCostTypeEnabled(int index) {
        if (index < 0 || index >= costTypesEnabled.length)
            throw new IllegalArgumentException("Invalid cost type index: " + index);

        return costTypesEnabled[index];
    }

    public void setCostTypeEnabled(int index, boolean enabled) {
        if (index < 0 || index >= costTypesEnabled.length)
            return;
        costTypesEnabled[index] = enabled;
        save();
    }

    public float getZoomThreshold() {
        return zoomThreshold;
    }

    public void setZoomThreshold(float zoomThreshold) {
        this.zoomThreshold = zoomThreshold;
        save();
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
        save();
    }

    public boolean isDrawUnitPath() {
        return drawUnitPath;
    }

    public void setDrawUnitPath(boolean drawUnitPath) {
        this.drawUnitPath = drawUnitPath;
        save();
    }

    public boolean isDrawSpawnPointPath() {
        return drawSpawnPointPath;
    }

    public void setDrawSpawnPointPath(boolean drawSpawnPointPath) {
        this.drawSpawnPointPath = drawSpawnPointPath;
        save();
    }
}
