package mindustrytool.browser.gui.browser;

import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;

/**
 * Shared helpers to render likes/comments/downloads stat rows.
 */
public final class ItemStats {
    private ItemStats(){}

    public static void renderStatsRow(Table parent, BrowserItem data, boolean mobile){
        parent.table(statsBar -> {
            addStatButton(statsBar, Icon.ok, data.likes());
            addStatButton(statsBar, Icon.chatSmall, data.comments());
            addStatButton(statsBar, Icon.downloadSmall, data.downloads());       
        }).margin(8).row();
    }

    public static void addStatButton(Table table, arc.scene.style.Drawable icon, Long value) {
        Image img = new Image(icon);
        img.setColor(Color.white);
        table.add(img).size(Scl.scl(16f)).pad(Scl.scl(8));
        table.add(String.valueOf(value)).color(Pal.lightishGray).style(Styles.outlineLabel);
    }
}
