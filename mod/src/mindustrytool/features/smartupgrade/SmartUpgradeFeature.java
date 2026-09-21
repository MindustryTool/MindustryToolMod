package mindustrytool.features.smartupgrade;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Scaling;
import arc.util.Time;
import mindustry.Vars;
import mindustry.core.GameState.State;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.EventType.TapEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.distribution.BufferedItemBridge;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.DirectionBridge.DirectionBridgeBuild;
import mindustry.world.blocks.distribution.DirectionLiquidBridge;
import mindustry.world.blocks.distribution.Duct;
import mindustry.world.blocks.distribution.DuctBridge;
import mindustry.world.blocks.distribution.DuctBridge.DuctBridgeBuild;
import mindustry.world.blocks.distribution.DuctRouter;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.ItemBridge.ItemBridgeBuild;
import mindustry.world.blocks.distribution.Junction;
import mindustry.world.blocks.distribution.OverflowDuct;
import mindustry.world.blocks.distribution.OverflowGate;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.blocks.distribution.StackConveyor;
import mindustry.world.blocks.liquid.Conduit;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.blocks.liquid.LiquidBridge.LiquidBridgeBuild;
import mindustry.world.blocks.liquid.LiquidJunction;
import mindustry.world.blocks.liquid.LiquidRouter;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

/**
 * Smart Upgrade feature: bulk upgrades connected distribution chains
 * (conveyors, ducts), conduits, bridges, walls, and drills with a single tap or keybind.
 */
public class SmartUpgradeFeature extends Feature {

    public enum BlockGroup {
        CONVEYOR, CONDUIT, ITEM_BRIDGE, LIQUID_BRIDGE, WALL, DRILL, NONE
    }

    public final ConfigGroup config;
    public final ConfigValue<Integer> maxUpdatesConfig;
    public final ConfigValue<Integer> holdDurationConfig;
    public final ConfigValue<Boolean> onlySameTypeConfig;
    public final ConfigValue<Boolean> traverseBridgesConfig;

    private int cachedMaxUpdates = 500;
    private int cachedHoldDuration = 300;
    private boolean cachedOnlySameType = false;
    private boolean cachedTraverseBridges = true;

    private @Nullable SmartUpgradeSettingsDialog settingsDialog;
    private @Nullable Table currentMenu;
    private @Nullable Tile selectedTile;

    private @Nullable Tile touchTile;
    private long touchTime;
    private boolean holdTriggered;

