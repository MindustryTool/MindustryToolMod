package mindustrytool.plugins.browser;

import arc.scene.ui.layout.Table;
import mindustry.ui.Styles;

public class SortSelector {
    private final FilterConfig config;

    public SortSelector(FilterConfig config) {
        this.config = config;
    }

    public void render(Table table, SearchConfig searchConfig) {
        table.button(b -> {
            b.label(() -> searchConfig.getSort().getName()).fontScale(config.scale);
        }, () -> {
            int index = Config.sorts.indexOf(searchConfig.getSort());
            int nextIndex = (index + 1) % Config.sorts.size();
            searchConfig.setSort(Config.sorts.get(nextIndex));
        }).height(40 * config.scale).minWidth(150 * config.scale).get().setStyle(Styles.flatBordert);
    }
}
