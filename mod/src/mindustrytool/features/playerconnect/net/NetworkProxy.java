package mindustrytool.features.playerconnect.net;

import arc.Core;
import arc.func.Cons;
import arc.net.ArcNetException;
import arc.net.Client;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.FrameworkMessage.KeepAlive;
import arc.net.NetListener;
import arc.net.Server;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Ratekeeper;
import arc.util.Reflect;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.ByteBufferInput;
import arc.util.io.ByteBufferOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.UUID;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.ArcNetProvider.PacketSerializer;
import mindustry.net.Net.NetProvider;
import mindustry.net.NetConnection;
import mindustrytool.Main;
import mindustrytool.features.playerconnect.net.Packets.ConnectionCloseReason;
import mindustrytool.features.playerconnect.net.Packets.RoomCloseReason;
import mindustrytool.features.playerconnect.net.Packets.RoomPlayer;
import mindustrytool.features.playerconnect.net.Packets.RoomStats;

public class NetworkProxy extends Client implements NetListener {
    public static final String PROTOCOL_VERSION = "159";
    public static final int DEFAULT_TIMEOUT = 10000;

    private static final String ROOM_ID_KEY = "mindustrytool.player-connect.room-id";
    private static final Ratekeeper NOOP_RATE = new NoopRatekeeper();

    private final IntMap<VirtualConnection> connections = new IntMap<>();
    private final Seq<VirtualConnection> orderedConnections = new Seq<>(false);
    private final NetListener serverDispatcher;
    private final Server server;
    private final String roomId;

    private NetProvider originalProvider;
    private volatile boolean isShutdown;
    private String remoteHost = "";

    private Cons<String> onRoomCreated;
    private Cons<RoomCloseReason> onRoomClosed;
    private Cons<Integer> onPingUpdated;

    private String roomPassword = "";
    private String roomName = "";
    private String ip = "";

    public NetworkProxy() {
        super(32768, 16384, new Serializer());
        this.roomId = getOrCreateId();

        addListener(this);

        NetProvider provider = Reflect.get(Vars.net, "provider");
        if (Vars.steam) {
            provider = Reflect.get(provider, "provider");
        }

        this.server = Reflect.get(provider, "server");
        this.serverDispatcher = Reflect.get(server, "dispatchListener");

        wrapProvider();
    }

    public String getProviderIp() {
        return ip;
    }

    private void wrapProvider() {
        NetProvider current = Reflect.get(Vars.net, "provider");
        if (originalProvider == null) {
            originalProvider = current;
        }
        Reflect.set(Vars.net, "provider", new ProxyProvider(current, orderedConnections));
    }

    public void restoreProvider() {
        if (originalProvider != null) {
            Reflect.set(Vars.net, "provider", originalProvider);
            originalProvider = null;
        }
    }

