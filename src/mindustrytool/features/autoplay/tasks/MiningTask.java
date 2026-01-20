package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.world.Tile;

public class MiningTask implements AutoplayTask {
    private boolean enabled = true;
    private final MinerAI ai = new MinerAI();
    private final ObjectSet<Item> selectedItems = new ObjectSet<>();

    public MiningTask() {
        // Items will be loaded in load()
    }

    @Override
    public void init() {
        AutoplayTask.super.init();

        @SuppressWarnings("unchecked")
        Seq<String> saved = Core.settings.getJson("mindustrytool.autoplay.task." + getId() + ".items", Seq.class,
                Seq::new);

        selectedItems.clear();

        if (saved != null && saved.size > 0) {
            for (String name : saved) {
                Item item = Vars.content.items().find(i -> i.name.equals(name));
                if (item != null) {
                    selectedItems.add(item);
                }
            }
        } else {
            // Default to all items
            selectedItems.addAll(Vars.content.items());
        }
    }

    @Override
    public void save() {
        AutoplayTask.super.save();

        Seq<String> names = new Seq<>();

        selectedItems.each(i -> names.add(i.name));

        Core.settings.putJson("mindustrytool.autoplay.task." + getId() + ".items", names.toArray(String.class));
    }

    @Override
    public String getName() {
        return Iconc.filter + " Auto Mine";
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.filter;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean shouldRun(Unit unit) {
        if (!unit.canMine()) {
            return false;
        }

        if (Vars.player.team().core() == null) {
            return false;
        }

        Item best = null;
        int minAmount = Integer.MAX_VALUE;
        int mineTier = Vars.player.unit().type.mineTier;
        Tile tile = null;

        for (Item item : selectedItems) {
            if (item.hardness > mineTier) {
                continue;
            }

            if ((unit.type.mineFloor && Vars.indexer.hasOre(item))
                    || (unit.type.mineWalls && Vars.indexer.hasWallOre(item))) {

                tile = Vars.indexer.findClosestOre(unit.x, unit.y, item);

                if (tile == null) {
                    tile = Vars.indexer.findClosestWallOre(unit.x, unit.y, item);
                }

                if (tile == null) {
                    throw new RuntimeException("No ore or wall ore found, howwwwww");
                }

                int amount = Vars.player.team().core().items.get(item);

                if (amount < minAmount) {
                    minAmount = amount;
                    best = item;
                }
            }
        }

        if (best != null) {
            unit.mineTile = tile;
            ai.targetItem = best;
        }

        return best != null;
    }

    @Override
    public AIController getAI() {
        return ai;
    }

    @Override
    public void update(Unit unit) {
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void buildSettings(Table table) {
        table.top().left();

        int i = 0;
        int width = 300;

        int cols = Math.max((int) (Core.graphics.getWidth() / Scl.scl() * 0.9 / width), 1);

        for (Item item : Vars.content.items()) {
            var unit = Vars.player.unit();

            if (unit == null || !unit.canMine(item) || !item.unlockedNow()) {
                continue;
            }

            if ((!unit.type.mineFloor || !Vars.indexer.hasOre(item))
                    && (!unit.type.mineWalls || !Vars.indexer.hasWallOre(item))) {
                continue;
            }

            table.table(card -> {
                card.check("", selectedItems.contains(item), b -> {
                    if (b) {
                        selectedItems.add(item);
                    } else {
                        selectedItems.remove(item);
                    }
                    save();
                }).pad(5).left();

                card.image(item.uiIcon).size(24).padRight(5).left();
                card.add(item.localizedName).left();

                card.table().growX();
            }).width(width).left();

            if (++i % cols == 0) {
                table.row();
            }
        }
    }

    public class MinerAI extends AIController {
        public boolean mining = true;
        public Item targetItem;
        public Tile ore;

        @Override
        public void updateMovement() {
            Building core = unit.closestCore();

            if (!unit.canMine() || core == null) {
                return;
            }

            if (!unit.validMine(unit.mineTile)) {
                unit.mineTile(null);
            }

            if (mining) {
                if (targetItem != null && core.acceptStack(targetItem, 1, unit) == 0) {
                    unit.clearItem();
                    unit.mineTile = null;
                    return;
                }

                // if inventory is full, drop it off.
                if (unit.stack.amount >= unit.type.itemCapacity
                        || (targetItem != null && !unit.acceptsItem(targetItem))) {
                    mining = false;
                } else {
                    if (timer.get(timerTarget3, 60) && targetItem != null) {
                        ore = null;
                        if (unit.type.mineFloor)
                            ore = Vars.indexer.findClosestOre(core.x, core.y, targetItem);
                        if (ore == null && unit.type.mineWalls)
                            ore = Vars.indexer.findClosestWallOre(core.x, core.y, targetItem);
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
                    if (core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0) {
                        Vars.control.input.droppingItem = true;
                        Vars.control.input.tryDropItems(core, core.x, core.y);
                    }
                    mining = true;
                }

                circle(core, unit.type.range / 1.8f);
            }

            if (!unit.type.flying) {
                unit.updateBoosting(unit.type.boostWhenMining || unit.floorOn().isDuct
                        || unit.floorOn().damageTaken > 0f || unit.floorOn().isDeep());
            }
        }
    }
}
