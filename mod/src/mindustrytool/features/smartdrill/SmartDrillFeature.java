package mindustrytool.features.smartdrill;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.GameState.State;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.smartdrill.ui.SmartDrillSettingsDialog;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

import java.util.HashMap;

/**
 * Smart Drill feature: calculates and places optimized drill configurations on
 * tapped ore tiles with connected distribution bridges or ducts.
 * <p>
 * Activated exclusively through Quick Access toggle so that normal touch/drag
 * building (such as conveyor placement) remains completely unaffected when off.
 */
public class SmartDrillFeature extends Feature {

    public enum Direction {
        RIGHT(1, 0, 0),
        UP(0, 1, 1),
        LEFT(-1, 0, 2),
        DOWN(0, -1, 3);

        public final int x;
        public final int y;
        public final int rotation;

        Direction(int x, int y, int rotation) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }

        public boolean horizontal() {
            return this == RIGHT || this == LEFT;
        }

        public boolean vertical() {
            return this == UP || this == DOWN;
        }

        public Point2 mul(int i) {
            return new Point2(x * i, y * i);
        }

        public Direction opposite() {
            switch (this) {
                case RIGHT:
                    return LEFT;
                case UP:
                    return DOWN;
                case LEFT:
                    return RIGHT;
                case DOWN:
                    return UP;
                default:
                    return this;
            }
        }
    }

    public final ConfigGroup config;
    public final ConfigValue<Integer> maxTilesConfig;
    public final ConfigValue<Boolean> autoDisableConfig;

    private @Nullable SmartDrillSettingsDialog settingsDialog;
    private @Nullable Table currentMenu;
    private @Nullable Tile selectedTile;

    public SmartDrillFeature() {
        super(FeatureMetadata.builder()
                .id("smart-drill")
                .icon(FileIcon.of("pickaxe.png"))
                .order(11)
                .quickAccess(true)
                .enabledByDefault(false)
                .development(false)
                .build());

        this.config = configGroup();
        this.maxTilesConfig = config.intValue("maxTiles", 100);
        this.autoDisableConfig = config.boolValue("autoDisable", true);

        Events.on(TapEvent.class, this::handleTap);
        Events.on(StateChangeEvent.class, e -> {
            if (e.to == State.menu) {
                closeMenu();
            }
        });
    }

    public void resetToDefaults() {
        maxTilesConfig.reset();
        autoDisableConfig.reset();
    }

    @Override
    public void onEnable() {
        if (Vars.ui != null && Vars.ui.hudfrag != null && Core.bundle != null) {
            Vars.ui.hudfrag.showToast(Core.bundle.get("feature.smart-drill.active-hint",
                    "Smart Drill active: Tap an ore patch to plan drills."));
        }
    }

    @Override
    public void onDisable() {
        closeMenu();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new SmartDrillSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    private void handleTap(TapEvent e) {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame()
                || Vars.world == null || Vars.player == null) {
            return;
        }

        if (currentMenu != null && e.tile != selectedTile) {
            closeMenu();
            return;
        }

        Tile tile = e.tile;
        if (tile == null || tile.build != null) {
            return;
        }

        Item drop = tile.drop();
        Item wallDrop = tile.wallDrop();

        if (drop != null) {
            showDirectionMenu(tile, drop, false);
        } else if (wallDrop != null) {
            showDirectionMenu(tile, wallDrop, true);
        }
    }

    public void closeMenu() {
        if (currentMenu != null) {
            currentMenu.remove();
            currentMenu = null;
            selectedTile = null;
        }
    }

    private void showDirectionMenu(Tile tile, Item drop, boolean isBeam) {
        closeMenu();
        selectedTile = tile;

        currentMenu = new Table(Styles.black8);
        currentMenu.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null
                && Vars.ui.hudfrag.shown && Vars.state != null && Vars.state.isGame());
        currentMenu.touchable = Touchable.enabled;
        currentMenu.margin(4f);

        Table directionTable = new Table();

        // Up
        directionTable.add().size(44f);
        directionTable.button(Icon.up, () -> showDrillMenu(tile, drop, Direction.UP, isBeam)).size(44f).pad(2);
        directionTable.add().size(44f).row();

        // Left, Cancel, Right
        directionTable.button(Icon.left, () -> showDrillMenu(tile, drop, Direction.LEFT, isBeam)).size(44f).pad(2);
        directionTable.button(Icon.cancel, this::closeMenu).size(44f).pad(2);
        directionTable.button(Icon.right, () -> showDrillMenu(tile, drop, Direction.RIGHT, isBeam)).size(44f).pad(2).row();

        // Down
        directionTable.add().size(44f);
        directionTable.button(Icon.down, () -> showDrillMenu(tile, drop, Direction.DOWN, isBeam)).size(44f).pad(2);
        directionTable.add().size(44f);

        currentMenu.add(directionTable);
        attachMenuToHud();
    }

    private void showDrillMenu(Tile tile, Item drop, Direction direction, boolean isBeam) {
        if (currentMenu == null) {
            return;
        }

        currentMenu.clear();

        Seq<Block> drills = Vars.content.blocks()
                .select(b -> isBeam ? isValidBeamDrill(b, drop) : isValidDrill(b, drop));

        if (drills.isEmpty()) {
            if (Vars.ui != null && Vars.ui.hudfrag != null && Core.bundle != null) {
                Vars.ui.hudfrag.showToast(Core.bundle.get("feature.smart-drill.no-drills",
                        "No suitable unlocked drills for this ore."));
            }
            closeMenu();
            return;
        }

        int col = 0;
        for (Block drill : drills) {
            currentMenu.button(b -> {
                if (drill.uiIcon != null) {
                    b.image(drill.uiIcon).scaling(Scaling.fit).size(32f);
                }
            }, Styles.clearNonei, () -> {
                if (Vars.control != null && Vars.control.input != null) {
                    Vars.control.input.isBuilding = false;
                }
                closeMenu();
                Core.app.post(() -> executePlacement(tile, direction, drill, drop, isBeam));
            }).size(48f).pad(3).tooltip(drill.localizedName);

            col++;
            if (col % 4 == 0 && col < drills.size) {
                currentMenu.row();
            }
        }

        currentMenu.pack();
    }

    private void attachMenuToHud() {
        if (currentMenu == null) {
            return;
        }

        currentMenu.update(() -> {
            if (selectedTile == null || Vars.player == null || Core.camera == null) {
                closeMenu();
                return;
            }

            Vec2 pos = Core.camera.project(selectedTile.worldx(), selectedTile.worldy());
            float x = pos.x;
            float y = pos.y;

            if (Core.graphics != null && currentMenu != null) {
                float w = currentMenu.getWidth();
                float h = currentMenu.getHeight();
                x = Mathf.clamp(x, w / 2f + 8f, Core.graphics.getWidth() - w / 2f - 8f);
                y = Mathf.clamp(y, h / 2f + 8f, Core.graphics.getHeight() - h / 2f - 8f);
            }
            currentMenu.setPosition(x, y, Align.center);
        });

        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            Vars.ui.hudGroup.addChild(currentMenu);
            currentMenu.pack();
        }
    }

    private void executePlacement(Tile tile, Direction direction, Block drill, Item drop, boolean isBeam) {
        int count = 0;
        if (isBeam && drill instanceof BeamDrill) {
            count = placeBeamDrill(tile, direction, (BeamDrill) drill, drop);
        } else if (drill instanceof Drill) {
            count = placeFloorDrill(tile, direction, (Drill) drill, drop);
        }

        if (count > 0 && Vars.ui != null && Vars.ui.hudfrag != null && Core.bundle != null) {
            Vars.ui.hudfrag.showToast(Core.bundle.format("feature.smart-drill.placed",
                    count, drill.localizedName));
        }

        if (autoDisableConfig.get()) {
            disable();
        }
    }

    public boolean isValidDrill(@Nullable Block block, @Nullable Item drop) {
        if (!isUnlocked(block) || drop == null) {
            return false;
        }
        return block instanceof Drill && ((Drill) block).tier >= drop.hardness;
    }

    public boolean isValidBeamDrill(@Nullable Block block, @Nullable Item drop) {
        if (!isUnlocked(block) || drop == null) {
            return false;
        }
        return block instanceof BeamDrill && ((BeamDrill) block).tier >= drop.hardness;
    }

    public boolean isUnlocked(@Nullable Block block) {
        if (block == null) {
            return false;
        }
        if (Vars.state == null || Vars.state.rules == null) {
            return true;
        }
        return block.unlockedNowHost()
                && block.placeablePlayer
                && block.environmentBuildable()
                && block.supportsEnv(Vars.state.rules.env);
    }

    private int placeFloorDrill(Tile tile, Direction direction, Drill drill, Item drop) {
        if (drill.size != 2) {
            if (Vars.ui != null && Vars.ui.hudfrag != null && Core.bundle != null) {
                Vars.ui.hudfrag.showToast(Core.bundle.get("feature.smart-drill.unsupported-size",
                        "Auto-placement for this drill size is not supported yet."));
            }
            return 0;
        }
        return place2x2Drill(tile, direction, drill, drop);
    }

    private int place2x2Drill(Tile tile, Direction direction, Block drill, Item drop) {
        Unit unit = Vars.player != null ? Vars.player.unit() : null;
        if (unit == null) {
            return 0;
        }

        int maxTiles = maxTilesConfig.get();
        Seq<Tile> tiles = findAllConnectedOreTiles(tile, drop, maxTiles);
        if (tiles.isEmpty()) {
            return 0;
        }

        tiles.retainAll(t -> t.drop() == drop);
        expandTiles(tiles, 3);

        Seq<Tile> drillTiles = tiles.select(this::isDrillTile);
        Seq<Tile> bridgeTiles = tiles.select(this::isBridgeTile);

        int placed = 0;
        for (Tile drillTile : drillTiles) {
            BuildPlan plan = new BuildPlan(drillTile.x, drillTile.y, direction.rotation, drill);
            if (plan.placeable(Vars.player.team())) {
                unit.addBuild(plan);
                placed++;
            }
        }

        Tile outMostTile = tiles.max(t -> {
            switch (direction) {
                case UP:
                    return t.y;
                case DOWN:
                    return -t.y;
                case LEFT:
                    return -t.x;
                case RIGHT:
                    return t.x;
                default:
                    return 0;
            }
        });

        if (outMostTile != null && !bridgeTiles.isEmpty()) {
            bridgeTiles.sort(t -> t.dst2(outMostTile));
            Tile output = bridgeTiles.first().nearby(direction.mul(3));
            Tile outputBridge = output != null ? output : bridgeTiles.first();
            bridgeTiles.add(outputBridge);
            bridgeTiles.sort(t -> t.dst2(outputBridge));

            for (Tile bridgeTile : bridgeTiles) {
                Tile neighbor = bridgeTiles.find(t -> Math.abs(t.x - bridgeTile.x) + Math.abs(t.y - bridgeTile.y) == 3);
                Point2 bridgeConfig = new Point2();
                if (neighbor != null && bridgeTile != outputBridge) {
                    bridgeConfig.set(neighbor.x - bridgeTile.x, neighbor.y - bridgeTile.y);
                }
                BuildPlan plan = new BuildPlan(bridgeTile.x, bridgeTile.y, 0, Blocks.itemBridge, bridgeConfig);
                if (plan.placeable(Vars.player.team())) {
                    unit.addBuild(plan);
                    placed++;
                }
            }
        }

        return placed;
    }

    private int placeBeamDrill(Tile tile, Direction direction, BeamDrill drill, Item drop) {
        Unit unit = Vars.player != null ? Vars.player.unit() : null;
        if (unit == null) {
            return 0;
        }

        int maxTiles = maxTilesConfig.get();
        Seq<Tile> ores = findAllConnectedWallOreTiles(tile, drop, maxTiles);
        if (ores.isEmpty()) {
            return 0;
        }

        Direction opposite = direction.opposite();
        HashMap<Integer, Boolean> hasDrill = new HashMap<>();
        Seq<BuildPlan> drillPlans = new Seq<>();
        int half = (drill.size - 1) / 2;
        int placed = 0;

        for (Tile ore : ores) {
            int gridx = (ore.x / drill.size) * drill.size;
            int gridy = (ore.y / drill.size) * drill.size;
            int key = direction.horizontal() ? gridy : gridx;

            if (hasDrill.containsKey(key)) {
                continue;
            }
            hasDrill.put(key, true);

            for (int i = 1; i < drill.range; i++) {
                int reach = half + 1;
                int x = gridx + opposite.mul(i).x + half;
                int y = gridy + opposite.mul(i).y + half;

                BuildPlan drillPlan = new BuildPlan(x, y, direction.rotation, drill);
                int nodeOffX = direction.horizontal()
                        ? (direction == Direction.LEFT ? (reach + (drill.size % 2 == 0 ? 1 : 0)) : -reach)
                        : 0;
                int nodeOffY = direction.vertical()
                        ? reach * (direction == Direction.DOWN ? (reach + (drill.size % 2 == 0 ? 1 : 0)) : -reach)
                        : 0;

                int ductOffX = nodeOffX == 0 ? 1 : 0;
                int ductOffY = nodeOffY == 0 ? 1 : 0;

                BuildPlan powerNodePlan = new BuildPlan(x + nodeOffX, y + nodeOffY, direction.rotation, Blocks.beamNode);
                BuildPlan drillDuctPlan = new BuildPlan(x + nodeOffX + ductOffX, y + nodeOffY + ductOffY,
                        opposite.rotation, Blocks.duct);

                if (drillPlan.placeable(Vars.player.team())
                        && powerNodePlan.placeable(Vars.player.team())
                        && drillDuctPlan.placeable(Vars.player.team())) {
                    unit.addBuild(drillPlan);
                    unit.addBuild(powerNodePlan);
                    unit.addBuild(drillDuctPlan);
                    drillPlans.add(drillPlan);
                    placed += 3;
                    break;
                }
            }
        }

        return placed;
    }

    public Seq<Tile> findAllConnectedOreTiles(Tile start, Item drop, int maxTiles) {
        Seq<Tile> tiles = new Seq<>();
        Seq<Tile> queue = new Seq<>();
        ObjectSet<Tile> visited = new ObjectSet<>();

        int centerX = start.x;
        int centerY = start.y;

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && tiles.size < maxTiles) {
            queue.sort(t -> {
                float dx = Math.abs(t.x - centerX);
                float dy = Math.abs(t.y - centerY);
                float diff = Math.abs(dx - dy);
                return Math.max(dx * dx + diff, dy * dy + diff);
            });

            Tile tile = queue.remove(0);
            tiles.add(tile);

            for (int i = 0; i < 4; i++) {
                Tile neighbor = tile.nearby(i);
                if (neighbor == null || visited.contains(neighbor)
                        || (neighbor.drop() != drop && neighbor.wallDrop() != drop)) {
                    continue;
                }
                visited.add(neighbor);
                queue.add(neighbor);
            }
        }

        return tiles;
    }

    public Seq<Tile> findAllConnectedWallOreTiles(Tile start, Item drop, int maxTiles) {
        Seq<Tile> tiles = new Seq<>();
        Seq<Tile> queue = new Seq<>();
        ObjectSet<Tile> visited = new ObjectSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && tiles.size < maxTiles) {
            Tile tile = queue.remove(0);
            tiles.add(tile);

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if (x == 0 && y == 0) {
                        continue;
                    }
                    Tile neighbor = tile.nearby(x, y);
                    if (neighbor == null || visited.contains(neighbor) || neighbor.wallDrop() != drop) {
                        continue;
                    }
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return tiles;
    }

    public void expandTiles(Seq<Tile> tiles, int times) {
        for (int i = 0; i < times; i++) {
            expandTiles(tiles);
        }
    }

    private void expandTiles(Seq<Tile> tiles) {
        Seq<Tile> newTiles = new Seq<>();
        for (Tile tile : tiles) {
            for (int i = 0; i < 4; i++) {
                Tile neighbor = tile.nearby(i);
                if (neighbor == null || tiles.contains(neighbor)) {
                    continue;
                }
                newTiles.addUnique(neighbor);
            }
        }
        tiles.addAll(newTiles);
    }

    public boolean isDrillTile(Tile tile) {
        switch (tile.x % 6) {
            case 0:
            case 2:
                if ((tile.y - 1) % 6 == 0) {
                    return true;
                }
                break;
            case 1:
                if ((tile.y - 3) % 6 == 0 || (tile.y - 3) % 6 == 2) {
                    return true;
                }
                break;
            case 3:
            case 5:
                if ((tile.y - 4) % 6 == 0) {
                    return true;
                }
                break;
            case 4:
                if (tile.y % 6 == 0 || tile.y % 6 == 2) {
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    public boolean isBridgeTile(Tile tile) {
        return tile.x % 3 == 0 && tile.y % 3 == 0;
    }
}
