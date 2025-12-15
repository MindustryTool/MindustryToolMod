package mindustrytool.plugins.browser;

import arc.Core;
import arc.scene.ui.Button;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.ui.Styles;

public class SortSelector {
    private final FilterConfig config;

    public SortSelector(FilterConfig config) { this.config = config; }

    public void render(Table table, SearchConfig searchConfig) {
        ButtonGroup<Button> buttonGroup = new ButtonGroup<>();
        table.table(Styles.flatOver, t -> t.add(Core.bundle.format("message.sort"))
                .fontScale(config.scale).left().labelAlign(Align.left))
                .top().left().expandX().padBottom(4);
        table.row();
        table.pane(card -> {
            card.defaults().size(config.cardSize, 50);
            int i = 0;
            for (Sort sort : Config.sorts) {
                card.button(btn -> btn.add(sort.getName()).fontScale(config.scale), Styles.togglet, () -> searchConfig.setSort(sort))
                        .group(buttonGroup).checked(sort.equals(searchConfig.getSort()))
                        .padRight(FilterConfig.CARD_GAP).padBottom(FilterConfig.CARD_GAP);
                if (++i % config.cols == 0) card.row();
            }
        }).top().left().expandX().scrollY(false).padBottom(48);
    }
}