    public SmartUpgradeFeature() {
        super(FeatureMetadata.builder()
                .id("smart-upgrade")
                .icon(FileIcon.of("chevrons-up.png"))
                .order(12)
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        config = configGroup();
        maxUpdatesConfig = config.intValue("max-updates", 500);
        holdDurationConfig = config.intValue("hold-duration", 300);
        onlySameTypeConfig = config.boolValue("only-same-type", false);
        traverseBridgesConfig = config.boolValue("traverse-bridges", true);

        maxUpdatesConfig.signal().subscribe(v -> cachedMaxUpdates = v != null ? v : 500);
        holdDurationConfig.signal().subscribe(v -> cachedHoldDuration = v != null ? v : 300);
        onlySameTypeConfig.signal().subscribe(v -> cachedOnlySameType = v != null ? v : false);
        traverseBridgesConfig.signal().subscribe(v -> cachedTraverseBridges = v != null ? v : true);

        syncConfigCache();

        bindToggle("smartUpgradeToggle", KeyCode.unset);
        bindAction("smartUpgradeTrigger", KeyCode.u, this::triggerHoveredUpgrade);
        bindDialog("smartUpgradeSettings", KeyCode.unset, getSettingDialog(), false);

        Events.run(Trigger.update, this::update);
        Events.run(Trigger.draw, this::draw);

        Events.on(TapEvent.class, e -> {
            if (currentMenu != null && !isMenuTouched()) {
                closeMenu();
            }
        });

        Events.on(StateChangeEvent.class, e -> {
            if (e.to == State.menu) {
                closeMenu();
                resetHold();
            }
        });
    }

    private void syncConfigCache() {
        Integer max = maxUpdatesConfig.get();
        cachedMaxUpdates = max != null ? max : 500;

        Integer hold = holdDurationConfig.get();
        cachedHoldDuration = hold != null ? hold : 300;

        Boolean sameType = onlySameTypeConfig.get();
        cachedOnlySameType = sameType != null ? sameType : false;

        Boolean bridges = traverseBridgesConfig.get();
        cachedTraverseBridges = bridges != null ? bridges : true;
    }

    public void resetToDefaults() {
        maxUpdatesConfig.reset();
        holdDurationConfig.reset();
        onlySameTypeConfig.reset();
        traverseBridgesConfig.reset();
        syncConfigCache();
    }

    @Override
    public void onDisable() {
        closeMenu();
        resetHold();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new SmartUpgradeSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    @Override
    public void onQuickAccessLongClick() {
        Prov<SolimDialog> provider = getSettingDialog();
        if (provider != null) {
            SolimDialog dialog = provider.get();
            if (dialog != null) {
                dialog.show();
            }
        }
    }

    public static BlockGroup getGroup(@Nullable Block block) {
        if (block == null) {
            return BlockGroup.NONE;
        }
        if (block instanceof LiquidBridge || block instanceof DirectionLiquidBridge) {
            return BlockGroup.LIQUID_BRIDGE;
        }
        if (block instanceof ItemBridge || block instanceof DuctBridge) {
            return BlockGroup.ITEM_BRIDGE;
        }
        if (block instanceof Conveyor || block instanceof StackConveyor || block instanceof Duct) {
            return BlockGroup.CONVEYOR;
        }
        if (block instanceof Conduit) {
            return BlockGroup.CONDUIT;
        }
        if (block instanceof Wall) {
            return BlockGroup.WALL;
        }
        if (block instanceof Drill || block instanceof BeamDrill) {
            return BlockGroup.DRILL;
        }
        return BlockGroup.NONE;
    }

    public static boolean isUnlocked(@Nullable Block block) {
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

    public Seq<Block> getUpgradeCandidates(@Nullable Block currentBlock) {
        Seq<Block> candidates = new Seq<>();
        if (currentBlock == null) {
            return candidates;
        }
        BlockGroup group = getGroup(currentBlock);
        if (group == BlockGroup.NONE) {
            return candidates;
        }

        if (Vars.content == null || Vars.content.blocks() == null) {
            return candidates;
        }

        for (Block block : Vars.content.blocks()) {
            if (block != null
                    && getGroup(block) == group
                    && block != currentBlock
                    && block.size == currentBlock.size
                    && isUnlocked(block)) {
                candidates.add(block);
            }
        }
        return candidates;
    }

    public void triggerHoveredUpgrade() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame()
                || Vars.world == null || Vars.player == null) {
            return;
        }

        Tile tile = Vars.world.tileWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY());
        if (tile == null || tile.build == null || tile.team() != Vars.player.team()) {
            return;
        }

        if (getGroup(tile.block()) == BlockGroup.NONE) {
            return;
        }

        showMenu(tile);
    }

    public void showMenu(Tile tile) {
        if (tile == null || tile.build == null) {
            return;
        }
        closeMenu();

        Seq<Block> candidates = getUpgradeCandidates(tile.block());
        if (candidates.isEmpty()) {
            if (Vars.ui != null && Vars.ui.hudfrag != null && Core.bundle != null) {
                Vars.ui.hudfrag.showToast(Core.bundle.get("feature.smart-upgrade.no-candidates",
                        "No upgrade candidates available."));
            }
            return;
        }

        selectedTile = tile;
        currentMenu = new Table(Styles.black8);
        currentMenu.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null
                && Vars.ui.hudfrag.shown && Vars.state != null && Vars.state.isGame());
        currentMenu.touchable = Touchable.enabled;
        currentMenu.margin(4f);

