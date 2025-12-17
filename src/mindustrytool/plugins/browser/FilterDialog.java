package mindustrytool.plugins.browser;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.ui.dialogs.BaseDialog;

public class FilterDialog extends BaseDialog {
    private final Cons<Cons<Seq<TagCategory>>> tagProvider;
    private final ModService modService = new ModService();
    private final TagService tagService;
    private final Seq<String> modIds = new Seq<>();

    public FilterDialog(TagService tagService, SearchConfig searchConfig, Cons<Cons<Seq<TagCategory>>> tagProvider) {
        super("");
        this.tagService = tagService;
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

            // Mod Selector (Fixed at top, outside scroll pane to prevent
            // flickering/scrolling issues)
            // We pass a runnable that ONLY rebuilds tags, not the whole dialog
            arc.scene.ui.layout.Table modTable = new arc.scene.ui.layout.Table();
            modService.getMod(mods -> new ModSelector(config, modIds).render(modTable, searchConfig, mods, () -> {
                rebuildTags(config, searchConfig, searchField.getText());
            }));
            cont.add(modTable).growX().padLeft(10).padRight(10).padBottom(10).top().row();

            // Tag List (Scrollable)
            cont.pane(table -> {
                // Store the table to rebuild it later
                this.tagTable = table;
                table.top().left(); // Align content to top-left
                rebuildTags(config, searchConfig, "");
            }).padLeft(10).padRight(10).scrollY(true).expand().fill().top(); // Align pane to top

            cont.row();
            buttons.clearChildren();
            buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
            addCloseButton();
            show();
        } catch (Exception e) {
            Log.err(e);
        }
    }

    private arc.scene.ui.layout.Table tagTable;

    private void rebuildTags(FilterConfig config, SearchConfig searchConfig, String query) {
        if (tagTable == null)
            return;

        // We clear here, but also need to clear inside the callback to be safe against
        // async race conditions
        tagTable.clear();
        tagTable.top().left();

        tagProvider.get(categories -> {
            // Ensure we are working on the current table and it's cleared before adding
            if (tagTable == null)
                return;
            tagTable.clear();
            tagTable.top().left();
            TagCategoryRenderer.render(tagTable, searchConfig, categories, config, modIds, query);
        });
    }
}
