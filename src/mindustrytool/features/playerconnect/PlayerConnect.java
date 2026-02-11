package mindustrytool.features.playerconnect;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;

import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.net.Client;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.struct.Seq;
import arc.net.NetSerializer;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Threads;
import arc.util.Time;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.EventType.PlayerIpBanEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.net.Net.NetProvider;
import mindustrytool.features.playerconnect.Packets.RoomCloseReason;

public class PlayerConnect {

    private static NetworkProxy room;
    private static Client pinger;
    private static ExecutorService worker = Threads.unboundedExecutor("Worker", 1);
    private static Thread roomThread, pingerThread;

    private static final Seq<Request> requestQueue = new Seq<>();
    private static boolean isShowingDialog = false;

    public static int ping;

    static {
        Events.run(EventType.HostEvent.class, () -> {
            close();
            Log.info("Close room on host event");
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

        Timer.schedule(() -> {
            if (isHosting()) {
                room.sendTCP(new Packets.PingPacket());
            }
        }, 0, 1f);

        Events.on(PlayerLeave.class, event -> {
            requestQueue.remove(req -> req.player.uuid().equals(event.player.uuid()));
            processNextRequest();
        });

        Events.on(PlayerJoin.class, event -> {
            if (PlayerConnect.isRoomClosed()) {
                return;
            }
            if (PlayerConnectConfig.isAutoAccept()) {
                return;
            }

            if (event.player == Vars.player) {
                return;
            }

            var originalTeam = event.player.team();

            event.player.team(Team.derelict);

            if (event.player.unit() != null) {
                event.player.unit().kill();
            }

            Call.infoMessage(event.player.con(), "Waiting for host to accept...");

            requestQueue.add(new Request(event.player, originalTeam));

            if (!isShowingDialog) {
                processNextRequest();
            }
        });

        Events.on(PlayerIpBanEvent.class, event -> {
            unbanProxyIp();
        });

        Events.on(RoomCreatedEvent.class, event -> {
            unbanProxyIp();
        });

        Core.app.addListener(new ApplicationListener() {
            @Override
            public void exit() {
                close();
            }
        });
    }

    private static class Request {
        final Player player;
        final Team originalTeam;

        public Request(Player player, Team originalTeam) {
            this.player = player;
            this.originalTeam = originalTeam;
        }
    }

    private static void updateStats() {
        if (room == null) {
            return;
        }

        room.updateStats();
    }

    public static boolean isRoomClosed() {
        return room == null || !room.isConnected();
    }

    public static boolean isHosting() {
        return room != null && room.isConnected();
    }

    public static String getRemoteHost() {
        return room == null ? null : room.getRemoteHost();
    }

    public static void create(String ip, int port,
            Cons<PlayerConnectLink> onSucceed,
            Cons<Throwable> onFailed,
            Cons<RoomCloseReason> onDisconnected//
    ) {
        if (room != null && room.isConnected()) {
            throw new IllegalStateException("Room is already created, please close it before.");
        }

        if (room == null || roomThread == null || !roomThread.isAlive()) {
            room = new NetworkProxy();
            roomThread = Threads.daemon("Proxy", room);
        }

        worker.submit(() -> {
            try {
                room.connect(ip, port, id -> {
                    Core.app.post(() -> Events.fire(new RoomCreatedEvent(room)));
                    onSucceed.get(new PlayerConnectLink(ip, port, id));
                }, onDisconnected);
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

    // private static Cons<Disconnect> customDisconnectLisenser;

    public static void join(PlayerConnectLink link, String password, Runnable success) {
        if (link == null) {
            throw new IllegalArgumentException("Link cannot be null.");
        }

        // if (customDisconnectLisenser == null) {

        // ObjectMap<Class<?>, Cons<Object>> listeners = Reflect.get(Vars.net,
        // "clientListeners");

        // var originalDisconnectListener = listeners.get(Disconnect.class);
        // customDisconnectLisenser = (p) -> {
        // Vars.netClient.setQuiet();
        // Time.runTask(3f, () -> {
        // Vars.ui.loadfrag.hide();
        // Vars.ui.showErrorMessage("Disconnected from server. Wrong password or room is
        // closed");
        // });
        // originalDisconnectListener.get(p);
        // };

        // Vars.net.handleClient(Disconnect.class, customDisconnectLisenser);
        // }

        Vars.ui.loadfrag.show("@connecting");

        Vars.logic.reset();
        Vars.net.reset();

        Vars.netClient.beginConnecting();

        NetProvider provider = Reflect.get(Vars.net, "provider");

        if (Vars.steam) {
            provider = Reflect.get(provider, "provider");
        }

        Client client = Reflect.get(provider, "client");

        Reflect.set(client, "serialization", new NetworkProxy.Serializer());

        var tcp = Reflect.get(Connection.class, client, "tcp");

        Reflect.set(tcp, "serialization", new NetworkProxy.Serializer());

        NetListener[] listeners = Reflect.get(Connection.class, client, "listeners");

        NetListener wrap = new NetListener() {
            @Override
            public void connected(Connection connection) {
                for (NetListener listener : listeners) {
                    listener.connected(connection);
                }
            }

            @Override
            public void disconnected(Connection connection, DcReason reason) {
                for (NetListener listener : listeners) {
                    listener.disconnected(connection, reason);
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Packets.MessagePacket messagePacket) {
                    Core.app.post(() -> Vars.ui.showErrorMessage(messagePacket.message));
                    Vars.netClient.setQuiet();
                    Vars.ui.loadfrag.hide();
                    client.close();
                    return;
                }

                for (NetListener listener : listeners) {
                    listener.received(connection, object);
                }
            }

            @Override
            public void idle(Connection connection) {
                for (NetListener listener : listeners) {
                    listener.idle(connection);
                }
            }
        };

        Reflect.set(Connection.class, client, "listeners", new NetListener[] { wrap });

        Vars.net.connect(link.host, link.port, () -> {

            if (!Vars.net.client()) {
                throw new IllegalStateException("Net client is not active.");
            }

            var packet = new Packets.RoomJoinPacket(link.roomId, password);
            Vars.net.send(packet, true);

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

    private static void unbanProxyIp() {
        if (PlayerConnect.isRoomClosed()) {
            return;
        }

        String remoteHost = PlayerConnect.getRemoteHost();
        if (remoteHost != null) {
            try {
                InetAddress address = InetAddress.getByName(remoteHost);
                Core.app.post(() -> unban(address.getHostAddress()));
            } catch (Exception e) {
                Log.err("Failed to check ban IP against remote host", e);
            }
        }
    }

    private static void unban(String ip) {
        Vars.netServer.admins.unbanPlayerIP(ip);
        Log.info("PlayerConnect: Unbanned Proxy IP @", ip);
    }

    private static synchronized void processNextRequest() {
        if (requestQueue.isEmpty()) {
            isShowingDialog = false;
            return;
        }

        isShowingDialog = true;
        Request req = requestQueue.remove(0);

        if (!req.player.con.isConnected()) {
            processNextRequest();
            return;
        }

        Vars.ui.showCustomConfirm(//
                "@playerconnect.join-request",
                "Player [accent]\"" + req.player.name + "\"[white] wants to join.", //
                "@accept", //
                "@reject", //
                () -> {
                    if (req.player.con.isConnected()) {
                        req.player.team(req.originalTeam);
                    }
                    processNextRequest();
                }, () -> {
                    if (req.player.con.isConnected()) {
                        Call.infoMessage(req.player.con, "You have been rejected to join the game by room host.");
                        req.player.con.close();
                    }
                    processNextRequest();
                });
    }
}
