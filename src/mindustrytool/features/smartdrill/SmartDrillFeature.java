package mindustrytool.features.smartdrill;

import arc.Core;
import arc.Events;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Scaling;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.GameState.State;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Icon;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.services.TapListener;
import arc.scene.ui.Dialog;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SmartDrillFeature implements Feature {
    private Table currentMenu;
    private Tile selectedTile;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("feature.smart-drill")
                .description("feature.smart-drill.description")
                .icon(Icon.filter)
                .quickAccess(true)
                .build();
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.of(new SmartDrillSettingDialog());
    }

    public static int getMaxTiles(Block drill) {
        return Core.settings.getInt("mindustrytool.smart-drill.max-tiles." + drill.name, 100);
    }

    @Override
    public void init() {
        TapListener.getInstance().registerHoldListener(300, 10, null, (tile, data) -> {
            if (!isEnabled() || tile == null || tile.build != null || tile.drop() == null) {
                return;
            }
            if (currentMenu == null) {
                handleHold(tile);
            }
        });

        Events.on(TapEvent.class, e -> {
            if (!isEnabled()) {
                return;
            }

            if (currentMenu != null && e.tile != selectedTile) {
                closeMenu();
            }
        });

        Events.on(StateChangeEvent.class, e -> {
            if (e.to == State.menu) {
                closeMenu();
            }
        });
    }

    @Override
    public void onDisable() {
        closeMenu();
    }

    private void handleHold(Tile tile) {
        Item drop = tile.drop();
        Item wallDrop = tile.wallDrop();

        if (drop != null) {
            showDirectionMenu(tile, drop)
                    .thenAccept(direction -> {
                        if (direction == null) {
                            return;
                        }
                        showDrillMenu(tile, drop, direction, false);
                    });
        } else if (wallDrop != null) {
            showDirectionMenu(tile, wallDrop)
                    .thenAccept(direction -> {
                        if (direction == null) {
                            return;
                        }
                        showDrillMenu(tile, wallDrop, direction, true);
                    });
        } else {
            closeMenu();
        }
    }

    private void closeMenu() {
        if (currentMenu != null) {
            currentMenu.remove();
            currentMenu = null;
            selectedTile = null;
        }
    }

    private CompletableFuture<Direction> showDirectionMenu(Tile tile, Item drop) {
        closeMenu();

        CompletableFuture<Direction> future = new CompletableFuture<>();

        selectedTile = tile;
        currentMenu = new Table();
        currentMenu.visible(() -> Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown);
        currentMenu.touchable = arc.scene.event.Touchable.enabled;

        currentMenu.update(() -> {
            if (selectedTile == null) {
                closeMenu();
                future.complete(null);
                return;
            }

            Vec2 pos = Core.camera.project(selectedTile.worldx(), selectedTile.worldy());
            currentMenu.setPosition(pos.x, pos.y, Align.center);
        });

        Table directionTable = new Table();

        // Up
        directionTable.add().size(48f);
        directionTable.button(Icon.up, () -> future.complete(Direction.UP)).size(48f).pad(4);
        directionTable.add().size(48f).row();

        // Left, Cancel, Right
        directionTable.button(Icon.left, () -> future.complete(Direction.LEFT)).size(48f).pad(4);
        directionTable.button(Icon.cancel, () -> {
            future.complete(null);
            closeMenu();
        }).size(48f).pad(4);
        directionTable.button(Icon.right, () -> future.complete(Direction.RIGHT)).size(48f).pad(4).row();

        // Down
        directionTable.add().size(48f);
        directionTable.button(Icon.down, () -> future.complete(Direction.DOWN)).size(48f).pad(4);
        directionTable.add().size(48f);

        currentMenu.add(directionTable);

        Vars.ui.hudGroup.addChild(currentMenu);
        Timer.schedule(() -> {
            if (currentMenu != null) {
                currentMenu.toFront();
            }
        }, 0.1f);
        currentMenu.pack();

        return future;
    }

    private void showDrillMenu(Tile tile, Item drop, Direction direction, boolean isBeam) {
        if (currentMenu == null) {
            return;
        }

        currentMenu.clear();

        int i = 0;

        for (Block block : Vars.content.blocks()
                .select(block -> isBeam ? isValidBeamDrill(block, drop) : isValidDrill(block, drop))) {
            currentMenu.button(b -> b.image(block.uiIcon).scaling(Scaling.fit), Styles.clearNonei, () -> {
                Vars.control.input.isBuilding = false;
                Core.app.post(() -> {
                    if (isBeam) {
                        placeBeamDrill(tile, direction, (BeamDrill) block, drop);
                    } else {
                        placeDrill(tile, direction, (Drill) block, drop);
                    }
                });
                closeMenu();
            }).size(48f).pad(4);

            if (++i % 4 == 0) {
                currentMenu.row();
            }
        }

        if (i == 0) {
            currentMenu.add("@none").pad(8);
        }

        currentMenu.pack();
        currentMenu.toFront();
    }

    private boolean isValidDrill(Block block, Item drop) {
        if (!unlocked(block))
            return false;
        if (block instanceof Drill drill)
            return drill.tier >= drop.hardness;
        return false;
    }

    private boolean isValidBeamDrill(Block block, Item drop) {
        if (!unlocked(block))
            return false;
        if (block instanceof BeamDrill beamDrill)
            return beamDrill.tier >= drop.hardness;
        return false;
    }

    private boolean unlocked(Block block) {
        return block.unlockedNowHost() && block.placeablePlayer && block.environmentBuildable() &&
                block.supportsEnv(Vars.state.rules.env);
    }

    private void placeDrill(Tile tile, Direction direction, Drill drill, Item drop) {
        switch (drill.size) {
            case 1:
            default:
                Vars.ui.showInfoFade("Not supported");
                break;

            case 2:
                place2x2Drill(tile, direction, drill, drop);
                break;

            case 3:
                Vars.ui.showInfoFade("Not supported");
                break;

            case 4:
                Vars.ui.showInfoFade("Not supported");
                break;
        }
    }

    private void placeBeamDrill(Tile tile, Direction direction, BeamDrill drill, Item drop) {
        Seq<Tile> ores = findAllConnectedOreTiles(tile, drop, getMaxTiles(drill));
        if (ores.isEmpty()) {
            return;
        }
    }

    private void place2x2Drill(Tile tile, Direction direction, Block drill, Item drop) {
        var unit = Vars.player.unit();

        if (unit == null) {
            return;
        }

        Seq<Tile> tiles = findAllConnectedOreTiles(tile, drop, getMaxTiles(drill));

        if (tiles.isEmpty()) {
            return;
        }

        tiles.retainAll(t -> t.drop() == drop);

        expandTiles(tiles, 2);

        var drillTiles = tiles.select(this::isDrillTile);
        var bridgeTiles = tiles.select(this::isBridgeTile);

        for (Tile drillTile : drillTiles) {
            BuildPlan plan = new BuildPlan(drillTile.x, drillTile.y, direction.rotation, drill);
            if (plan.placeable(Vars.player.team())) {
                unit.addBuild(plan);
            }
        }

        var outMostTile = tiles.max(t -> {
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

        bridgeTiles.sort(t -> t.dst2(outMostTile));
        var output = bridgeTiles.first().nearby(direction.mul(3));
        if (output == null) {
            output = bridgeTiles.first();
        }
        var outputBridge = output;
        bridgeTiles.add(output);
        bridgeTiles.sort(t -> t.dst2(outputBridge));

        for (Tile bridgeTile : bridgeTiles) {
            Tile neighbor = bridgeTiles.find(t -> Math.abs(t.x - bridgeTile.x) + Math.abs(t.y - bridgeTile.y) == 3);
            Point2 config = new Point2();
            if (neighbor != null && bridgeTile != outputBridge) {
                config.set(neighbor.x - bridgeTile.x, neighbor.y - bridgeTile.y);
            }
            BuildPlan plan = new BuildPlan(bridgeTile.x, bridgeTile.y, 0, Blocks.itemBridge, config);
            if (plan.placeable(Vars.player.team())) {
                unit.addBuild(plan);
            }
        }
    }

    private Seq<Tile> findAllConnectedOreTiles(Tile start, Item drop, int maxTiles) {
        Seq<Tile> tiles = new Seq<>();
        Seq<Tile> queue = new Seq<>();
        ObjectSet<Tile> visited = new ObjectSet<>();

        int centerX = start.x;
        int centerY = start.y;

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && tiles.size < maxTiles) {

            queue.sort(t -> {
                int dx = Math.abs(t.x - centerX);
                int dy = Math.abs(t.y - centerY);

                return Math.max(dx, dy);
            });

            Tile tile = queue.remove(0);
            tiles.add(tile);

            for (int i = 0; i < 4; i++) {
                Tile neighbor = tile.nearby(i);

                if (neighbor == null || visited.contains(neighbor) || neighbor.drop() != drop) {
                    continue;
                }

                visited.add(neighbor);
                queue.add(neighbor);
            }
        }

        return tiles;
    }

    private void expandTiles(Seq<Tile> tiles, int times) {
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

    private boolean isDrillTile(Tile tile) {
        switch (tile.x % 6) {
            case 0:
            case 2:
                if ((tile.y - 1) % 6 == 0)
                    return true;
                break;
            case 1:
                if ((tile.y - 3) % 6 == 0 || (tile.y - 3) % 6 == 2)
                    return true;
                break;
            case 3:
            case 5:
                if ((tile.y - 4) % 6 == 0)
                    return true;
                break;
            case 4:
                if ((tile.y) % 6 == 0 || (tile.y) % 6 == 2)
                    return true;
                break;
        }

        return false;
    }

    private boolean isBridgeTile(Tile tile) {
        return tile.x % 3 == 0 && tile.y % 3 == 0;
    }

    public static enum Direction {
        RIGHT(1, 0, 0), UP(0, 1, 1), LEFT(-1, 0, 2), DOWN(0, -1, 3);

        public final int x, y, rotation;

        Direction(int x, int y, int rotation) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }

        public static Direction from(int i) {
            return values()[i];
        }

        public static Direction from(int x, int y) {
            if (Math.abs(x) > Math.abs(y)) {
                return x > 0 ? RIGHT : LEFT;
            } else {
                return y > 0 ? UP : DOWN;
            }
        }

        public Point2 mul(int i) {
            return new Point2(x * i, y * i);
        }
    }
}
