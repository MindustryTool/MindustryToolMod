package mindustrytool.features.content.browser;

import arc.func.Cons;
import arc.scene.ui.layout.Table;
import arc.util.Strings;

import mindustry.ui.Styles;

public final class TagBar {
    private TagBar() {
    }

    public static Table rebuild(arc.scene.ui.ScrollPane pane, SearchConfig cfg, Cons<SearchConfig> onUpdate) {
        Table newTable = new Table();
        newTable.left();

        if (!cfg.getSelectedTags().isEmpty()) {
            for (SearchConfig.SelectedTag tag : cfg.getSelectedTags()) {
                newTable.button(b -> {
                    b.left();
                    if (tag.getIcon() != null) {
                        b.add(new NetworkImage(tag.getIcon())).size(20).padRight(4);
                    }
                    b.add(Strings.capitalize(tag.getName())).fontScale(0.9f);
                }, Styles.flatBordert, () -> {
                    cfg.getSelectedTags().remove(tag);
                    onUpdate.get(cfg);
                    // Recursively rebuild and update the pane's widget
                    rebuild(pane, cfg, onUpdate);
                }).height(36).pad(2).padRight(4).tooltip("Click to remove");
            }
        }

        // Preserve scroll
        float scrollX = pane.getScrollX();
        pane.setWidget(newTable);
        pane.validate();
        pane.setScrollXForce(scrollX);
        pane.updateVisualScroll();

        return newTable;
    }
}
