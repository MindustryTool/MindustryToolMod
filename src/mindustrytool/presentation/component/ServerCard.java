package mindustrytool.presentation.component;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.scene.ui.layout.Table;
import arc.util.*;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.core.model.ServerData;
import mindustrytool.core.util.DescriptionTruncator;

public class ServerCard {
    public static void render(Table container, ServerData data) {
        container.top().left().background(null);
        boolean canConnect = data.mapName() != null && data.address() != null;
        container.button(t -> {
            t.top().left().setColor(Pal.gray);
            t.add(data.name()).left().labelAlign(Align.left);
            Draw.reset();
            t.row();
            if (data.description() != null && !data.description().isEmpty()) t.add("[gray]" + DescriptionTruncator.truncate(data.description(), 3)).left().wrap().row();
            t.add(Core.bundle.format("players", data.players())).left().labelAlign(Align.left).row();
            if (data.mapName() != null && !data.mapName().isEmpty()) {
                t.add("Map: " + data.mapName()).left().labelAlign(Align.left).row();
                if (data.address() != null && !data.address().isEmpty()) t.add("Address: " + data.address() + ":" + data.port()).left().labelAlign(Align.left).row();
            }
            if (data.gamemode() != null && !data.gamemode().isEmpty()) t.add("Gamemode: " + data.gamemode()).left().labelAlign(Align.left).row();
            if (data.mode() != null && !data.mode().isEmpty()) t.add("Mode: " + data.mode()).left().labelAlign(Align.left).row();
            if (data.mods() != null && !data.mods().isEmpty()) t.add("Mods: " + Strings.join(", ", data.mods())).left().labelAlign(Align.left).row();
        }, Styles.emptyi, () -> { if (canConnect) Vars.ui.join.connect(data.address(), data.port()); else Vars.ui.showInfoFade("Cannot connect."); }).growY().growX().left().disabled(!canConnect).bottom().pad(8);
    }
}
