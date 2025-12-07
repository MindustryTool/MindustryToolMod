package mindustrytool.feature.playerconnect.network;

import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.*;
import playerconnect.shared.Packets;

public class RoomStatsBuilder {
    public static Packets.RoomStats build() {
        Packets.RoomStats s = new Packets.RoomStats();
        try {
            s.gamemode = Vars.state.rules.mode().name();
            s.mapName = Vars.state.map.name();
            s.name = Vars.player.name();
            s.mods = Vars.mods.getModStrings();
            s.locale = Vars.player.locale;
            s.version = Version.combined();
            s.players = buildPlayers();
            s.createdAt = new java.util.Date().getTime();
        } catch (Throwable e) { Log.err(e); }
        return s;
    }

    private static Seq<Packets.RoomPlayer> buildPlayers() {
        Seq<Packets.RoomPlayer> pl = new Seq<>();
        for (Player p : Groups.player) { Packets.RoomPlayer r = new Packets.RoomPlayer(); r.locale = p.locale; r.name = p.name(); pl.add(r); }
        return pl;
    }
}
