package mindustrytool.presentation.component;

import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;

public final class DetailStats {
    private DetailStats() {}

    public static void draw(Table t, Long likes, Long comments, Long downloads) {
        t.center();
        t.image(Icon.upOpenSmall).padLeft(2).padRight(2);
        t.add(" " + safe(likes) + " ").marginLeft(2);
        t.image(Icon.chatSmall).padLeft(2).padRight(2);
        t.add(" " + safe(comments) + " ").marginLeft(2);
        t.image(Icon.downloadSmall).padLeft(2).padRight(2);
        t.add(" " + safe(downloads) + " ").marginLeft(2);
    }

    private static long safe(Long val) { return val != null ? val : 0L; }
}
