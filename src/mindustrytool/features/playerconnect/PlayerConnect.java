package mindustrytool.features.playerconnect;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

import arc.Events;
import arc.func.Cons;
import arc.net.Client;
import arc.net.NetSerializer;
import arc.util.Threads;
import arc.util.Time;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustrytool.features.playerconnect.Packets.RoomClosedPacket.CloseReason;

public class PlayerConnect {
    static {
        Vars.ui.paused.hidden(() -> {
            arc.util.Timer.schedule(() -> {
                if (!Vars.net.active()) {
                    close();
                }
            }, 1f);
        });

        Events.run(EventType.HostEvent.class, () -> {
            close();
        });

        Events.run(EventType.DisposeEvent.class, () -> {
            dispose();
            disposePinger();
        });

        Events.run(PlayerJoin.class, () -> {
            updateStats();
        });

        Events.run(PlayerLeave.class, () -> {
            updateStats();
        });

        Events.run(WorldLoadEndEvent.class, () -> {
            updateStats();
        });

        Timer.schedule(() -> {
            updateStats();
        }, 60f, 60f);
    }

    private static NetworkProxy room;
    private static Client pinger;
    private static ExecutorService worker = Threads.unboundedExecutor("Worker", 1);
    private static Thread roomThread, pingerThread;

    private static void updateStats() {
        if (room == null) {
            return;
        }

        room.updateStats();
    }

    public static boolean isRoomClosed() {
        return room == null || !room.isConnected();
    }

    public static void create(String ip, int port,
            String password,
            Cons<PlayerConnectLink> onSucceed,
            Cons<Throwable> onFailed,
            Cons<CloseReason> onDisconnected//
    ) {
        if (room.isConnected()) {
            throw new IllegalStateException("Room is already created, please close it before.");
        }

        if (room == null || roomThread == null || !roomThread.isAlive()) {
            room = new NetworkProxy(password);
            roomThread = Threads.daemon("Proxy", room);
        }

        worker.submit(() -> {
            try {
                room.connect(ip, port, id -> onSucceed.get(new PlayerConnectLink(ip, port, id)), onDisconnected);
            } catch (Throwable e) {
                onFailed.get(e);
            }
        });
    }

    public static void close() {
        if (room != null) {
            room.closeRoom();
        }
    }

    /** Delete properly the room */
    public static void dispose() {
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

    public static void join(PlayerConnectLink link, String password, Runnable success) {
        if (link == null) {
            throw new IllegalArgumentException("Link cannot be null.");
        }

        Vars.ui.loadfrag.show("@connecting");

        Vars.logic.reset();
        Vars.net.reset();

        Vars.netClient.beginConnecting();
        Vars.net.connect(link.host, link.port, () -> {
            ByteBuffer tmpBuffer = ByteBuffer.allocate(256);
            NetSerializer tmpSerializer = new NetworkProxy.Serializer();

            if (!Vars.net.client()) {
                throw new IllegalStateException("Net client is not active.");
            }

            // We need to serialize the packet manually
            tmpBuffer.clear();

            var packet = new Packets.RoomJoinPacket(link.roomId, password);

            tmpSerializer.write(tmpBuffer, packet);
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
        NetSerializer tmpSerializer = new NetworkProxy.Serializer();

        if (pinger == null || pingerThread == null || !pingerThread.isAlive()) {
            pinger = new Client(8192, 8192, tmpSerializer);
            pingerThread = Threads.daemon("Pinger", pinger);
        }

        worker.submit(() -> {
            synchronized (pingerThread) {
                long time = Time.millis();
                try {
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
