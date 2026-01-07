package mindustrytool.ui;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.gen.Tex;
import mindustrytool.dto.TagData;

public class TagContainer {
    public static void draw(Table container, Seq<TagData> tags) {
        container.clearChildren();
        container.left();

        if (tags == null) {
            return;
        }

        container.add("@schematic.tags").padRight(4);

        container.pane(scrollPane -> {
            scrollPane.left();
            scrollPane.defaults().pad(4).height(42);
            int i = 0;
            for (var tag : tags) {
                scrollPane.table(Tex.button, item -> item.add(tag.name())//
                        .height(42)//
                        .fillX()//
                        .growX()//
                        .labelAlign(Align.center)//
                ).fillX();

                if (++i % 4 == 0) {
                    scrollPane.row();
                }
            }

        })
                .fillX()//
                .margin(20)//
                .left()//
                .scrollX(true);
    }
}