    public void connect(
            String host, int port,
            String roomName, String password,
            Cons<String> onRoomCreated,
            Cons<RoomCloseReason> onRoomClosed,
            Cons<Integer> onPingUpdated) throws IOException {
        this.remoteHost = host;
        this.roomName = roomName;
        this.roomPassword = password != null ? password : "";
        this.onRoomCreated = onRoomCreated;
        this.onRoomClosed = onRoomClosed;
        this.onPingUpdated = onPingUpdated;

        InetAddress address = InetAddress.getByName(host);
        ip = address.getHostAddress();

        connect(DEFAULT_TIMEOUT, host, port, port);
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public String roomId() {
        return roomId;
    }

    @Override
    public void run() {
        isShutdown = false;
        while (!isShutdown) {
            try {
                update(250);
                for (int i = 0; i < orderedConnections.size; i++) {
                    VirtualConnection con = orderedConnections.get(i);
                    if (con.isConnected() && con.isIdle()) {
                        con.notifyIdle0();
                    }
                }
            } catch (IOException ex) {
                Log.err("IOException in NetworkProxy.run()", ex);
                Vars.ui.showException(ex);
                closeRoom();
            } catch (ArcNetException ex) {
                Log.err("ArcNetException in NetworkProxy.run()", ex);
                closeRoom();
            }
        }
    }

    @Override
    public void stop() {
        if (isShutdown) {
            return;
        }

        closeRoom();

        isShutdown = true;
        Selector selector = Reflect.get(Client.class, this, "selector");
        if (selector != null) {
            selector.wakeup();
        }
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void close(DcReason reason) {
        Thread.dumpStack();
        super.close(reason);
    }

    public void closeRoom() {
        if (isConnected()) {
            sendTCP(new Packets.RoomClosureRequestPacket());
        }

        close();
    }

    @Override
    public void dispose() {
        restoreProvider();
        orderedConnections.each(c -> c.closeQuietly(DcReason.closed));
        connections.clear();
        try {
            super.dispose();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void connected(Connection connection) {
        Core.app.post(() -> {
            RoomStats stats = getStats();
            Packets.RoomCreationRequestPacket p = new Packets.RoomCreationRequestPacket(
                    PROTOCOL_VERSION,
                    roomPassword,
                    stats);
            sendTCP(p);
        });
    }

    @Override
    public void disconnected(Connection connection, DcReason reason) {
        if (onRoomClosed != null) {
            onRoomClosed.get(RoomCloseReason.closed);
        }

        orderedConnections.each(c -> c.closeQuietly(reason));
        connections.clear();
        orderedConnections.clear();

        restoreProvider();
        Log.debug("PlayerConnect room closed: @ @", connection.getID(), reason);
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof KeepAlive) {
            return;
        }

        if (!(object instanceof Packets.Packet)) {
            Log.info("Received non-PC packet: @", object);
            return;
        }

        try {
            if (object instanceof Packets.PingPacket) {
                Packets.PingPacket pingPacket = (Packets.PingPacket) object;
                long latency = (Time.millis() - pingPacket.sendAt) / 3;
                if (onPingUpdated != null) {
                    onPingUpdated.get((int) latency);
                }
            } else if (object instanceof Packets.MessagePacket) {
                Packets.MessagePacket messagePacket = (Packets.MessagePacket) object;
                Call.sendMessage("[scarlet][[Server]:[white] " + messagePacket.message);
            } else if (object instanceof Packets.Message2Packet) {
                Packets.Message2Packet message2Packet = (Packets.Message2Packet) object;
                Call.sendMessage("[scarlet][[Server]:[white] "
                        + Core.bundle.get("claj.message." + Strings.camelToKebab(message2Packet.message.name()),
                                message2Packet.message.name()));
            } else if (object instanceof Packets.PopupPacket) {
                Packets.PopupPacket popupPacket = (Packets.PopupPacket) object;
                Core.app.post(() -> Vars.ui.showText("[scarlet][[Server][white] ", popupPacket.message));
            } else if (object instanceof Packets.RoomClosedPacket) {
                Packets.RoomClosedPacket closedPacket = (Packets.RoomClosedPacket) object;
                if (onRoomClosed != null) {
                    onRoomClosed.get(closedPacket.reason);
                }
                Core.app.post(() -> Vars.ui.showText("[scarlet][[Server][white] ", closedPacket.reason.toString()));
            } else if (object instanceof Packets.RoomLinkPacket) {
                if (onRoomCreated != null) {
                    onRoomCreated.get(roomId);
                }
            } else if (object instanceof Packets.ConnectionWrapperPacket) {
                Packets.ConnectionWrapperPacket wrapperPacket = (Packets.ConnectionWrapperPacket) object;
                int id = wrapperPacket.connectionId;
                VirtualConnection con = connections.get(id);

                if (con == null) {
                    if (object instanceof Packets.ConnectionJoinPacket) {
                        Packets.ConnectionJoinPacket joinPacket = (Packets.ConnectionJoinPacket) object;
                        if (!roomId.equals(joinPacket.roomId)) {
                            Packets.ConnectionClosedPacket packet = new Packets.ConnectionClosedPacket(
                                    id, ConnectionCloseReason.error);
                            sendTCP(packet);
                            return;
                        }

                        con = new VirtualConnection(this, id);
                        addConnection(con);
                        con.notifyConnected0();

                        try {
                            NetConnection netCon = (NetConnection) con.getArbitraryData();
                            if (netCon != null) {
                                netCon.packetRate = NOOP_RATE;
                                netCon.chatRate = NOOP_RATE;
                            }
                        } catch (Exception error) {
                            Log.debug("Failed to set packet rate: @", error);
                        }
                    }
                } else if (object instanceof Packets.ConnectionPacketWrapPacket) {
                    Packets.ConnectionPacketWrapPacket packetWrap = (Packets.ConnectionPacketWrapPacket) object;
                    con.notifyReceived0(packetWrap.object);
                } else if (object instanceof Packets.ConnectionIdlingPacket) {
                    con.setIdle();
                } else if (object instanceof Packets.ConnectionClosedPacket) {
                    Packets.ConnectionClosedPacket closedPacket = (Packets.ConnectionClosedPacket) object;
                    con.closeQuietly(closedPacket.reason.toDcReason());
                }
            }
        } catch (Exception error) {
            Log.err("Failed to handle PlayerConnect packet: @", error);
        }
    }

    protected void addConnection(VirtualConnection con) {
        connections.put(con.id, con);
        orderedConnections.add(con);
    }

    protected void removeConnection(VirtualConnection con) {
        connections.remove(con.id);
        orderedConnections.remove(con);
    }

    public RoomStats getStats() {
        RoomStats stats = new RoomStats();
        stats.setId(roomId);
        stats.setModVersion(Main.self != null && Main.self.meta != null ? Main.self.meta.version : "1.0");
        stats.setGamemode(Vars.state != null && Vars.state.rules != null ? Vars.state.rules.mode().name() : "custom");
        stats.setMapName(Vars.state != null && Vars.state.map != null ? Vars.state.map.name() : "unknown");
        stats.setName(roomName);
        stats.setMods(Vars.mods != null ? Vars.mods.getModStrings().list() : new Seq<String>().list());

        Seq<RoomPlayer> players = new Seq<>();
        if (Groups.player != null) {
            for (Player player : Groups.player) {
                players.add(new RoomPlayer(player.name, player.locale));
            }
        }
        stats.setLocale(Vars.player != null ? Vars.player.locale : "en");
        stats.setVersion(Version.combined());
        stats.setPlayers(players.list());
        stats.setCreatedAt(System.currentTimeMillis());

        return stats;
    }

    public void updateStats(String currentRoomName) {
        this.roomName = currentRoomName;
        Core.app.post(() -> {
            try {
                if (!Vars.net.server() || !isConnected()) {
                    return;
                }
                sendTCP(new Packets.StatsPacket(getStats()));
            } catch (Exception err) {
                Log.err("Failed to update room stats: @", err);
            }
        });
    }

    public static class Serializer extends PacketSerializer {
        @Override
        public Object read(ByteBuffer buffer) {
            if (buffer.get() == Packets.id) {
                Packets.Packet packet = Packets.newPacket(buffer.get());
                packet.read(new ByteBufferInput(buffer));

                if (packet instanceof Packets.ConnectionPacketWrapPacket) {
                    ((Packets.ConnectionPacketWrapPacket) packet).object = super.read(buffer);
                }

                return packet;
            }

            buffer.position(buffer.position() - 1);
            return super.read(buffer);
        }

        @Override
        public void write(ByteBuffer buffer, Object object) {
            if (object instanceof Packets.Packet) {
                Packets.Packet packet = (Packets.Packet) object;
                buffer.put(Packets.id).put(Packets.getId(packet));
                packet.write(new ByteBufferOutput(buffer));

                if (packet instanceof Packets.ConnectionPacketWrapPacket) {
                    super.write(buffer, ((Packets.ConnectionPacketWrapPacket) packet).object);
                }
                return;
            }

            super.write(buffer, object);
        }
    }

    public static class VirtualConnection extends Connection {
        final Seq<NetListener> listeners = new Seq<>();
        final int id;
        final NetworkProxy proxy;

        volatile boolean isConnected = true;
        volatile boolean isIdling = true;

        public VirtualConnection(NetworkProxy proxy, int id) {
            this.proxy = proxy;
            this.id = id;

            if (proxy.serverDispatcher != null) {
                addListener(proxy.serverDispatcher);
            }
            Log.info("VirtualConnection created with id: @", id);
        }

        @Override
        public int sendTCP(Object object) {
            if (object == null) {
                throw new IllegalArgumentException("object cannot be null.");
            }
            isIdling = false;
            return proxy.sendTCP(new Packets.ConnectionPacketWrapPacket(id, true, object));
        }

        @Override
        public int sendTCPBuffer(ByteBuffer buffer) {
            isIdling = false;
            return proxy.sendTCP(new Packets.ConnectionPacketWrapPacket(id, true, buffer));
        }

        @Override
        public int sendUDP(Object object) {
            if (object == null) {
                throw new IllegalArgumentException("object cannot be null.");
            }
            isIdling = false;
            return proxy.sendUDP(new Packets.ConnectionPacketWrapPacket(id, false, object));
        }

        @Override
        public int sendUDPBuffer(ByteBuffer buffer) {
            isIdling = false;
            return proxy.sendUDP(new Packets.ConnectionPacketWrapPacket(id, false, buffer));
        }

        @Override
        public void close(DcReason reason) {
            boolean wasConnected = isConnected;
            isConnected = isIdling = false;

            if (wasConnected) {
                proxy.sendTCP(new Packets.ConnectionClosedPacket(id, ConnectionCloseReason.fromDcReason(reason)));
                notifyDisconnected0(reason);
            }
        }

        public void closeQuietly(DcReason reason) {
            boolean wasConnected = isConnected;
            isConnected = isIdling = false;
            if (wasConnected) {
                notifyDisconnected0(reason);
            }
        }

        @Override
        public int getID() {
            return id;
        }

        @Override
        public boolean isConnected() {
            return isConnected;
        }

        @Override
        public InetSocketAddress getRemoteAddressTCP() {
            return isConnected() ? proxy.getRemoteAddressTCP() : null;
        }

        @Override
        public InetSocketAddress getRemoteAddressUDP() {
            return isConnected() ? proxy.getRemoteAddressUDP() : null;
        }

        @Override
        public boolean isIdle() {
            return isIdling;
        }

        @Override
        public String toString() {
            return "VirtualConnection " + id;
        }

        public void addListener(NetListener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("listener cannot be null.");
            }
            listeners.add(listener);
        }

        public void removeListener(NetListener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("listener cannot be null.");
            }
            listeners.remove(listener);
        }

        public void notifyConnected0() {
            listeners.each(l -> l.connected(this));
        }

        public void notifyDisconnected0(DcReason reason) {
            proxy.removeConnection(this);
            listeners.each(l -> l.disconnected(this, reason));
        }

        public void setIdle() {
            isIdling = true;
        }

        public void notifyIdle0() {
            listeners.each(l -> isIdle(), l -> l.idle(this));
        }

        public void notifyReceived0(Object object) {
            listeners.each(l -> l.received(this, object));
        }
    }

    private static String getOrCreateId() {
        String roomId = Core.settings.getString(ROOM_ID_KEY, null);
        if (roomId != null && !roomId.trim().isEmpty()) {
            return roomId;
        }

        String generatedRoomId = UUID.randomUUID().toString();
        Core.settings.put(ROOM_ID_KEY, generatedRoomId);
        Core.settings.forceSave();
        return generatedRoomId;
    }
}
