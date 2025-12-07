package mindustrytool.presentation.builder;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.ui.Styles;
import mindustrytool.core.model.ModData;
import mindustrytool.core.model.SearchConfig;
import mindustrytool.presentation.component.NetworkImage;

public class ModSelector {
    private final FilterConfig config;
    private final Seq<String> modIds;

    public ModSelector(FilterConfig config, Seq<String> modIds) {
        this.config = config;
        this.modIds = modIds;
    }

    public void render(Table table, SearchConfig searchConfig, Seq<ModData> mods, Runnable onUpdate) {
        table.table(Styles.flatOver, t -> t.add(Core.bundle.format("messagemod"))
                .fontScale(config.scale).left().labelAlign(Align.left))
                .top().left().expandX().padBottom(4);
        table.row();
        table.pane(card -> {
            card.defaults().size(config.cardSize, 50);
            int i = 0;
            for (var mod : mods.sort((a, b) -> a.position() - b.position())) {
                card.button(btn -> {
                    btn.left();
                    if (mod.icon() != null && !mod.icon().isEmpty())
                        btn.add(new NetworkImage(mod.icon())).size(40 * config.scale).padRight(4).marginRight(4);
                    btn.add(mod.name()).fontScale(config.scale);
                }, Styles.togglet, () -> {
                    if (modIds.contains(mod.id())) modIds.remove(mod.id());
                    else modIds.add(mod.id());
                    Core.app.post(onUpdate);
                }).checked(modIds.contains(mod.id())).padRight(FilterConfig.CARD_GAP).padBottom(FilterConfig.CARD_GAP).left().fillX().margin(12);
                if (++i % config.cols == 0) card.row();
            }
        }).top().left().expandX().scrollY(false).padBottom(48);
    }
}
