package mindustrytool.ui;

import arc.func.Cons;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.features.browser.SearchConfig;

public class TagBar {
    public static void draw(Table tagBar, SearchConfig searchConfig, Cons<SearchConfig> onUpdate) {
        for (var tag : searchConfig.getSelectedTags()) {
            tagBar.table(Tex.button, table -> {
                if (tag.getIcon() != null) {
                    table.add(new NetworkImage(tag.getIcon())).size(24).padRight(4);
                }
                table.add(Strings.capitalize(tag.getCategoryName() + "_" + tag.getName()));
                table.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                    searchConfig.getSelectedTags().remove(tag);
                    onUpdate.get(searchConfig);
                }).margin(4);
            });
        }
    }
}
