package mindustrytool.features.browser.map;

import java.security.InvalidParameterException;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.dto.MapDetailData;
import mindustrytool.ui.DetailStats;
import mindustrytool.ui.TagContainer;
import mindustrytool.ui.UserCard;

public class MapInfoDialog extends BaseDialog {

    public MapInfoDialog() {
        super("");

        setFillParent(true);
        addCloseListener();
    }

    public void show(MapDetailData data) {
        if (data == null) {
            throw new InvalidParameterException("Map can not be null");
        }
        cont.clear();
        cont.top().left();

        title.setText("[[" + Core.bundle.get("map") + "] " + data.name());

        boolean portrait = Core.graphics.isPortrait();

        if (portrait) {
            cont.add(new MapImage(data.id())).scaling(Scaling.fit)
                    .maxHeight(Core.graphics.getHeight() * 0.45f)
                    .growX()
                    .pad(10f)
                    .top()
                    .row();

            cont.pane(t -> buildDetails(t, data)).top().left().grow().scrollX(false).pad(10);
        } else {
            cont.table(main -> {
                main.top().left();

                var size = Math.max(Core.graphics.getHeight() * 0.5f, Core.graphics.getWidth() * 0.5f);

                main.add(new MapImage(data.id())).scaling(Scaling.fit)
                        .height(size)
                        .width(size)
                        .pad(10f)
                        .top();

                main.pane(t -> buildDetails(t, data)).grow().scrollX(false).pad(10);
            }).grow();
        }

        buttons.clearChildren();
        buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
        buttons.button("@open", Icon.link, () -> Core.app.openURI(Config.WEB_URL + "/maps/" + data.id())).pad(4);
        buttons.button("@back", Icon.left, this::hide);

        show();
    }

    private void buildDetails(Table card, MapDetailData data) {
        card.top().left().defaults().top().left();

        // Author
        card.table(t -> {
            t.left();
            t.add(Core.bundle.format("message.author")).marginRight(4).padRight(4);
            UserCard.draw(t, data.createdBy());
        }).fillX().padBottom(4).top().left().row();

        // Stats
        card.table(stats -> DetailStats.draw(stats, data.likes(), data.comments(), data.downloads()))
                .fillX().padBottom(4).top().left().row();

        // Tags
        if (data.tags() != null && data.tags().size > 0) {
            card.table(container -> TagContainer.draw(container, data.tags()))
                    .fillX().padBottom(4).top().left().row();
        }

        // Description
        card.add(data.description())
                .left()
                .wrap()
                .wrapLabel(true)
                .growX()
                .labelAlign(Align.topLeft)
                .top().left();
    }
}
