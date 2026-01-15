package mindustrytool.features.display.pathfinding;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.struct.LongMap;
import arc.struct.LongSeq;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.ai.Pathfinder;
import mindustry.content.Blocks;
import mindustry.game.EventType.TileOverlayChangeEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Optional;

import static mindustry.Vars.*;

public class PathfindingDisplay implements Feature {
    private final PathfindingConfig config = new PathfindingConfig();
    private final LongMap<PathfindingCache> pathCache = new LongMap<>();
    private final LongMap<Object> activeTiles = new LongMap<>();

    private Seq<Tile> spawns = new Seq<>(false);
    private boolean isEnabled;
    private BaseDialog settingsDialog;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("Pathfinding Visualizer")
                .description("Visualizes unit pathfinding paths.")
                .icon(Iconc.commandRally)
                .order(0)
                .enabledByDefault(true)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        config.load();
        Events.run(Trigger.draw, this::draw);

        Events.on(WorldLoadEvent.class, e -> reset());
        Events.on(TileOverlayChangeEvent.class, e -> {
            if (e.previous == Blocks.spawn) {
                spawns.remove(e.tile);
            }

            if (e.overlay == Blocks.spawn) {
                spawns.add(e.tile);
            }
        });
    }

    public void reset() {
        spawns.clear();

        for (Tile tile : world.tiles) {
            if (tile.overlay() == Blocks.spawn) {
                spawns.add(tile);
            }
        }
    }

    @Override
    public void onEnable() {
        isEnabled = true;
    }

    @Override
    public void onDisable() {
        isEnabled = false;
        pathCache.clear();
        activeTiles.clear();
    }

    @Override
    public Optional<Dialog> setting() {
        if (settingsDialog == null) {
            settingsDialog = new BaseDialog("Pathfinding Settings");
            settingsDialog.addCloseButton();
            settingsDialog.shown(this::rebuildSettings);
            settingsDialog.buttons.button("Reset", Icon.refresh, () -> {
                config.setZoomThreshold(0.5f);
                rebuildSettings();
            }).size(250, 64);
        }
        return Optional.of(settingsDialog);
    }

    private void rebuildSettings() {
        Table settingsContainer = settingsDialog.cont;
        settingsContainer.clear();
        settingsContainer.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);
        float currentZoom = config.getZoomThreshold();

        Slider zoomSlider = new Slider(0f, 2f, 0.1f, false);
        zoomSlider.setValue(currentZoom);

        Label zoomValueLabel = new Label(
                currentZoom <= 0.01f ? "Off" : String.format("%.1fx", currentZoom),
                Styles.outlineLabel);
        zoomValueLabel.setColor(currentZoom <= 0.01f ? Color.gray : Color.lightGray);

        Table zoomContent = new Table();
        zoomContent.touchable = arc.scene.event.Touchable.disabled;
        zoomContent.margin(3f, 33f, 3f, 33f);
        zoomContent.add("Min Zoom", Styles.outlineLabel).left().growX();
        zoomContent.add(zoomValueLabel).padLeft(10f).right();

        zoomSlider.changed(() -> {
            float newZoomValue = zoomSlider.getValue();
            config.setZoomThreshold(newZoomValue);
            zoomValueLabel.setText(newZoomValue <= 0.01f ? "Off" : String.format("%.1fx", newZoomValue));
            zoomValueLabel.setColor(newZoomValue <= 0.01f ? Color.gray : Color.lightGray);
        });

        settingsContainer.stack(zoomSlider, zoomContent).width(width).left().padTop(4f).row();

        settingsContainer.check("Draw Unit Path", config.isDrawUnitPath(), (checked) -> {
            config.setDrawUnitPath(checked);
        }).left().row();

        settingsContainer.check("Draw Spawn Point Path", config.isDrawSpawnPointPath(), (checked) -> {
            config.setDrawSpawnPointPath(checked);
            rebuildSettings();
        }).left().row();

        if (config.isDrawSpawnPointPath()) {
            Table costTable = new Table();
            costTable.left().defaults().left().padLeft(16);

            String[] costNames = { "Ground", "Legs", "Water", "Neoplasm", "Flat", "Hover" };

            for (int i = 0; i < costNames.length; i++) {
                int index = i;
                costTable.check(costNames[i], config.isCostTypeEnabled(index), c -> {
                    config.setCostTypeEnabled(index, c);
                }).padBottom(4).row();
            }

            settingsContainer.add(costTable).left().row();
        }
    }

    private void draw() {
        if (!isEnabled || !state.isGame()) {
            return;
        }

        float zoomThreshold = config.getZoomThreshold();
        float currentZoom = renderer.getScale();

        if (zoomThreshold > 0 && currentZoom < zoomThreshold) {
            return;
        }

        if (config.isDrawSpawnPointPath()) {
            drawSpawnPointPath();
        }

        if (config.isDrawUnitPath()) {
            drawUnitPath();
        }
    }

    private void drawUnitPath() {
        Draw.z(Layer.overlayUI);
        activeTiles.clear();

        int totalUnits = Groups.unit.size();
        int maxSteps = (totalUnits > 2000) ? 50 : (totalUnits > 1000) ? 100 : (totalUnits > 500) ? 150 : 250;
        boolean useCulling = totalUnits > 300;
        Rect cullBounds = useCulling ? Core.camera.bounds(Tmp.r1).grow(500f) : null;
        float currentTime = Time.time;

        for (Unit unit : Groups.unit) {
            if (unit.team == player.team()) {
                continue;
            }

            if (useCulling && !cullBounds.contains(unit.x, unit.y)) {
                continue;
            }

            long packedPosition = Point2.pack(unit.tileX(), unit.tileY());
            long cacheKey = (((long) packedPosition) << 32) | ((long) unit.type.flowfieldPathType << 8)
                    | (long) unit.team.id;

            if (activeTiles.containsKey(cacheKey)) {
                continue;
            }

            activeTiles.put(cacheKey, null);

            PathfindingCache cacheEntry = pathCache.get(cacheKey);

            if (cacheEntry == null || (currentTime - cacheEntry.lastUpdateTime) > 15f) {
                if (cacheEntry == null) {
                    cacheEntry = new PathfindingCache();
                    cacheEntry.data = new float[250 * 2];
                    pathCache.put(cacheKey, cacheEntry);
                }
                recalculatePath(unit, cacheEntry, maxSteps);
                cacheEntry.lastUpdateTime = currentTime + Mathf.random(3f, 8f);
            }

            if (cacheEntry.size >= 2) {
                cacheEntry.data[0] = unit.x;
                cacheEntry.data[1] = unit.y;
            }

            drawFromCache(cacheEntry, unit.team.color, maxSteps);
        }

        if (Time.time % 60 == 0) {
            LongSeq keysToRemove = new LongSeq();
            for (LongMap.Entry<PathfindingCache> entry : pathCache.entries()) {
                if (!activeTiles.containsKey(entry.key)) {
                    keysToRemove.add(entry.key);
                }
            }

            for (int i = 0; i < keysToRemove.size; i++) {
                pathCache.remove(keysToRemove.get(i));
            }
        }
    }

    private void recalculatePath(Unit unit, PathfindingCache cacheEntry, int maxSteps) {
        if (pathfinder == null) {
            return;
        }

        Tile tile = unit.tileOn();
        if (tile == null) {
            return;
        }

        int costType = unit.type.flowfieldPathType;
        int fieldType = Pathfinder.fieldCore;
        Pathfinder.Flowfield field = pathfinder.getField(unit.team, costType, fieldType);

        if (field == null) {
            cacheEntry.size = 0;
            return;
        }

        Tile currentTile = tile;
        cacheEntry.data[0] = unit.x;
        cacheEntry.data[1] = unit.y;
        int dataIndex = 2;

        for (int i = 0; i < maxSteps; i++) {
            Tile nextTile = pathfinder.getTargetTile(currentTile, field);
            if (nextTile == null || nextTile == currentTile)
                break;
            if (dataIndex >= cacheEntry.data.length - 2)
                break;

            cacheEntry.data[dataIndex++] = nextTile.worldx();
            cacheEntry.data[dataIndex++] = nextTile.worldy();
            currentTile = nextTile;
        }
        cacheEntry.size = dataIndex;
    }

    private void drawFromCache(PathfindingCache cacheEntry, Color pathColor, int maxSteps) {
        if (cacheEntry.size < 4)
            return;

        Lines.stroke(1f);
        float currentX = cacheEntry.data[0];
        float currentY = cacheEntry.data[1];
        int totalSegments = (cacheEntry.size / 2) - 1;

        if (totalSegments <= 0)
            return;

        for (int i = 0; i < totalSegments; i++) {
            float nextX = cacheEntry.data[(i + 1) * 2];
            float nextY = cacheEntry.data[(i + 1) * 2 + 1];

            Draw.color(pathColor);
            Draw.alpha(1f - ((float) i / maxSteps));
            Lines.line(currentX, currentY, nextX, nextY);

            currentX = nextX;
            currentY = nextY;
        }
        Draw.reset();
    }

    private void drawSpawnPointPath() {
        if (pathfinder == null) {
            return;
        }

        Draw.z(Layer.overlayUI);

        final int MAX_STEPS = 1000;

        for (var spawnPoint : Vars.state.rules.spawns) {
            var team = spawnPoint.team == null ? Vars.state.rules.waveTeam : spawnPoint.team;

            if (team == player.team()) {
                continue;
            }

            Draw.color(team.color);
            Draw.alpha(0.5f);
            Lines.stroke(2f);

            for (var costType = 0; costType < Pathfinder.costTypes.size; costType++) {
                if (!config.isCostTypeEnabled(costType)) {
                    continue;
                }

                for (var spawnTile : spawns) {

                    Lines.line(spawnTile.worldx(), spawnTile.worldy(), player.x, player.y);

                    int fieldType = Pathfinder.fieldCore;
                    Pathfinder.Flowfield field = pathfinder.getField(team, costType, fieldType);

                    if (field == null) {
                        return;
                    }

                    Tile currentTile = spawnTile;
                    float lastX = spawnTile.worldx();
                    float lastY = spawnTile.worldy();

                    for (int i = 0; i < MAX_STEPS; i++) {
                        Tile nextTile = pathfinder.getTargetTile(currentTile, field);
                        if (nextTile == null || nextTile == currentTile) {
                            break;
                        }

                        Lines.line(lastX, lastY, nextTile.worldx(), nextTile.worldy());
                        lastX = nextTile.worldx();
                        lastY = nextTile.worldy();
                        currentTile = nextTile;
                    }
                }
            }
        }
    }
}
