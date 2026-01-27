package mindustrytool.features.smartconveyor;

import arc.Core;
import arc.Events;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ArmoredConveyor;
import mindustry.world.blocks.distribution.BufferedItemBridge;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Duct;
import mindustry.world.blocks.distribution.DuctRouter;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.Junction;
import mindustry.world.blocks.distribution.OverflowDuct;
import mindustry.world.blocks.distribution.OverflowGate;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.blocks.distribution.StackConveyor;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class SmartConveyorFeature implements Feature {

    private boolean enabled = false;
    private Table currentMenu;
    private Tile selectedTile;
    private Tile lastClick;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("feature.smart-conveyor.name")
                .description("feature.smart-conveyor.description")
                .icon(Icon.distribution)
                .build();
    }

    @Override
    public void init() {
        Events.on(TapEvent.class, e -> {
            if (!enabled) {
                return;
            }

            if (e.tile == null) {
                return;
            }

            if (e.tile != lastClick && lastClick != null) {
                lastClick = e.tile;
                closeMenu();
                return;
            }

            lastClick = e.tile;

            if (currentMenu != null) {

                if (e.tile == selectedTile) {
                    closeMenu();
                    return;
                }

                closeMenu();

                if (isConveyor(e.tile.block())) {
                    showMenu(e.tile);
                }
                return;
            }

            if (isConveyor(e.tile.block())) {
                showMenu(e.tile);
            }
        });

        Events.on(StateChangeEvent.class, e -> {
            if (e.to == State.menu) {
                closeMenu();
            }
        });
    }

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
        closeMenu();
    }

    private void closeMenu() {
        if (currentMenu != null) {
            currentMenu.remove();
            currentMenu = null;
            selectedTile = null;
        }
    }

    private boolean isConveyor(Block block) {
        return block instanceof Conveyor || block instanceof StackConveyor || block instanceof Duct;
    }

    private boolean unlocked(Block block) {
        return block.unlockedNowHost() && block.placeablePlayer && block.environmentBuildable() &&
                block.supportsEnv(Vars.state.rules.env);
    }

    private void showMenu(Tile tile) {
        selectedTile = tile;
        currentMenu = new Table(Styles.black6);
        currentMenu.touchable = arc.scene.event.Touchable.enabled;

        currentMenu.update(() -> {
            if (selectedTile == null || selectedTile.block() == null || !isConveyor(selectedTile.block())) {
                closeMenu();
                return;
            }

            Vec2 pos = Core.camera.project(selectedTile.worldx(), selectedTile.worldy());

            currentMenu.setPosition(pos.x, pos.y + selectedTile.block().size * Vars.tilesize / 2f, Align.center);
        });

        Vars.content.blocks().each(block -> {
            if (isConveyor(block) && unlocked(block)) {
                addUpgradeButton(currentMenu, tile, block);
            }
        });

        Vars.ui.hudGroup.addChild(currentMenu);
        currentMenu.pack();
    }

    private void addUpgradeButton(Table table, Tile tile, Block targetBlock) {
        if (tile.block() == targetBlock)
            return;

        table.button(new TextureRegionDrawable(targetBlock.uiIcon), Styles.clearNonei, () -> {
            upgradeChain(tile, targetBlock);
            closeMenu();
        }).size(48f).pad(4);
    }

    private void upgradeChain(Tile startTile, Block targetBlock) {
        if (Vars.player.unit() == null) {
            return;
        }

        ObjectSet<Tile> visited = new ObjectSet<>();
        Seq<Tile> queue = new Seq<>();

        queue.add(startTile);
        visited.add(startTile);

        int maxUpdates = 500;
        int updates = 0;

        while (!queue.isEmpty() && updates < maxUpdates) {
            Tile current = queue.pop();

            if (isConveyor(current.block())) {
                Vars.player.unit().addBuild(new BuildPlan(current.x, current.y, current.build.rotation, targetBlock));
                updates++;
            }

            Building build = current.build;

            if (build == null) {
                continue;
            }

            Block block = current.block();

            if (block instanceof Conveyor || block instanceof StackConveyor || block instanceof ArmoredConveyor
                    || block instanceof Duct) {
                checkAndAdd(queue, visited, build.front());
                checkAndAdd(queue, visited, build.back());
            } else if (block instanceof BufferedItemBridge || block instanceof ItemBridge) {
                Object c = build.config();

                if (c instanceof Point2 conf) {
                    Tile link = Vars.world.tile(current.x + conf.x, current.y + conf.y);
                    if (link != null && link.build != null) {
                        for (int i = 0; i < 4; i++) {
                            checkAndAdd(queue, visited, link.nearby(i).build);
                        }
                    }
                }

            } else if (block instanceof Sorter || block instanceof Router || block instanceof OverflowGate ||
                    block instanceof DuctRouter || block instanceof OverflowDuct || block instanceof Junction) {
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, current.nearby(i).build);
                }
            }
        }
    }

    private boolean checkAndAdd(Seq<Tile> queue, ObjectSet<Tile> visited, Building target) {
        if (target == null) {
            return false;
        }

        Tile tile = target.tile;

        if (tile == null) {
            return false;
        }

        if (visited.contains(tile)) {
            return false;
        }

        Block block = tile.block();
        if (isConveyor(block) || block instanceof Junction || block instanceof ItemBridge ||
                block instanceof Router || block instanceof Sorter || block instanceof OverflowGate ||
                block instanceof DuctRouter || block instanceof OverflowDuct) {

            visited.add(tile);
            queue.add(tile);
            return true;
        }

        return false;
    }
}
