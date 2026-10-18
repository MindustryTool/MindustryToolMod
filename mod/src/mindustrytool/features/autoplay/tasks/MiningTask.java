package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.ui.Fonts;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.AutoplayFeature;
import solim.config.ConfigValue;
import solim.config.OrderedSeqPersister;
import solim.input.Button;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class MiningTask implements AutoplayTask {

    public static final String ID = "mining";

    private final ConfigValue<Seq<String>> selectedItems;
    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final MinerAI ai = new MinerAI();
    private final Interval scanTimer = new Interval(1);

    public MiningTask(AutoplayFeature feature) {
        this.selectedItems = feature.configGroup().value("mining.items", new Seq<>(), new OrderedSeqPersister());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return Core.bundle.get("feature.autoplay.task.mining");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return FileIcon.of("pickaxe.png", Icon.filter);
    }

    @Override
    public Readable<String> status() {
        return status;
    }

    public boolean isSelected(Item item) {
        Seq<String> seq = selectedItems.get();
        return seq == null || seq.isEmpty() || seq.contains(item.name);
    }

    public void toggleItem(Item item, boolean selected) {
        Seq<String> current = new Seq<>();
        Seq<String> stored = selectedItems.get();
        if (stored == null || stored.isEmpty()) {
            for (Item i : Vars.content.items()) {
                current.add(i.name);
            }
        } else {
            current.addAll(stored);
        }

        if (selected) {
            if (!current.contains(item.name)) {
                current.add(item.name);
            }
        } else {
            current.remove(item.name);
        }

        selectedItems.set(current);
    }

    private static @Nullable Tile safeFindClosestOre(float originX, float originY, Item item) {
        try {
            return Vars.indexer.findClosestOre(originX, originY, item);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable Tile safeFindClosestWallOre(float originX, float originY, Item item) {
        try {
            return Vars.indexer.findClosestWallOre(originX, originY, item);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable Tile findOreTile(Unit unit, Building core, Item item) {
        if (core == null) {
            return null;
        }
        float originX = core.x;
        float originY = core.y;

        Tile tile = null;
        boolean isFloorOre = false;

        if (unit.type.mineFloor && Vars.indexer.hasOre(item)) {
            tile = safeFindClosestOre(originX, originY, item);
            if (tile != null) {
                isFloorOre = true;
            }
        }

        if (tile == null && unit.type.mineWalls && Vars.indexer.hasWallOre(item)) {
            tile = safeFindClosestWallOre(originX, originY, item);
        }

        if (tile != null && isValidOreTile(tile, item)) {
            return tile;
        }

        if (tile != null && isFloorOre) {
            Tile validNearby = findNearbyUncoveredOre(tile, item, 12);
            if (validNearby != null) {
                return validNearby;
            }
        }
        return null;
    }

    public static boolean isValidOreTile(@Nullable Tile tile, Item item) {
        return tile != null && ((tile.drop() == item && (tile.block() == Blocks.air || tile.block() == null))
                || tile.wallDrop() == item
                || (tile.block() != null && tile.block().itemDrop == item));
    }

    private static @Nullable Tile findNearbyUncoveredOre(Tile center, Item item, int radius) {
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r)
                        continue;
                    Tile neighbor = Vars.world.tile(center.x + dx, center.y + dy);
                    if (isValidOreTile(neighbor, item)) {
                        return neighbor;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean update(Unit unit) {
        if (!unit.canMine()) {
            status.set(Core.bundle.get("feature.autoplay.status.cannot-mine"));
            return false;
        }

        Building core = unit.closestCore();
        if (core == null) {
            status.set(Core.bundle.get("feature.autoplay.status.no-core"));
            return false;
        }

        boolean currentTargetValid = ai.targetItem != null
                && ai.ore != null
                && isSelected(ai.targetItem)
                && unit.canMine(ai.targetItem)
                && core.acceptStack(ai.targetItem, 1, unit) > 0
                && isValidOreTile(ai.ore, ai.targetItem);

        Item bestItem = null;
        Tile bestTile = null;
        boolean allFull = false;

        if (unit.stack.amount > 0 && unit.stack.item != null) {
            Item held = unit.stack.item;

            boolean canContinueMining = isSelected(held)
                    && unit.canMine(held)
                    && core.acceptStack(held, 1, unit) > 0;

            Tile heldOre = canContinueMining
                    ? ((ai.targetItem == held && ai.ore != null && isValidOreTile(ai.ore, held))
                            ? ai.ore
                            : findOreTile(unit, core, held))
                    : null;

            if (canContinueMining && heldOre != null) {
                bestItem = held;
                bestTile = heldOre;
            } else {

                ai.mining = false;
                unit.mineTile = null;
                ai.targetItem = held;
                ai.ore = null;

                String uni = Fonts.getUnicodeStr(held.name);
                if ((uni == null || uni.isEmpty()) && Iconc.codes.containsKey(held.name)) {
                    uni = Character.toString((char) Iconc.codes.get(held.name));
                }
                status.set(Core.bundle.format("feature.autoplay.status.mining",
                        uni != null && !uni.isEmpty() ? uni : held.localizedName));
                return true;
            }
        } else if (currentTargetValid && !scanTimer.get(0, 30f)) {
            bestItem = ai.targetItem;
            bestTile = ai.ore;
        } else {
            int minAmount = Integer.MAX_VALUE;
            allFull = true;

            for (Item item : Vars.content.items()) {
                if (!isSelected(item) || !unit.canMine(item)) {
                    continue;
                }

                if (core.acceptStack(item, 1, unit) <= 0) {
                    continue;
                }
                allFull = false;

                Tile tile = findOreTile(unit, core, item);
                if (tile == null) {
                    continue;
                }

                int currentAmount = core.items.get(item);
                if (currentAmount < minAmount) {
                    minAmount = currentAmount;
                    bestItem = item;
                    bestTile = tile;
                }
            }

            if (currentTargetValid) {
                int currentTargetAmount = core.items.get(ai.targetItem);
                int threshold = Math.max(unit.type.itemCapacity * 2, 60);
                if (minAmount >= currentTargetAmount - threshold) {
                    bestItem = ai.targetItem;
                    bestTile = ai.ore;
                }
            }
        }

        if (allFull) {
            unit.mineTile = null;
            status.set(Core.bundle.get("feature.autoplay.status.core-full"));
            return false;
        }

        if (bestItem == null) {
            unit.mineTile = null;
            status.set(Core.bundle.get("feature.autoplay.status.no-ores"));
            return false;
        }

        if (ai.mining) {
            boolean canMineOre = unit.stack.amount == 0 || unit.stack.item == bestItem;
            unit.mineTile = canMineOre ? bestTile : null;
        } else {
            unit.mineTile = null;
        }
        ai.targetItem = bestItem;
        ai.ore = bestTile;
        String uni = Fonts.getUnicodeStr(bestItem.name);

        if ((uni == null || uni.isEmpty()) && Iconc.codes.containsKey(bestItem.name)) {
            uni = Character.toString((char) Iconc.codes.get(bestItem.name));
        }

        status.set(Core.bundle.format("feature.autoplay.status.mining",
                uni != null && !uni.isEmpty() ? uni : bestItem.localizedName));
        return true;
    }

    @Override
    public BaseAutoplayAI getAI() {
        return ai;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    public static boolean hasOreOnMap(Item item) {
        if (!Vars.state.isGame() || Vars.indexer == null) {
            return true;
        }
        return Vars.indexer.hasOre(item) || Vars.indexer.hasWallOre(item);
    }

    public static @Nullable String getUnitMiningDisabledReason(@Nullable Unit unit, Item item) {
        if (!Vars.state.isGame() || Vars.indexer == null || unit == null) {
            return null;
        }
        if (!unit.canMine()) {
            return Core.bundle.get("feature.autoplay.settings.mining.unit-cannot-mine");
        }
        if (unit.type.mineTier < item.hardness) {
            return Core.bundle.format("feature.autoplay.settings.mining.tier-too-low", item.hardness,
                    unit.type.mineTier);
        }
        boolean hasFloor = Vars.indexer.hasOre(item);
        boolean hasWall = Vars.indexer.hasWallOre(item);
        if (hasWall && !hasFloor && !unit.type.mineWalls) {
            return Core.bundle.get("feature.autoplay.settings.mining.no-wall-mining");
        }
        if (hasFloor && !hasWall && !unit.type.mineFloor) {
            return Core.bundle.get("feature.autoplay.settings.mining.no-floor-mining");
        }
        return null;
    }

    @Override
    public void buildSettings(AutoplayFeature feature) {
        column().props(growX(), gap(unit(1))).children(() -> {
            text(Core.bundle.get("feature.autoplay.settings.mining.filter"))
                    .props(color(WebStyles.Colors.GHOST_FG), growX(), left());

            wrap().props(growX(), gap(unit(1))).children(() -> {
                Unit unit = Vars.player != null ? Vars.player.unit() : null;

                for (Item item : Vars.content.items()) {
                    if (!item.unlockedNow()) {
                        continue;
                    }

                    if (!hasOreOnMap(item)) {
                        continue;
                    }

                    String disabledReason = getUnitMiningDisabledReason(unit, item);
                    boolean canMine = disabledReason == null;

                    Readable<Boolean> checked = selectedItems.signal()
                            .map(seq -> seq == null || seq.isEmpty() || seq.contains(item.name));

                    Button chipButton = button(() -> {
                        if (canMine) {
                            toggleItem(item, !Boolean.TRUE.equals(checked.peek()));
                        }
                    })
                            .props(checked(checked), style(WebStyles.filterChipText()), enabled(Readable.of(canMine)), paddingX(unit(2)), height(unit(8)), prefHeight(unit(8)))
                            .children(() -> {
                                icon(new TextureRegionDrawable(item.uiIcon)).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                text(item.localizedName)
                                        .props(color(checked.map(c -> Boolean.TRUE.equals(c)
                                                ? (canMine ? Color.white : Color.lightGray)
                                                : Color.gray)));
                            });

                    if (!canMine) {
                        chipButton.props(tooltip(disabledReason));
                    }
                }
            });
        });
    }

    public static class MinerAI extends BaseAutoplayAI {
        public static final float TRANSFER_COOLDOWN = 25f;
        public static final float TRANSFER_TIMEOUT = 60f;

        public boolean mining = true;
        public @Nullable Item targetItem;
        public @Nullable Tile ore;

        private float transferCooldownTimer = 0f;
        private float transferPendingTimer = 0f;
        private boolean transferPending = false;

        public void tryTransferInventory(Building core) {
            if (unit == null || core == null || unit.stack.amount <= 0 || unit.stack.item == null) {
                return;
            }
            if (core.acceptStack(unit.stack.item, unit.stack.amount, unit) <= 0) {
                return;
            }
            if (transferPending || transferCooldownTimer > 0f) {
                return;
            }
            if (!unit.within(core, unit.type.range)) {
                return;
            }

            Call.transferInventory(Vars.player, core);
            transferPending = true;
            transferPendingTimer = TRANSFER_TIMEOUT;
            transferCooldownTimer = TRANSFER_COOLDOWN;
        }

        @Override
        public void updateMovement() {
            if (unit == null) {
                return;
            }
            Building core = unit.closestCore();
            if (!unit.canMine() || core == null) {
                return;
            }

            if (transferCooldownTimer > 0f) {
                transferCooldownTimer -= Time.delta;
            }

            if (transferPending) {
                if (unit.stack.amount == 0) {
                    transferPending = false;
                    transferPendingTimer = 0f;
                } else {
                    transferPendingTimer -= Time.delta;
                    if (transferPendingTimer <= 0f) {
                        transferPending = false;
                    }
                }
            }

            if (!unit.validMine(unit.mineTile)) {
                unit.mineTile = null;
            }

            if (mining) {
                if (unit.stack.amount > 0 && targetItem != null && !unit.acceptsItem(targetItem)) {
                    mining = false;
                    unit.mineTile = null;
                    return;
                }

                if (targetItem != null && core.acceptStack(targetItem, 1, unit) <= 0) {
                    if (unit.stack.amount > 0) {
                        mining = false;
                        unit.mineTile = null;
                    } else {
                        unit.clearItem();
                        unit.mineTile = null;
                    }
                    return;
                }

                if (unit.stack.amount >= unit.type.itemCapacity) {
                    mining = false;
                    unit.mineTile = null;
                } else {
                    if (timer.get(timerTarget3, 60f) && targetItem != null) {
                        ore = findOreTile(unit, core, targetItem);
                    }

                    if (ore != null) {
                        boolean canMineOre = targetItem != null
                                && (unit.stack.amount == 0 || unit.stack.item == targetItem);
                        moveTo(ore, unit.type.mineRange / 2f, 20f);
                        if (canMineOre && unit.within(ore, unit.type.mineRange) && unit.validMine(ore)) {
                            unit.mineTile = ore;
                        } else if (!canMineOre) {
                            unit.mineTile = null;
                        }
                    }
                }
            } else {
                unit.mineTile = null;

                if (unit.stack.amount == 0) {
                    mining = true;
                    return;
                }

                if (unit.within(core, unit.type.range)) {
                    tryTransferInventory(core);
                    if (unit.stack.amount == 0) {
                        mining = true;
                    }
                }

                circle(core, unit.type.range / 1.8f);
            }

            if (!unit.type.flying) {
                Floor floor = unit.floorOn();
                boolean hazard = floor != null && (floor.isDuct || floor.damageTaken > 0f || floor.isDeep());
                unit.updateBoosting(unit.type.boostWhenMining || hazard);
            }
        }
    }
}
