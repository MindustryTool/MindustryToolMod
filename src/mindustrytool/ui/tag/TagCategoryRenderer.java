package mindustrytool.ui.tag;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustrytool.core.model.SearchConfig;
import mindustrytool.core.model.TagCategory;
import mindustrytool.ui.common.FilterConfig;

public class TagCategoryRenderer {
    public static void render(Table table, SearchConfig searchConfig, Seq<TagCategory> categories, FilterConfig config, Seq<String> modIds) {
        if (categories == null) return;
        TagSelector tagSelector = new TagSelector(config, modIds);
        for (TagCategory category : categories.sort((a, b) -> a.position() - b.position())) {
            if (category.tags() == null || category.tags().isEmpty()) continue;
            table.row();
            tagSelector.render(table, searchConfig, category);
        }
    }
}
