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
        onResize(() -> { if (searchConfig != null) show(searchConfig); });
    }

    public void show(SearchConfig searchConfig) {
        modService.onUpdate(() -> show(searchConfig));
        tagService.onUpdate(() -> show(searchConfig));
        try {
            FilterConfig config = new FilterConfig();
            cont.clear();
            cont.pane(table -> {
                modService.getMod(mods -> new ModSelector(config, modIds).render(table, searchConfig, mods, () -> show(searchConfig)));
                table.row();
                new SortSelector(config).render(table, searchConfig);
                table.row();
                tagProvider.get(categories -> TagCategoryRenderer.render(table, searchConfig, categories, config, modIds));
            }).padLeft(20).padRight(20).scrollY(true).expand().fill().left().top();
            cont.row();
            buttons.clearChildren();
            buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
            addCloseButton();
            show();
        } catch (Exception e) { 
            Log.err(e); 
        }
    }
}
