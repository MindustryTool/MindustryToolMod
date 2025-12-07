package mindustrytool.presentation.builder;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.*;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.core.model.ServerHost;
import mindustrytool.feature.playerconnect.network.PlayerConnect;

public class ServerPingHelper {
    public static void buildPing(Table ping, ServerHost host) {
        ping.label(() -> Strings.animated(Time.time, 4, 11, ".")).pad(2).color(Pal.accent).left();
        PlayerConnect.pingHost(host.ip, host.port, ms -> {
            ping.clear();
            ping.image(Icon.ok).color(Color.green).padLeft(5).padRight(5).left();
            if (Vars.mobile) ping.row().add(ms + "ms").color(Color.lightGray).padLeft(5).padRight(5).left();
            else ping.add(ms + "ms").color(Color.lightGray).padRight(5).left();
        }, e -> { ping.clear(); ping.image(Icon.cancel).color(Color.red).padLeft(5).padRight(5).left(); });
    }
}
