package mindustrytool.presentation.component;

import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;

public final class DetailStats {
    private DetailStats() {}

    public static void draw(Table t, long likes, long comments, long downloads) {
        t.center();
        t.image(Icon.upOpenSmall).padLeft(2).padRight(2);
        t.add(" " + likes + " ").marginLeft(2);
        t.image(Icon.chatSmall).padLeft(2).padRight(2);
        t.add(" " + comments + " ").marginLeft(2);
        t.image(Icon.downloadSmall).padLeft(2).padRight(2);
        t.add(" " + downloads + " ").marginLeft(2);
    }
}
