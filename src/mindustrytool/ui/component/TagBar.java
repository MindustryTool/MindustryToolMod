package mindustrytool.ui.component;

import arc.func.Cons;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.core.model.SearchConfig;

public final class TagBar {
    private TagBar() {}

    public static void draw(Table bar, SearchConfig cfg, Cons<SearchConfig> onUpdate) {
        for (SearchConfig.SelectedTag tag : cfg.getSelectedTags()) {
            bar.table(Tex.button, t -> {
                if (tag.getIcon() != null) {
                    Cell<Image> iconCell = t.add(new NetworkImage(tag.getIcon()));
                    iconCell.size(24).padRight(4);
                }
                t.add(Strings.capitalize(tag.getCategoryName() + "_" + tag.getName()));
                t.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                    cfg.getSelectedTags().remove(tag);
                    onUpdate.get(cfg);
                }).margin(4);
            });
        }
    }
}
