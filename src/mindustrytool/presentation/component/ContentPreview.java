package mindustrytool.presentation.component;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.Align;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustrytool.core.model.ContentData;
import mindustrytool.domain.handler.MapHandler;
import mindustrytool.domain.handler.SchematicHandler;
import mindustrytool.presentation.builder.ImageHandler;
import mindustrytool.presentation.dialog.MapInfoDialog;
import mindustrytool.presentation.dialog.SchematicInfoDialog;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.data.api.Api;

public class ContentPreview {
    public enum Type { MAP, SCHEMATIC }
    private final Type type;
    private final ContentData data;
    private final Runnable action;

    public ContentPreview(Type type, ContentData data, Runnable action) { this.type = type; this.data = data; this.action = action; }

    public Button create(Table container, BaseDialog infoDialog) {
        Button[] btn = {null};
        btn[0] = container.button(p -> {
            p.top().margin(0).add(buttons(infoDialog)).growX().fillX().height(50).row();
            p.add(image()).size(200).row();
            p.table(s -> DetailStats.draw(s, data.likes(), data.comments(), data.downloads())).margin(8);
        }, () -> { if (!btn[0].childrenPressed()) action.run(); }).pad(4).style(Styles.flati).get();
        btn[0].getStyle().up = Tex.pane;
        return btn[0];
    }

    private Table buttons(BaseDialog dialog) {
        return new Table(t -> {
            t.center().defaults().size(50).pad(2);
            if (type == Type.SCHEMATIC) t.button(Icon.copy, Styles.emptyi, () -> SchematicHandler.Copy(data));
            t.button(Icon.download, Styles.emptyi, () -> { if (type == Type.MAP) MapHandler.Download(data); else SchematicHandler.Download(data); });
            t.button(Icon.info, Styles.emptyi, () -> { if (type == Type.MAP) Api.findMapById(data.id(), d -> ((MapInfoDialog)dialog).show(d)); else Api.findSchematicById(data.id(), d -> ((SchematicInfoDialog)dialog).show(d)); }).tooltip("@info.title");
        });
    }

    private Stack image() {
        ImageHandler.ImageType imgType = type == Type.MAP ? ImageHandler.ImageType.MAP : ImageHandler.ImageType.SCHEMATIC;
        return new Stack(new ImageHandler(data.id(), imgType), new Table(t -> t.top().table(Styles.black3, c -> { Label l = c.add(data.name()).style(Styles.outlineLabel).color(Color.white).top().growX().width(184).get(); l.setEllipsis(true); l.setAlignment(Align.center); Draw.reset(); }).growX().margin(1).pad(4).maxWidth(Scl.scl(184)).padBottom(0)));
    }
}