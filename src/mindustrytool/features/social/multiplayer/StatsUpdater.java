package mindustrytool.features.social.multiplayer;

import arc.*;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import playerconnect.shared.Packets;

/** Handles stats updates and building room stats. */
public class StatsUpdater {
    private static boolean initialized = false;

    public static void init() {
        if (initialized)
            return;
        initialized = true;

        Events.run(HostEvent.class, RoomManager::close);
        Events.run(ClientPreConnectEvent.class, RoomManager::close);
        Events.run(DisposeEvent.class, () -> {
            RoomManager.dispose();
            PingManager.dispose();
        });
        Events.run(PlayerJoin.class, StatsUpdater::update);
        Events.run(PlayerLeave.class, StatsUpdater::update);
        Events.run(WorldLoadEndEvent.class, StatsUpdater::update);
        Vars.ui.paused.hidden(() -> Timer.schedule(() -> {
            if (!Vars.net.active() || Vars.state.isMenu())
                RoomManager.close();
        }, 1f));
        Timer.schedule(StatsUpdater::update, 60f, 60f);
    }

    private static void update() {
        if (!Vars.net.server())
            return;
        Core.app.post(() -> {
            try {
                NetworkProxy room = RoomManager.getRoom();
                if (room == null || !room.isConnected())
                    return;
                Packets.StatsPacket p = new Packets.StatsPacket();
                p.roomId = room.roomId();
                p.data = buildStats();
                room.sendTCP(p);
            } catch (Throwable e) {
                Log.err(e);
            }
        });
    }

    public static Packets.RoomStats getStats() {
        return buildStats();
    }

    public static String overrideName = null;

    public static Packets.RoomStats buildStats() {
        Packets.RoomStats s = new Packets.RoomStats();
        try {
            s.gamemode = Vars.state.rules.mode().name();
            s.mapName = Vars.state.map.name();
            s.name = overrideName != null ? overrideName : Vars.player.name();
            s.mods = Vars.mods.getModStrings();
            s.locale = Vars.player.locale;
            s.version = Version.combined();
            s.players = buildPlayers();
            s.createdAt = new java.util.Date().getTime();
        } catch (Throwable e) {
            Log.err(e);
        }
        return s;
    }

    private static Seq<Packets.RoomPlayer> buildPlayers() {
        Seq<Packets.RoomPlayer> pl = new Seq<>();
        for (Player p : Groups.player) {
            Packets.RoomPlayer r = new Packets.RoomPlayer();
            r.locale = p.locale;
            r.name = p.name();
            pl.add(r);
        }
        return pl;
    }
}
