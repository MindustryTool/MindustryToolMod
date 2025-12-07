package mindustrytool.presentation.builder;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.ui.Styles;
import mindustrytool.core.model.SearchConfig;
import mindustrytool.core.model.TagCategory;
import mindustrytool.core.model.TagData;
import mindustrytool.presentation.component.NetworkImage;

public class TagSelector {
    private final FilterConfig config;
    private final Seq<String> modIds;

    public TagSelector(FilterConfig config, Seq<String> modIds) {
        this.config = config;
        this.modIds = modIds;
    }

    public void render(Table table, SearchConfig searchConfig, TagCategory category) {
        table.table(Styles.flatOver, t -> t.add(category.name())
                .fontScale(config.scale).left().labelAlign(Align.left))
                .top().left().padBottom(4);
        table.row();
        table.pane(card -> {
            card.defaults().size(config.cardSize, 50);
            int z = 0;
            for (TagData value : category.tags().sort((a, b) -> a.position() - b.position())) {
                if (value == null) continue;
                Seq<String> planetIds = value.planetIds();
                boolean isGeneric = planetIds == null || planetIds.size == 0;
                boolean isSelected = planetIds != null && planetIds.find(t -> modIds.contains(t)) != null;
                if (!isGeneric && !isSelected) continue;

                card.button(btn -> {
                    btn.left();
                    if (value.icon() != null && !value.icon().isEmpty())
                        btn.add(new NetworkImage(value.icon())).size(40 * config.scale).padRight(4).marginRight(4);
                    btn.add(value.name()).fontScale(config.scale);
                }, Styles.togglet, () -> searchConfig.setTag(category, value))
                        .checked(searchConfig.containTag(category, value))
                        .padRight(FilterConfig.CARD_GAP).padBottom(FilterConfig.CARD_GAP)
                        .left().expandX().margin(12);
                if (++z % config.cols == 0) card.row();
            }
        }).growY().wrap().top().left().expandX().scrollX(true).scrollY(false).padBottom(48);
    }
}
