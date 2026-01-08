package mindustrytool.features.display.pathfinding;

import arc.Core;

public class PathfindingConfig {
    private static final String ZOOM_THRESHOLD_KEY = "mindustrytool.pathfinding.zoomThreshold";
    private static final String DRAW_UNIT_PATH_KEY = "mindustrytool.pathfinding.drawUnitPath";
    private static final String DRAW_SPAWN_POINT_PATH_KEY = "mindustrytool.pathfinding.drawSpawnPointPath";

    private float zoomThreshold;
    private boolean drawUnitPath;
    private boolean drawSpawnPointPath;

    public void load() {
        zoomThreshold = Core.settings.getFloat(ZOOM_THRESHOLD_KEY, 0.5f);
        drawUnitPath = Core.settings.getBool(DRAW_UNIT_PATH_KEY, true);
        drawSpawnPointPath = Core.settings.getBool(DRAW_SPAWN_POINT_PATH_KEY, true);
    }

    public void save() {
        Core.settings.put(ZOOM_THRESHOLD_KEY, zoomThreshold);
        Core.settings.put(DRAW_UNIT_PATH_KEY, drawUnitPath);
        Core.settings.put(DRAW_SPAWN_POINT_PATH_KEY, drawSpawnPointPath);
    }

    public float getZoomThreshold() {
        return zoomThreshold;
    }

    public void setZoomThreshold(float zoomThreshold) {
        this.zoomThreshold = zoomThreshold;
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
