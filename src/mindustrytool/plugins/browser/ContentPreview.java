package mindustrytool.plugins.browser;

import arc.graphics.Color;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.Align;
import arc.util.Scaling;
import mindustry.gen.*;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class ContentPreview {
    public enum Type {
        MAP, SCHEMATIC
    }

    private final Type type;
    private final ContentData data;
    private final Runnable action;
    private final int cardWidth;

    public ContentPreview(Type type, ContentData data, Runnable action) {
        this(type, data, action, 200);
    }

    public ContentPreview(Type type, ContentData data, Runnable action, int cardWidth) {
        this.type = type;
        this.data = data;
        this.action = action;
        this.cardWidth = cardWidth;
    }

    public Button create(Table container, BaseDialog infoDialog) {
        // Calculate inner usage width (Card Width - Button Padding)
        // Styles.flati usually has padding. Let's calculate safe inner width.
        // If cardWidth is 50, inner might be 36 or so.
        float innerSize = Math.max(10f, cardWidth - 14f); // 14px safety padding

        Button[] btn = { null };
        btn[0] = container.button(p -> {
            p.top().margin(0).add(buttons(infoDialog, innerSize)).growX().fillX().height(50).row();
            p.add(image(innerSize)).size(innerSize).row();
            p.table(s -> DetailStats.draw(s, data.likes(), data.comments(), data.downloads())).margin(8);
        }, () -> {
            if (!btn[0].childrenPressed())
                action.run();
        }).pad(4).width(cardWidth).style(Styles.flati).get(); // Force strict outer width
        btn[0].getStyle().up = Tex.pane;
        return btn[0];
    }

    private Table buttons(BaseDialog dialog, float availableWidth) {
        return new Table(t -> {
            // Calculate dynamic button size to fit available width
            int btnCount = (type == Type.SCHEMATIC) ? 3 : 2;

            // availableWidth is already inner size.
            // Horizontal padding between buttons? roughly 2px * count?
            float maxBtnSize = (availableWidth - (btnCount * 2f)) / btnCount;
            float btnSize = Math.min(50f, Math.max(10f, maxBtnSize));

            t.center().defaults().size(btnSize).pad(1); // reduced pad
            if (type == Type.SCHEMATIC)
                t.button(Icon.copy, Styles.emptyi, () -> ContentHandler.copySchematic(data))
                        .with(b -> b.getImage().setScaling(Scaling.fit));

            t.button(Icon.download, Styles.emptyi, () -> {
                if (type == Type.MAP)
                    ContentHandler.downloadMap(data);
                else
                    ContentHandler.downloadSchematic(data);
            }).with(b -> b.getImage().setScaling(Scaling.fit));

            t.button(Icon.info, Styles.emptyi, () -> {
                InfoOpener.open(data, type == Type.MAP ? ContentType.MAP : ContentType.SCHEMATIC, dialog);
            }).tooltip("@info.title").with(b -> b.getImage().setScaling(Scaling.fit));
        });
    }

    private Stack image(float size) {
        ImageHandler.ImageType imgType = type == Type.MAP ? ImageHandler.ImageType.MAP
                : ImageHandler.ImageType.SCHEMATIC;

        // Use size for image dimensions
        return new Stack(new ImageHandler(data.id(), imgType), new Table(t -> t.top().table(Styles.black3, c -> {
            Label l = c.add(data.name()).style(Styles.outlineLabel).color(Color.white).top().growX()
                    .width(size).get(); // Use size
            l.setEllipsis(true);
            l.setAlignment(Align.center);
        }).growX().margin(1).pad(2).maxWidth(Scl.scl(size)).padBottom(0)));
    }
}
