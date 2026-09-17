package mindustrytool.features.autoplay.tasks;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.AutoplayFeature;
import solim.config.ConfigValue;
import solim.config.OrderedSeqPersister;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class MiningTask implements AutoplayTask {

    public static final String ID = "mining";

    private final ConfigValue<Seq<String>> selectedItems;
    private final Signal<String> status = Signal.of(Core.bundle.get("feature.autoplay.status.idle"));
    private final MinerAI ai = new MinerAI();

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
        return Icon.filter;
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

        Item bestItem = null;
        Tile bestTile = null;
        int minAmount = Integer.MAX_VALUE;
        boolean allFull = true;

        for (Item item : Vars.content.items()) {
            if (!isSelected(item) || !unit.canMine(item)) {
                continue;
            }

            if (core.acceptStack(item, 1, unit) <= 0) {
                continue;
            }
            allFull = false;

            if ((unit.type.mineFloor && Vars.indexer.hasOre(item))
                    || (unit.type.mineWalls && Vars.indexer.hasWallOre(item))) {

                Tile tile = Vars.indexer.findClosestOre(unit.x, unit.y, item);
                if (tile == null) {
                    tile = Vars.indexer.findClosestWallOre(unit.x, unit.y, item);
                }

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

        unit.mineTile = bestTile;
        ai.targetItem = bestItem;
        ai.ore = bestTile;
        status.set(Core.bundle.format("feature.autoplay.status.mining", bestItem.localizedName));
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

    @Override
    public void buildSettings(AutoplayFeature feature) {
        column().growX().gap(unit(1)).children(() -> {
            text(Core.bundle.get("feature.autoplay.settings.mining.filter")).growX().left().color(WebStyles.Colors.GHOST_FG);

            wrap().growX().gap(unit(1)).children(() -> {
                for (Item item : Vars.content.items()) {
                    if (!item.unlockedNow()) {
                        continue;
                    }

                    Readable<Boolean> checked = selectedItems.signal().map(seq ->
                            seq == null || seq.isEmpty() || seq.contains(item.name));

                    button(() -> toggleItem(item, !Boolean.TRUE.equals(checked.peek())))
                            .style(WebStyles.filterChipText())
                            .checked(checked)
                            .paddingX(unit(2))
                            .height(unit(8))
                            .children(() -> {
                                icon(new TextureRegionDrawable(item.uiIcon)).size(unit(4));
                                text(item.localizedName).color(checked.map(c -> Boolean.TRUE.equals(c) ? Color.white : Color.gray));
                            });
                }
            });
        });
    }

    public static class MinerAI extends BaseAutoplayAI {
        public boolean mining = true;
        public @Nullable Item targetItem;
        public @Nullable Tile ore;

        @Override
        public void updateMovement() {
            if (unit == null) {
                return;
            }
            Building core = unit.closestCore();
            if (!unit.canMine() || core == null) {
                return;
            }

            if (!unit.validMine(unit.mineTile)) {
                unit.mineTile = null;
            }

            if (mining) {
                if (targetItem != null && core.acceptStack(targetItem, 1, unit) <= 0) {
                    unit.clearItem();
                    unit.mineTile = null;
                    return;
                }

                if (unit.stack.amount >= unit.type.itemCapacity) {
                    mining = false;
                } else if (timer.get(timerTarget4, 60f) && targetItem != null && !unit.acceptsItem(targetItem)) {
                    mining = false;
                } else {
                    if (timer.get(timerTarget3, 60f) && targetItem != null) {
                        ore = null;
                        if (unit.type.mineFloor) {
                            ore = Vars.indexer.findClosestOre(core.x, core.y, targetItem);
                        }
                        if (ore == null && unit.type.mineWalls) {
                            ore = Vars.indexer.findClosestWallOre(core.x, core.y, targetItem);
                        }
                    }

                    if (ore != null) {
                        moveTo(ore, unit.type.mineRange / 2f, 20f);
                        if (unit.within(ore, unit.type.mineRange) && unit.validMine(ore)) {
                            unit.mineTile = ore;
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
                    Call.transferInventory(Vars.player, core);
                    mining = true;
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
