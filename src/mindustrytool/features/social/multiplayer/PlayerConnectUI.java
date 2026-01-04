package mindustrytool.features.social.multiplayer;

import arc.Core;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;

/** Combined UI builders for Player Connect section. */
public class PlayerConnectUI {
    public static class Filter {
        public volatile String text = "";
        public volatile boolean showHidden = false;
    }

    public static void buildHeader(Table hosts, Collapser coll, Runnable onShowHidden) {
        hosts.table(n -> {
            n.add("Player Connect").pad(10).growX().left().color(Pal.accent);
            n.button(Icon.eyeSmall, Styles.emptyi, onShowHidden).update(i -> i.getStyle().imageUp = Icon.eyeSmall)
                    .size(40f).right().padRight(3).tooltip("@servers.showhidden");
            n.button(Icon.add, Styles.emptyi, () -> new CreateRoomDialog().show()).size(40f).right().padRight(3)
                    .tooltip("Create Room");
            n.button(Icon.downOpen, Styles.emptyi, () -> {
                coll.toggle(false);
                Core.settings.put("collapsed-playerconnect", coll.isCollapsed());
            })
                    .update(i -> i.getStyle().imageUp = !coll.isCollapsed() ? Icon.upOpen : Icon.downOpen).size(40f)
                    .right().padRight(10f);
        }).growX().row();
        hosts.image().growX().pad(5).padLeft(10).padRight(10).height(3).color(Pal.accent).row();
    }

    public static void buildSearch(Table hosts, Filter filter, Runnable refresh) {
        hosts.table(f -> {
            f.image(Icon.zoom).padLeft(10);
            f.field(filter.text, v -> {
                filter.text = v;
                refresh.run();
            }).pad(3).growX().maxTextLength(50);
            f.check("@servers.remote.hidden", filter.showHidden, b -> {
                filter.showHidden = b;
                refresh.run();
            }).update(c -> c.setChecked(filter.showHidden)).padLeft(10);
            f.button(Icon.refresh, Styles.emptyi, refresh).pad(3).size(50f).right();
        }).growX().row();
    }
}
