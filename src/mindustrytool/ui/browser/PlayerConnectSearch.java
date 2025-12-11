package mindustrytool.ui.browser;

import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.ui.Styles;
import mindustrytool.ui.room.RoomFilter;

/** Builds search bar for Player Connect filtering. */
public class PlayerConnectSearch {
    public static void build(Table hosts, RoomFilter filter, Runnable refresh) {
        hosts.table(f -> {
            f.image(Icon.zoom).padLeft(10);
            f.field(filter.text, v -> { filter.text = v; refresh.run(); }).pad(3).growX().maxTextLength(50);
            f.check("@servers.remote.hidden", filter.showHidden, b -> { filter.showHidden = b; refresh.run(); })
             .update(c -> c.setChecked(filter.showHidden)).padLeft(10);
            f.button(Icon.refresh, Styles.emptyi, refresh).pad(3).size(50f).right();
        }).growX().row();
    }
}
