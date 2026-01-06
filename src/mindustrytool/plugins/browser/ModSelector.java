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

        // Use a wrapping table directly instead of a pane to avoid nested scrolling
        // issues if placed in a scroll pane
        // But if this is inside a scroll pane, it's fine.
        // To fix flickering, we should avoid full rebuilds.
        // But here we just render. The caller controls rebuild.

        Table card = new Table();
        card.left().top(); // Align content top-left

        // Single row table for horizontal scrolling
        Table rowTable = new Table();
        rowTable.left().defaults().pad(1);

        for (ModData mod : mods.sort((a, b) -> a.position() - b.position())) {
            String name = mod.name();

            rowTable.button(btn -> {
                btn.left();
                if (mod.icon() != null && !mod.icon().isEmpty()) {
                    Cell<Image> iconCell = btn.add(new NetworkImage(mod.icon()));
                    iconCell.size(32 * config.scale).padRight(4).marginRight(4).align(Align.center);
                }
                btn.add(name).fontScale(config.scale).align(Align.center);
                btn.margin(4f).marginLeft(8f).marginRight(8f);

                // Highlight when checked
                btn.update(
                        () -> btn.setColor(btn.isChecked() ? mindustry.graphics.Pal.accent : arc.graphics.Color.white));
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

                arc.util.Log.info("Mod toggled: " + mod.id() + ". Current: " + modIds);

                Core.app.post(onUpdate);
            }).checked(modIds.contains(mod.id())).get().setStyle(Styles.flatBordert);

            arc.scene.ui.Button btn = (arc.scene.ui.Button) rowTable.getChildren().peek();
            rowTable.getCell(btn).height(40 * config.scale).pad(4);
        }

        // Wrap in ScrollPane
        arc.scene.ui.ScrollPane pane = new arc.scene.ui.ScrollPane(rowTable);
        pane.setScrollingDisabled(false, true); // Allow X, Disable Y
        pane.setFadeScrollBars(false);
        pane.setOverscroll(true, false);
        pane.setCancelTouchFocus(false); // Fix for buttons inside ScrollPane

        table.add(pane).growX().height(60 * config.scale).left();
    }
}
