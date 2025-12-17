package mindustrytool.plugins.browser;

import arc.Core;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.ui.Styles;

public class ModSelector {
    private final FilterConfig config;
    private final Seq<String> modIds;

    public ModSelector(FilterConfig config, Seq<String> modIds) {
        this.config = config;
        this.modIds = modIds;
    }

    public void render(Table table, SearchConfig searchConfig, Seq<ModData> mods, Runnable onUpdate) {
        // Load saved mods if empty (first run)
        if (modIds.isEmpty()) {
            String saved = Core.settings.getString("filter.mods", "");
            if (!saved.isEmpty()) {
                for (String id : saved.split(",")) {
                    if (!id.isEmpty())
                        modIds.add(id);
                }
            }
        }

        // Fix bundle key: Use a default if "messagemod" is missing
        String messageModTitle = Core.bundle.get("messagemod", "Mods");
        table.table(Styles.flatOver, t -> t.add(messageModTitle)
                .fontScale(config.scale).left().labelAlign(Align.left))
                .top().left().expandX().padBottom(4);
        table.row();

        // Use a wrapping table directly instead of a pane to avoid nested scrolling
        // issues if placed in a scroll pane
        // But if this is inside a scroll pane, it's fine.
        // To fix flickering, we should avoid full rebuilds.
        // But here we just render. The caller controls rebuild.

        Table card = new Table();
        card.left().top(); // Align content top-left

        // Exact width calculation
        float availableWidth = arc.Core.graphics.getWidth() - 60f;
        float currentWidth = 0;

        // Current row table
        Table[] currentRow = { new Table() };
        currentRow[0].left().defaults().pad(1);
        card.add(currentRow[0]).left().top().row();

        for (ModData mod : mods.sort((a, b) -> a.position() - b.position())) {
            String name = mod.name();

            // Conservative estimation
            float iconWidth = (mod.icon() != null && !mod.icon().isEmpty()) ? 42 * config.scale : 0; // 40 size + 2 pad
            float textWidth = name.length() * 10 * config.scale;
            float estimatedWidth = textWidth + iconWidth + 20 * config.scale;

            if (currentWidth + estimatedWidth > availableWidth) {
                // New row
                currentRow[0] = new Table();
                currentRow[0].left().defaults().pad(1);
                card.add(currentRow[0]).left().top().row();
                currentWidth = 0;
            }

            currentRow[0].button(btn -> {
                btn.left();
                if (mod.icon() != null && !mod.icon().isEmpty()) {
                    Cell<Image> iconCell = btn.add(new NetworkImage(mod.icon()));
                    iconCell.size(40 * config.scale).padRight(2).marginRight(2);
                }
                btn.add(name).fontScale(config.scale);
                btn.margin(2);
            }, () -> {
                if (modIds.contains(mod.id()))
                    modIds.remove(mod.id());
                else
                    modIds.add(mod.id());

                // Save settings
                StringBuilder sb = new StringBuilder();
                for (String id : modIds)
                    sb.append(id).append(",");
                Core.settings.put("filter.mods", sb.toString());

                Core.app.post(onUpdate);
            }).checked(modIds.contains(mod.id())).get().setStyle(Styles.flatBordert);

            arc.scene.ui.Button btn = (arc.scene.ui.Button) currentRow[0].getChildren().peek();
            currentRow[0].getCell(btn).height(40 * config.scale).pad(1);

            currentWidth += estimatedWidth;
        }

        table.add(card).growX().left();
    }
}
