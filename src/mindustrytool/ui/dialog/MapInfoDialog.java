package mindustrytool.ui.dialog;

import java.security.InvalidParameterException;
import arc.Core;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.core.config.Config;
import mindustrytool.core.model.MapDetailData;
import mindustrytool.ui.component.*;
import mindustrytool.ui.image.ImageHandler;
import arc.scene.ui.*;
import mindustry.gen.Tex;

public class MapInfoDialog extends BaseDialog {
    private Label descLabel;
    private ScrollPane descScrollPane;

    public MapInfoDialog() {
        super("");
        descLabel = new Label(""); descLabel.setWrap(true);
        descScrollPane = new ScrollPane(descLabel); descScrollPane.setFadeScrollBars(true);
        setFillParent(true); addCloseListener();
    }

    public void show(MapDetailData data) {
        if (data == null) throw new InvalidParameterException("Map can not be null");
        cont.clear();
        title.setText("[[" + Core.bundle.get("map") + "] " + data.name());
        cont.add(new ImageHandler(data.id(), ImageHandler.ImageType.MAP)).row();
        cont.table(card -> {
            card.center();
            card.add(Core.bundle.format("message.author")).marginRight(4).padRight(4);
            UserCard.draw(card, data.createdBy());
        }).fillX().left().minWidth(Core.graphics.getHeight() * 2 / 3);
        cont.row();
        cont.table(stats -> DetailStats.draw(stats, data.likes(), data.comments(), data.downloads())).fillX().center();
        cont.row();
        cont.table(container -> TagContainer.draw(container, data.tags())).fillX().left().row();
        cont.row();
        descLabel.setText(data.description());
        cont.table(Tex.pane, descContainer -> {
            descContainer.add(descScrollPane).grow().pad(4);
        }).height(Core.graphics.getHeight() / 4f).fillX().left().row();
        buttons.clearChildren();
        buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
        buttons.button("@back", Icon.left, this::hide).pad(4);
        buttons.button("@open", Icon.link, () -> Core.app.openURI(Config.WEB_URL + "/maps/" + data.id()));
        show();
    }
}
