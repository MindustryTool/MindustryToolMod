package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ai.types.MinerAI;
import mindustry.entities.units.AIController;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
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
        Seq<String> saved = Core.settings.getJson("mindustrytool.autoplay.task." + getId() + ".items", Seq.class, Seq::new);
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
        return "Auto Mine " + Iconc.unitMono;
    }

    @Override
    public Drawable getIcon() {
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
    public boolean shouldRun() {
        if (!Vars.player.unit().canMine()) {
            return false;
        }

        Item best = findBestItem();

        return best != null;
    }

    @Override
    public AIController getAI() {
        return ai;
    }

    @Override
    public void update() {
        if (Vars.player.unit() == null)
            return;

        Item best = findBestItem();

        if (best == null) {
            return;
        }

        ai.targetItem = best;

        // If current mine tile is valid and matches best item, keep it.
        Tile current = Vars.player.unit().mineTile;
        if (current != null && current.drop() == best) {
            return;
        }

        // Find new tile
        Tile ore = Vars.indexer.findClosestOre(Vars.player.x, Vars.player.y, best);

        if (ore != null) {
            Vars.player.unit().mineTile = ore;
        }
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
            if (Vars.player.unit() == null || !Vars.player.unit().canMine(item)) {
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

    private Item findBestItem() {
        if (Vars.player.team().core() == null) {
            return null;
        }

        Item best = null;
        int minAmount = Integer.MAX_VALUE;
        int mineTier = Vars.player.unit().type.mineTier;

        for (Item item : selectedItems) {
            if (item.hardness > mineTier) {
                continue;
            }

            int amount = Vars.player.team().core().items.get(item);
            if (amount < minAmount) {
                minAmount = amount;
                best = item;
            }
        }
        return best;
    }
}