        int col = 0;
        for (Block targetBlock : candidates) {
            currentMenu.button(b -> {
                if (targetBlock.uiIcon != null) {
                    b.image(targetBlock.uiIcon).scaling(Scaling.fit).size(32f);
                } else if (targetBlock.fullIcon != null) {
                    b.image(targetBlock.fullIcon).scaling(Scaling.fit).size(32f);
                }
            }, Styles.clearNonei, () -> {
                if (Vars.control != null && Vars.control.input != null) {
                    Vars.control.input.isBuilding = false;
                }
                Core.app.post(() -> {
                    int count = upgradeChain(tile, targetBlock);
                    if (count > 0 && Vars.ui != null && Vars.ui.hudfrag != null && Core.bundle != null) {
                        Vars.ui.hudfrag.showToast(Icon.up, Core.bundle.format("feature.smart-upgrade.upgraded",
                                count, targetBlock.localizedName));
                    }
                });
                closeMenu();
            }).size(44f).pad(2).tooltip(targetBlock.localizedName);

            col++;
            if (col % 5 == 0 && col < candidates.size) {
                currentMenu.row();
            }
        }

        currentMenu.update(() -> {
            if (selectedTile == null || selectedTile.build == null
                    || Vars.player == null || selectedTile.team() != Vars.player.team()
                    || getGroup(selectedTile.block()) == BlockGroup.NONE
                    || Core.camera == null) {
                closeMenu();
                return;
            }

            Vec2 pos = Core.camera.project(selectedTile.worldx(), selectedTile.worldy());
            float offset = selectedTile.block().size * Vars.tilesize * 1.5f;
            float x = pos.x;
            float y = pos.y + offset;

            if (Core.graphics != null && currentMenu != null) {
                float w = currentMenu.getWidth();
                float h = currentMenu.getHeight();
                x = Mathf.clamp(x, w / 2f + 8f, Core.graphics.getWidth() - w / 2f - 8f);
                y = Mathf.clamp(y, 8f, Core.graphics.getHeight() - h - 8f);
            }
            currentMenu.setPosition(x, y, Align.bottom | Align.center);
        });

        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            Vars.ui.hudGroup.addChild(currentMenu);
            currentMenu.pack();
        }
    }

    public void closeMenu() {
        if (currentMenu != null) {
            currentMenu.remove();
            currentMenu = null;
            selectedTile = null;
        }
    }

    private boolean isMenuTouched() {
        if (currentMenu == null || Core.scene == null || Core.input == null) {
            return false;
        }
        float mx = Core.input.mouseX();
        float my = Core.input.mouseY();
        Element hit = Core.scene.hit(mx, my, true);
        return hit != null && (hit == currentMenu || hit.isDescendantOf(currentMenu));
    }

    private void update() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.player == null) {
            closeMenu();
            resetHold();
            return;
        }

        if (currentMenu != null && Core.input != null) {
            if (Core.input.keyTap(KeyCode.escape) || Core.input.keyTap(KeyCode.back)) {
                closeMenu();
                return;
            }
        }

        if (Core.input != null && Core.input.isTouched()) {
            if (currentMenu != null) {
                if (Core.input.justTouched() && !isMenuTouched()) {
                    closeMenu();
                }
                return;
            }

            if (Core.scene != null && Core.scene.hasMouse()) {
                resetHold();
                return;
            }

            if (Vars.control != null && Vars.control.input != null && Vars.control.input.isBuilding) {
                resetHold();
                return;
            }

            if (Vars.world == null) {
                resetHold();
                return;
            }

            Tile tile = Vars.world.tileWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY());
            if (tile == null || tile.build == null || tile.team() != Vars.player.team()
                    || getGroup(tile.block()) == BlockGroup.NONE) {
                resetHold();
                return;
            }

            if (touchTile == null || touchTile != tile) {
                touchTile = tile;
                touchTime = Time.millis();
                holdTriggered = false;
            } else if (!holdTriggered) {
                if (Time.timeSinceMillis(touchTime) >= cachedHoldDuration) {
                    holdTriggered = true;
                    showMenu(tile);
                }
            }
        } else {
            resetHold();
        }
    }

    private void resetHold() {
        touchTile = null;
        holdTriggered = false;
    }

    private void draw() {
        if (!isEnabled() || currentMenu == null || selectedTile == null
                || selectedTile.build == null || Core.camera == null) {
            return;
        }

        float z = Draw.z();
        Draw.z(Layer.overlayUI);
        Lines.stroke(2f, Pal.accent);
        float size = selectedTile.block().size * Vars.tilesize;
        Lines.rect(selectedTile.build.x - size / 2f, selectedTile.build.y - size / 2f, size, size);
        Draw.reset();
        Draw.z(z);
    }

    public int upgradeChain(Tile startTile, Block targetBlock) {
        if (startTile == null || startTile.build == null || targetBlock == null) {
            return 0;
        }
        if (Vars.player == null || Vars.player.unit() == null) {
            if (Vars.ui != null && Vars.ui.hudfrag != null && Core.bundle != null) {
                Vars.ui.hudfrag.showToast(Icon.warning, Core.bundle.get("feature.smart-upgrade.no-builder",
                        "No builder unit available."));
            }
            return 0;
        }

        Team team = Vars.player.team();
        BlockGroup group = getGroup(startTile.block());
        if (group == BlockGroup.NONE) {
            return 0;
        }

        Block startBlock = startTile.block();
        int targetSize = targetBlock.size;

        ObjectSet<Tile> visited = new ObjectSet<>();
        Seq<Tile> queue = new Seq<>();

        queue.add(startTile);
        visited.add(startTile);

        int maxUpdates = cachedMaxUpdates;
        int updates = 0;

        while (!queue.isEmpty() && updates < maxUpdates) {
            Tile current = queue.pop();
            if (current == null || current.build == null || current.team() != team) {
                continue;
            }

            Block currentBlock = current.block();
            boolean shouldUpgrade = (cachedOnlySameType ? currentBlock == startBlock : getGroup(currentBlock) == group)
                    && currentBlock.size == targetSize
                    && currentBlock != targetBlock;

            if (shouldUpgrade) {
                BuildPlan plan = new BuildPlan(current.x, current.y, current.build.rotation,
                        targetBlock, current.build.config());
                if (plan.placeable(team)) {
                    if (Vars.player.unit().plans != null) {
                        Vars.player.unit().plans.remove(p -> p != null && p.x == plan.x && p.y == plan.y);
                    }
                    Vars.player.unit().addBuild(plan);
                    updates++;
                }
            }

            expandNeighbors(queue, visited, current.build, group, team);
        }

        return updates;
    }

    private void expandNeighbors(Seq<Tile> queue, ObjectSet<Tile> visited, Building build, BlockGroup group, Team team) {
        if (build == null) {
            return;
        }
        Block block = build.block;

        if (group == BlockGroup.CONVEYOR) {
            if (block instanceof Conveyor || block instanceof StackConveyor || block instanceof Duct) {
                checkAndAdd(queue, visited, build.front(), group, team);
                checkAndAdd(queue, visited, build.back(), group, team);
                for (int i = 0; i < 4; i++) {
                    Building near = build.nearby(i);
                    if (near != null && near.front() == build) {
                        checkAndAdd(queue, visited, near, group, team);
                    }
                }
            } else if (cachedTraverseBridges && build instanceof ItemBridgeBuild) {
                ItemBridgeBuild bridgeBuild = (ItemBridgeBuild) build;
                if (bridgeBuild.link != -1 && Vars.world != null) {
                    Building linked = Vars.world.build(bridgeBuild.link);
                    if (linked != null) {
                        checkAndAdd(queue, visited, linked, group, team);
                        for (int i = 0; i < 4; i++) {
                            checkAndAdd(queue, visited, linked.nearby(i), group, team);
                        }
                    }
                }
                if (bridgeBuild.incoming != null && Vars.world != null) {
                    for (int i = 0; i < bridgeBuild.incoming.size; i++) {
                        Building src = Vars.world.build(bridgeBuild.incoming.get(i));
                        if (src != null) {
                            checkAndAdd(queue, visited, src, group, team);
                            for (int k = 0; k < 4; k++) {
                                checkAndAdd(queue, visited, src.nearby(k), group, team);
                            }
                        }
                    }
                }
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            } else if (cachedTraverseBridges && (block instanceof ItemBridge || block instanceof BufferedItemBridge)) {
                Object conf = build.config();
                if (conf instanceof Point2) {
                    Point2 p = (Point2) conf;
                    if (Vars.world != null) {
                        Tile link = Vars.world.tile(build.tileX() + p.x, build.tileY() + p.y);
                        if (link != null && link.build != null) {
                            checkAndAdd(queue, visited, link.build, group, team);
                            for (int i = 0; i < 4; i++) {
                                checkAndAdd(queue, visited, link.build.nearby(i), group, team);
                            }
                        }
                    }
                }
            } else if (cachedTraverseBridges && build instanceof DuctBridgeBuild) {
                Building linked = ((DuctBridgeBuild) build).findLink();
                if (linked != null) {
                    checkAndAdd(queue, visited, linked, group, team);
                    for (int i = 0; i < 4; i++) {
                        checkAndAdd(queue, visited, linked.nearby(i), group, team);
                    }
                }
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            } else if (cachedTraverseBridges && isConveyorTraversable(block)) {
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            }
        } else if (group == BlockGroup.CONDUIT) {
            if (block instanceof Conduit) {
                checkAndAdd(queue, visited, build.front(), group, team);
                checkAndAdd(queue, visited, build.back(), group, team);
                for (int i = 0; i < 4; i++) {
                    Building near = build.nearby(i);
                    if (near != null && near.front() == build) {
                        checkAndAdd(queue, visited, near, group, team);
                    }
                }
            } else if (cachedTraverseBridges && build instanceof LiquidBridgeBuild) {
                LiquidBridgeBuild bridgeBuild = (LiquidBridgeBuild) build;
                if (bridgeBuild.link != -1 && Vars.world != null) {
                    Building linked = Vars.world.build(bridgeBuild.link);
                    if (linked != null) {
                        checkAndAdd(queue, visited, linked, group, team);
                        for (int i = 0; i < 4; i++) {
                            checkAndAdd(queue, visited, linked.nearby(i), group, team);
                        }
                    }
                }
                if (bridgeBuild.incoming != null && Vars.world != null) {
                    for (int i = 0; i < bridgeBuild.incoming.size; i++) {
                        Building src = Vars.world.build(bridgeBuild.incoming.get(i));
                        if (src != null) {
                            checkAndAdd(queue, visited, src, group, team);
                            for (int k = 0; k < 4; k++) {
                                checkAndAdd(queue, visited, src.nearby(k), group, team);
                            }
                        }
                    }
                }
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            } else if (cachedTraverseBridges && block instanceof LiquidBridge) {
                Object conf = build.config();
                if (conf instanceof Point2) {
                    Point2 p = (Point2) conf;
                    if (Vars.world != null) {
                        Tile link = Vars.world.tile(build.tileX() + p.x, build.tileY() + p.y);
                        if (link != null && link.build != null) {
                            checkAndAdd(queue, visited, link.build, group, team);
                            for (int i = 0; i < 4; i++) {
                                checkAndAdd(queue, visited, link.build.nearby(i), group, team);
                            }
                        }
                    }
                }
            } else if (cachedTraverseBridges && build instanceof DirectionBridgeBuild) {
                Building linked = ((DirectionBridgeBuild) build).findLink();
                if (linked != null) {
                    checkAndAdd(queue, visited, linked, group, team);
                    for (int i = 0; i < 4; i++) {
                        checkAndAdd(queue, visited, linked.nearby(i), group, team);
                    }
                }
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            } else if (cachedTraverseBridges && isConduitTraversable(block)) {
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            }
        } else if (group == BlockGroup.ITEM_BRIDGE) {
            if (build instanceof ItemBridgeBuild) {
                ItemBridgeBuild bridgeBuild = (ItemBridgeBuild) build;
                if (bridgeBuild.link != -1 && Vars.world != null) {
                    Building linked = Vars.world.build(bridgeBuild.link);
                    checkAndAdd(queue, visited, linked, group, team);
                }
                if (bridgeBuild.incoming != null && Vars.world != null) {
                    for (int i = 0; i < bridgeBuild.incoming.size; i++) {
                        Building src = Vars.world.build(bridgeBuild.incoming.get(i));
                        checkAndAdd(queue, visited, src, group, team);
                    }
                }
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            } else if (build instanceof DuctBridgeBuild) {
                Building linked = ((DuctBridgeBuild) build).findLink();
                checkAndAdd(queue, visited, linked, group, team);
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            } else {
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            }
        } else if (group == BlockGroup.LIQUID_BRIDGE) {
            if (build instanceof LiquidBridgeBuild) {
                LiquidBridgeBuild bridgeBuild = (LiquidBridgeBuild) build;
                if (bridgeBuild.link != -1 && Vars.world != null) {
                    Building linked = Vars.world.build(bridgeBuild.link);
                    checkAndAdd(queue, visited, linked, group, team);
                }
                if (bridgeBuild.incoming != null && Vars.world != null) {
                    for (int i = 0; i < bridgeBuild.incoming.size; i++) {
                        Building src = Vars.world.build(bridgeBuild.incoming.get(i));
                        checkAndAdd(queue, visited, src, group, team);
                    }
                }
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            } else if (build instanceof DirectionBridgeBuild) {
                Building linked = ((DirectionBridgeBuild) build).findLink();
                checkAndAdd(queue, visited, linked, group, team);
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            } else {
                for (int i = 0; i < 4; i++) {
                    checkAndAdd(queue, visited, build.nearby(i), group, team);
                }
            }
        } else if (group == BlockGroup.WALL || group == BlockGroup.DRILL) {
            if (build.proximity != null) {
                for (Building next : build.proximity) {
                    checkAndAdd(queue, visited, next, group, team);
                }
            }
        }
    }

    private void checkAndAdd(Seq<Tile> queue, ObjectSet<Tile> visited,
            @Nullable Building target, BlockGroup group, Team team) {
        if (target == null || target.tile == null) {
            return;
        }
        if (target.team != team) {
            return;
        }
        Tile tile = target.tile;
        if (visited.contains(tile)) {
            return;
        }

        Block block = target.block;
        if (block == null) {
            return;
        }

        if (group == BlockGroup.CONVEYOR) {
            if (getGroup(block) == BlockGroup.CONVEYOR || (cachedTraverseBridges && isConveyorTraversable(block))) {
                visited.add(tile);
                queue.add(tile);
            }
        } else if (group == BlockGroup.CONDUIT) {
            if (getGroup(block) == BlockGroup.CONDUIT || (cachedTraverseBridges && isConduitTraversable(block))) {
                visited.add(tile);
                queue.add(tile);
            }
        } else if (group == BlockGroup.ITEM_BRIDGE) {
            if (getGroup(block) == BlockGroup.ITEM_BRIDGE) {
                visited.add(tile);
                queue.add(tile);
            }
        } else if (group == BlockGroup.LIQUID_BRIDGE) {
            if (getGroup(block) == BlockGroup.LIQUID_BRIDGE) {
                visited.add(tile);
                queue.add(tile);
            }
        } else if (group == BlockGroup.WALL || group == BlockGroup.DRILL) {
            if (getGroup(block) == group) {
                visited.add(tile);
                queue.add(tile);
            }
        }
    }

    private boolean isConveyorTraversable(Block block) {
        return block instanceof Junction
                || block instanceof ItemBridge
                || block instanceof DuctBridge
                || block instanceof Router
                || block instanceof Sorter
                || block instanceof OverflowGate
                || block instanceof DuctRouter
                || block instanceof OverflowDuct;
    }

    private boolean isConduitTraversable(Block block) {
        return block instanceof LiquidJunction
                || block instanceof LiquidRouter
                || block instanceof LiquidBridge
                || block instanceof DirectionLiquidBridge;
    }
}
