package mindustrytool.feature.playerconnect.network;

import arc.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType.*;
import playerconnect.shared.Packets;

public class StatsUpdater {
    static {
        Events.run(HostEvent.class, RoomManager::close);
        Events.run(ClientPreConnectEvent.class, RoomManager::close);
        Events.run(DisposeEvent.class, () -> { RoomManager.dispose(); PingManager.dispose(); });
        Events.run(PlayerJoin.class, StatsUpdater::update);
        Events.run(PlayerLeave.class, StatsUpdater::update);
        Events.run(WorldLoadEndEvent.class, StatsUpdater::update);
        Vars.ui.paused.hidden(() -> Timer.schedule(() -> { if (!Vars.net.active() || Vars.state.isMenu()) RoomManager.close(); }, 1f));
        Timer.schedule(StatsUpdater::update, 60f, 60f);
    }

    private static void update() {
        if (!Vars.net.server()) return;
        Core.app.post(() -> {
            try {
                NetworkProxy room = RoomManager.getRoom();
                if (room == null || !room.isConnected()) return;
                Packets.StatsPacket p = new Packets.StatsPacket();
                p.roomId = room.roomId();
                p.data = RoomStatsBuilder.build();
                room.sendTCP(p);
            } catch (Throwable e) { Log.err(e); }
        });
    }

    public static Packets.RoomStats getStats() { return RoomStatsBuilder.build(); }
}
