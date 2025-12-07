package mindustrytool.presentation.component;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.gen.Tex;
import mindustrytool.core.model.TagData;

public final class TagContainer {
    private TagContainer() {}

    public static void draw(Table c, Seq<TagData> tags) {
        c.clearChildren();
        c.left();
        if (tags == null) return;
        c.add("@schematic.tags").padRight(4);
        c.pane(p -> {
            p.left().defaults().pad(4).height(42);
            int i = 0;
            for (var tag : tags) {
                p.table(Tex.button, t -> t.add(tag.name()).height(42).fillX().growX().labelAlign(Align.center)).fillX();
                if (++i % 4 == 0) p.row();
            }
        }).fillX().margin(20).left().scrollX(true);
    }
}
