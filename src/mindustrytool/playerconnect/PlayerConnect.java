package mindustrytool.playerconnect;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

import arc.Events;
import arc.func.Cons;
import arc.net.Client;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Threads;
import arc.util.Time;

import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import playerconnect.shared.Packets;
import playerconnect.shared.Packets.RoomPlayer;

public class PlayerConnect {
    static {
        // Pretty difficult to know when the player quits the game, there is no event...
        Vars.ui.paused.hidden(() -> {
            arc.util.Timer.schedule(() -> {
                if (!Vars.net.active() || Vars.state.isMenu())
                    closeRoom();
            }, 1f);
        });
        Events.run(EventType.HostEvent.class, PlayerConnect::closeRoom);
        Events.run(EventType.ClientPreConnectEvent.class, PlayerConnect::closeRoom);
        Events.run(EventType.DisposeEvent.class, () -> {
            disposeRoom();
            disposePinger();
        });

        Events.run(PlayerJoin.class, () -> {
            if (Vars.net.server()) {
                updateStats();
            }
        });

        Events.run(PlayerLeave.class, () -> {
            if (Vars.net.server()) {
                updateStats();
            }
        });

        Events.run(WorldLoadEndEvent.class, () -> {
            if (Vars.net.server()) {
                updateStats();
            }
        });
    }

    private static void updateStats() {
        if (room == null) {
            Log.warn("Not connected to a room yet");
            return;
        }
        Packets.StatsPacket p = new Packets.StatsPacket();
        Packets.RoomStats stats = new Packets.RoomStats();
        try {
            stats.gamemode = Vars.state.rules.mode().name();
            stats.mapName = Vars.state.map.name();
            stats.name = Vars.player.name();
            stats.mods = Vars.mods.getModStrings();

            Seq<RoomPlayer> players = new Seq<>();

            for (Player player : Groups.player) {
                RoomPlayer pl = new RoomPlayer();
                pl.locale = player.locale;
                pl.name = player.name();
                players.add(pl);
            }

            stats.players = players;

        } catch (Throwable err) {
            Log.err(err);
        }
        p.data = stats;
        room.sendTCP(p);
    }

    private static NetworkProxy room;
    private static Client pinger;
    private static ExecutorService worker = Threads.unboundedExecutor("CLaJ Worker", 1);
    private static arc.net.NetSerializer tmpSerializer;
    private static ByteBuffer tmpBuffer = ByteBuffer.allocate(256);// we need 16 bytes for the room join packet
    private static Thread roomThread, pingerThread;

    public static boolean isRoomClosed() {
        return room == null || !room.isConnected();
    }

    public static void createRoom(String ip, int port,
            Cons<PlayerConnectLink> onSucceed,
            Cons<Throwable> onFailed,
            Cons<Packets.RoomClosedPacket.CloseReason> onDisconnected//
    ) {
        if (room == null || roomThread == null || !roomThread.isAlive()) {
            roomThread = Threads.daemon("CLaJ Proxy", room = new NetworkProxy());
        }

        worker.submit(() -> {
            try {
                if (room.isConnected()) {
                    throw new IllegalStateException("Room is already created, please close it before.");
                }
                room.connect(ip, port, id -> onSucceed.get(new PlayerConnectLink(ip, port, id)), onDisconnected);
            } catch (Throwable e) {
                onFailed.get(e);
            }
        });
    }

    /** Just close the room connection, doesn't delete it */
    public static void closeRoom() {
        if (room != null)
            room.closeRoom();
    }

    /** Delete properly the room */
    public static void disposeRoom() {
        if (room != null) {
            room.stop();
            try {
                roomThread.join(1000);
            } catch (Exception ignored) {
            }
            try {
                room.dispose();
            } catch (Exception ignored) {
            }
            roomThread = null;
            room = null;
        }
    }

    public static void joinRoom(PlayerConnectLink link, Runnable success) {
        if (link == null)
            return;

        Vars.logic.reset();
        Vars.net.reset();

        Vars.netClient.beginConnecting();
        Vars.net.connect(link.host, link.port, () -> {
            if (!Vars.net.client())
                return;

            if (tmpSerializer == null)
                tmpSerializer = new NetworkProxy.Serializer();

            // We need to serialize the packet manually
            tmpBuffer.clear();
            Packets.RoomJoinPacket p = new Packets.RoomJoinPacket();
            p.password = "";
            p.roomId = link.roomId;
            tmpSerializer.write(tmpBuffer, p);
            tmpBuffer.limit(tmpBuffer.position()).position(0);
            Vars.net.send(tmpBuffer, true);

            success.run();
        });
    }

    /**
     * @apiNote async operation but blocking new tasks if a ping is already in
     *          progress
     */
    public static void pingHost(String ip, int port, Cons<Long> success, Cons<Exception> onFailed) {
        if (tmpSerializer == null)
            tmpSerializer = new NetworkProxy.Serializer();
        if (pinger == null || pingerThread == null || !pingerThread.isAlive())
            pingerThread = Threads.daemon("CLaJ Pinger", pinger = new Client(8192, 8192, tmpSerializer));

        worker.submit(() -> {
            synchronized (pingerThread) {
                long time = Time.millis();
                try {
                    // Connect successfully is enough.
                    pinger.connect(2000, ip, port);
                    time = Time.timeSinceMillis(time);
                    pinger.close();
                    success.get(time);
                } catch (Exception e) {
                    onFailed.get(e);
                }
            }
        });
    }

    public static void disposePinger() {
        if (pinger != null) {
            pinger.stop();
            try {
                pingerThread.join(1000);
            } catch (Exception ignored) {
            }
            try {
                pinger.dispose();
            } catch (Exception ignored) {
            }
            pingerThread = null;
            pinger = null;
        }
    }
}
