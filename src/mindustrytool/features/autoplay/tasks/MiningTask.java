package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
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
        for (Item item : Vars.content.items()) {
            if (Vars.player.unit() != null && Vars.player.unit().canMine(item)) {
                selectedItems.add(item);
            }
        }
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

        int cols = Math.max((int) (Core.graphics.getWidth() / Scl.scl() / width), 1);

        for (Item item : Vars.content.items()) {
            if (Vars.player.unit() == null || !Vars.player.unit().canMine(item)) {
                continue;
            }
            table.table(card -> {
                card.image(item.uiIcon).size(24).padRight(5).left();

                card.check(item.localizedName, selectedItems.contains(item), b -> {
                    if (b) {
                        selectedItems.add(item);
                    } else {
                        selectedItems.remove(item);
                    }
                }).pad(5).left();

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
