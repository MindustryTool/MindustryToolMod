package mindustrytool.plugins.browser;

import arc.Core;
import arc.func.Cons;
import arc.scene.ui.ScrollPane;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.ui.dialogs.BaseDialog;

public class FilterDialog extends BaseDialog {
    private final Cons<Cons<Seq<TagCategory>>> tagProvider;
    private final ModService modService = new ModService();

    private final Seq<String> modIds = new Seq<>();

    private arc.scene.ui.layout.Table tagTable;
    private ScrollPane tagPane;
    private final arc.struct.ObjectMap<String, Boolean> collapseState = new arc.struct.ObjectMap<>();

    public FilterDialog(TagService tagService, SearchConfig searchConfig, Cons<Cons<Seq<TagCategory>>> tagProvider) {
        super("");

        this.tagProvider = tagProvider;
        setFillParent(true);
        addCloseListener();
        onResize(() -> {
            if (searchConfig != null)
                show(searchConfig);
        });

        // Register listeners once
        modService.onUpdate(() -> {
            if (searchConfig != null)
                show(searchConfig);
        });
        tagService.onUpdate(() -> {
            if (searchConfig != null)
                show(searchConfig);
        });
    }

    public void show(SearchConfig searchConfig) {
        try {
            FilterConfig config = new FilterConfig();
            cont.clear();
            cont.top(); // Top align everything

            // Header Table (Search + Sort)
            arc.scene.ui.layout.Table header = new arc.scene.ui.layout.Table();

            // Search bar
            arc.scene.ui.TextField searchField = new arc.scene.ui.TextField();
            searchField.setMessageText(Core.bundle.get("settings.search", "Search..."));
            searchField.changed(() -> {
                rebuildTags(config, searchConfig, searchField.getText());
            });
            header.add(searchField).growX().height(40).padRight(10);

            // Sort Selector (Dropdown)
            new SortSelector(config).render(header, searchConfig);

            cont.add(header).growX().pad(10).top().row();

            // Mod Selector
            arc.scene.ui.layout.Table modTable = new arc.scene.ui.layout.Table();
            modService.getMod(mods -> new ModSelector(config, modIds).render(modTable, searchConfig, mods, () -> {
                rebuildTags(config, searchConfig, searchField.getText());
            }));
            cont.add(modTable).growX().padLeft(10).padRight(10).padBottom(10).top().row();

            // Tag List (Scrollable)
            this.tagPane = cont.pane(table -> {
                // Store the table to rebuild it later
                this.tagTable = table;
                table.top().left(); // Align content to top-left
                rebuildTags(config, searchConfig, "");
            }).padLeft(10).padRight(10).scrollY(true).expand().fill().top().get(); // Capture ScrollPane

            cont.row();
            buttons.clearChildren();
            buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
            addCloseButton();
            show();
        } catch (Exception e) {
            Log.err(e);
        }
    }

    @Override
    public void hide() {
        // Save settings on close
        StringBuilder sb = new StringBuilder();
        for (String id : modIds)
            sb.append(id).append(",");
        Core.settings.put("filter.mods", sb.toString());

        for (String id : collapseState.keys()) {
            Core.settings.put("filter.collapse." + id, collapseState.get(id));
        }

        super.hide();
    }

    private void rebuildTags(FilterConfig config, SearchConfig searchConfig, String query) {
        if (tagTable == null)
            return;

        tagProvider.get(categories -> {
            // Build NEW table off-screen
            arc.scene.ui.layout.Table newTagTable = new arc.scene.ui.layout.Table();
            newTagTable.top().left();

            // Populate the new table
            TagCategoryRenderer.render(newTagTable, searchConfig, categories, config, modIds, query, collapseState);

            // Swap atomically
            if (tagPane != null) {
                // Capture scroll position
                float scrollY = tagPane.getScrollY();

                // Swap widget
                tagPane.setWidget(newTagTable);
                this.tagTable = newTagTable; // Update reference

                // Force layout and restore scroll immediately (synchronously)
                tagPane.validate();
                tagPane.setScrollYForce(scrollY);
                tagPane.updateVisualScroll();
            }
        });
    }
}
