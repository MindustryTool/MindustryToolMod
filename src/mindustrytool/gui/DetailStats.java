package mindustrytool.gui;

import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;

public class DetailStats {
    public static void draw(Table table, Long likes, Long comments, Long downloads) {
        table.center();
        table.image(Icon.upOpenSmall).padLeft(2).padRight(2);
        table.add(" " + likes.toString() + " ").marginLeft(2);
        table.image(Icon.chatSmall).padLeft(2).padRight(2);
        table.add(" " + comments.toString() + " ").marginLeft(2);
        table.image(Icon.downloadSmall).padLeft(2).padRight(2);
        table.add(" " + downloads.toString() + " ").marginLeft(2);
    }
}
