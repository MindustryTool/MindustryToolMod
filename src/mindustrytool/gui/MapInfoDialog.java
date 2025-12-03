package mindustrytool.gui;

import java.security.InvalidParameterException;

import arc.Core;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.data.ContentDetailData;

public class MapInfoDialog extends BaseDialog {

    public MapInfoDialog() {
        super("");

        setFillParent(true);
        addCloseListener();
    }

    public void show(ContentDetailData data) {
        if (data == null) {
            throw new InvalidParameterException("Map can not be null");
        }
        cont.clear();

        title.setText("[[" + Core.bundle.get("map") + "] " + data.name());
        cont.add(new ImageHandler(data.id(), ImageHandler.ImageType.MAP)).maxWidth(Core.graphics.getWidth() * 2 / 3).row();
        cont.table(card -> {
            card.left();
            card.add(Core.bundle.format("message.author")).marginRight(4).padRight(4);
            UserCard.draw(card, data.createdBy());
        }).fillX().left();
        cont.row();
        cont.table(stats -> DetailStats.draw(stats, data.likes(), data.comments(), data.downloads()))//
                .fillX()//
                .left();
        cont.row();
        cont.table(container -> TagContainer.draw(container, data.tags()))//
                .fillX()//
                .left()//
                .row();

        cont.row();
        cont.add(data.description())//
                .left()//
                .wrap()//
                .wrapLabel(true)//
                .fillX();

        buttons.clearChildren();
        buttons.defaults().size(Core.graphics.isPortrait() ? 150f : 210f, 64f);
        buttons.button("@back", Icon.left, this::hide);
        // buttons.button("@editor.export", Icon.upload, () -> showExport(schem));
        // buttons.button("@edit", Icon.edit, () -> showEdit(schem));

        show();
    }
}
