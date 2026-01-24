package mindustrytool.features.playerconnect;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.Date;

import arc.Core;
import arc.func.Cons;
import arc.net.ArcNetException;
import arc.net.Client;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.net.Server;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Ratekeeper;
import arc.util.Reflect;
import arc.util.Strings;
import arc.util.io.ByteBufferInput;
import arc.util.io.ByteBufferOutput;

import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.net.NetConnection;
import mindustry.net.Net.NetProvider;
import mindustrytool.Main;
import mindustrytool.features.playerconnect.Packets.RoomPlayer;
import mindustrytool.features.playerconnect.Packets.ConnectionCloseReason;
import mindustrytool.features.playerconnect.Packets.RoomCloseReason;

public class NetworkProxy extends Client implements NetListener {
    public static final String PROTOCOL_VERSION = "2.1";
    public static final int defaultTimeout = 10000;

    private static final Ratekeeper noopRate = new NoopRatekeeper();

    private final IntMap<VirtualConnection> connections = new IntMap<>();
    private final Seq<VirtualConnection> orderedConnections = new Seq<>(false);
    private final Server server;
    private final NetListener serverDispatcher;

    private volatile boolean isShutdown;

    private String roomId = null;
    private RoomCloseReason closeReason;
    private String remoteHost = "";

    private Cons<String> onRoomCreated;
    private Cons<RoomCloseReason> onRoomClosed;

    public NetworkProxy() {
        super(32768, 16384, new Serializer());

        addListener(this);

        NetProvider provider = Reflect.get(Vars.net, "provider");

        if (Vars.steam) {
            provider = Reflect.get(provider, "provider");
        }

        server = Reflect.get(provider, "server");
        serverDispatcher = Reflect.get(server, "dispatchListener");
    }

    public void connect(String host, int udpTcpPort, Cons<String> onRoomCreated,
            Cons<RoomCloseReason> onRoomClosed//
    ) throws IOException {
        this.remoteHost = host;
        this.onRoomCreated = onRoomCreated;
        this.onRoomClosed = onRoomClosed;

        connect(defaultTimeout, host, udpTcpPort, udpTcpPort);
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * Redefine {@link #run()} and {@link #stop()} to handle exceptions and restart
     * update loop if needed. <br>
     * And to handle connection idling.
     */
    @Override
    public void run() {
        isShutdown = false;

        while (!isShutdown) {
            try {
                update(250);
                // update idle
                for (int i = 0; i < orderedConnections.size; i++) {
                    VirtualConnection con = orderedConnections.get(i);
                    if (con.isConnected() && con.isIdle()) {
                        con.notifyIdle0();
                    }
                }
            } catch (IOException ex) {
                Log.err("IOException in NetworkProxy.run(): @", ex);
                close();
            } catch (ArcNetException ex) {
                if (roomId == null) {
                    close();
                    throw ex;
                }
            }
        }
    }

    /**
     * Redefine {@link #run()} and {@link #stop()} to handle exceptions and restart
     * update loop if needed.
     */
    @Override
    public void stop() {
        if (isShutdown) {
            return;
        }

        close();
        isShutdown = true;
        Selector selector = Reflect.get(Client.class, this, "selector");
        selector.wakeup();
    }

    public String roomId() {
        return roomId;
    }

    public void closeRoom() {
        roomId = null;

        if (isConnected()) {
            sendTCP(new Packets.RoomClosureRequestPacket());
        }

        close();
    }

    @Override
    public void connected(Connection connection) {
        Core.app.post(() -> {
            Packets.RoomStats stats = getStats();
            Packets.RoomCreationRequestPacket p = new Packets.RoomCreationRequestPacket(
                    PROTOCOL_VERSION,
                    PlayerConnectConfig.getPassword(),
                    stats);

            sendTCP(p);
        });
    }

    @Override
    public void disconnected(Connection connection, DcReason reason) {
        roomId = null;

        if (onRoomClosed != null) {
            onRoomClosed.get(closeReason);
        }

        orderedConnections.each(c -> c.closeQuietly(reason));
        connections.clear();
        orderedConnections.clear();

        Log.debug("Room closed: @ @ @", connection.getID(), reason, closeReason);
    }

    @Override
    public void received(Connection connection, Object object) {
        var isPcPacket = object instanceof Packets.Packet;

        if (!isPcPacket) {
            return;
        }

        if (object instanceof Packets.ConnectionPacketWrapPacket wrapperPacket) {
            Log.debug(wrapperPacket.object);
        } else {
            Log.debug(object);
        }

        try {
            if (object instanceof Packets.MessagePacket messagePacket) {
                Call.sendMessage("[scarlet][[Server]:[] " + messagePacket.message);
            } else if (object instanceof Packets.Message2Packet message2Packet) {
                Call.sendMessage("[scarlet][[Server]:[] "
                        + arc.Core.bundle.get("claj.message." + Strings.camelToKebab(message2Packet.message.name())));
            } else if (object instanceof Packets.PopupPacket popupPacket) {
                Vars.ui.showText("[scarlet][[Server][] ", popupPacket.message);
            } else if (object instanceof Packets.RoomClosedPacket closedPacket) {
                closeReason = closedPacket.reason;
                Vars.ui.showText("[scarlet][[Server][] ", closedPacket.reason.toString());
            } else if (object instanceof Packets.RoomLinkPacket roomLinkPacket) {
                // Ignore if the room id is received twice
                if (roomId != null) {
                    throw new ArcNetException("Room id is received twice");
                }

                roomId = roomLinkPacket.roomId;

                if (roomId == null) {
                    throw new ArcNetException("Room id can not be null");
                }

                if (onRoomCreated == null) {
                    throw new ArcNetException("onRoomCreated is null");
                }

                onRoomCreated.get(roomId);
            } else if (object instanceof Packets.ConnectionWrapperPacket wrapperPacket) {
                // Ignore packets until the room id is received
                if (roomId == null) {
                    return;
                }

                int id = wrapperPacket.connectionId;
                VirtualConnection con = connections.get(id);

                if (con == null) {
                    // Create a new connection
                    if (object instanceof Packets.ConnectionJoinPacket joinPacket) {
                        // Check if the link is the right
                        if (!roomId.equals(joinPacket.roomId)) {
                            Packets.ConnectionClosedPacket packet = new Packets.ConnectionClosedPacket(id,
                                    ConnectionCloseReason.error);
                            sendTCP(packet);

                            return;
                        }

                        addConnection(con = new VirtualConnection(this, id));
                        con.notifyConnected0();
                        // Change the packet rate and chat rate to a no-op version
                        ((NetConnection) con.getArbitraryData()).packetRate = noopRate;
                        ((NetConnection) con.getArbitraryData()).chatRate = noopRate;
                    }

                } else if (object instanceof Packets.ConnectionPacketWrapPacket packetWrapPacket) {
                    con.notifyReceived0(packetWrapPacket.object);
                } else if (object instanceof Packets.ConnectionIdlingPacket) {
                    con.setIdle();
                } else if (object instanceof Packets.ConnectionClosedPacket closedPacket) {
                    con.closeQuietly(closedPacket.reason.toDcReason());
                    Log.debug("Connection closed: @ @ @", con.id, closedPacket.reason, closeReason);
                }
            }
        } catch (Exception error) {
            Log.debug("Failed to handle: " + object);
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

    public static class Serializer extends mindustry.net.ArcNetProvider.PacketSerializer {
        @Override
        public Object read(ByteBuffer buffer) {
            if (buffer.get() == Packets.id) {
                var packet = Packets.newPacket(buffer.get());
                packet.read(new ByteBufferInput(buffer));

                if (packet instanceof Packets.ConnectionPacketWrapPacket wrap) {
                    wrap.object = super.read(buffer);
                }

                return packet;
            }

            buffer.position(buffer.position() - 1);
            var result = super.read(buffer);

            return result;
        }

        @Override
        public void write(ByteBuffer buffer, Object object) {
            if (object instanceof Packets.Packet packet) {
                buffer.put(Packets.id).put(Packets.getId(packet));
                packet.write(new ByteBufferOutput(buffer));

                if (packet instanceof Packets.ConnectionPacketWrapPacket wrap) {
                    super.write(buffer, wrap.object);
                }

                return;
            }

            super.write(buffer, object);
        }
    }

    public Packets.RoomStats getStats() {
        Packets.RoomStats stats = new Packets.RoomStats();

        stats.modVersion = Main.self.meta.version;
        stats.gamemode = Vars.state.rules.mode().name();
        stats.mapName = Vars.state.map.name();
        stats.name = PlayerConnectConfig.getRoomName();
        stats.mods = Vars.mods.getModStrings().list();

        Seq<RoomPlayer> players = new Seq<>();

        for (Player player : Groups.player) {
            players.add(new RoomPlayer(player.name, player.locale));
        }

        stats.locale = Vars.player.locale;
        stats.version = Version.combined();
        stats.players = players.list();
        stats.createdAt = new Date().getTime();

        return stats;
    }

    public void updateStats() {
        Core.app.post(() -> {
            try {
                if (!Vars.net.server() || !isConnected()) {
                    return;
                }

                var packet = new Packets.StatsPacket(getStats());

                sendTCP(packet);
            } catch (Throwable err) {
                Log.err(err);
            }
        });
    }

    public static class VirtualConnection extends Connection {
        final Seq<NetListener> listeners = new Seq<>();
        final int id;

        volatile boolean isConnected = true;
        /** The server will notify if the client is idling */
        volatile boolean isIdling = true;
        NetworkProxy proxy;

        public VirtualConnection(NetworkProxy proxy, int id) {
            this.proxy = proxy;
            this.id = id;

            addListener(proxy.serverDispatcher);

            Log.info("Client connection created with id: @", id);
        }

        @Override
        public int sendTCP(Object object) {
            if (object == null) {
                throw new IllegalArgumentException("object cannot be null.");
            }

            isIdling = false;

            var packet = new Packets.ConnectionPacketWrapPacket(id, true, object);

            return proxy.sendTCP(packet);
        }

        @Override
        public int sendUDP(Object object) {
            if (object == null) {
                throw new IllegalArgumentException("object cannot be null.");
            }

            isIdling = false;

            var packet = new Packets.ConnectionPacketWrapPacket(id, false, object);

            return proxy.sendUDP(packet);
        }

        @Override
        public void close(DcReason reason) {
            boolean wasConnected = isConnected;
            isConnected = isIdling = false;

            if (wasConnected) {
                var packet = new Packets.ConnectionClosedPacket(id, ConnectionCloseReason.fromDcReason(reason));

                proxy.sendTCP(packet);

                notifyDisconnected0(reason);
            }
        }

        /**
         * Close the connection without notify the server about that. <br>
         * Common use is when the server itself saying to close the connection.
         */
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
        public void setKeepAliveTCP(int keepAliveMillis) {
        } // never used

        @Override
        public void setTimeout(int timeoutMillis) {
        } // never used

        @Override
        public InetSocketAddress getRemoteAddressTCP() {
            return isConnected() ? proxy.getRemoteAddressTCP() : null;
        }

        @Override
        public InetSocketAddress getRemoteAddressUDP() {
            return isConnected() ? proxy.getRemoteAddressUDP() : null;
        }

        @Override
        public int getTcpWriteBufferSize() {
            return 0;
        } // never used

        @Override
        public boolean isIdle() {
            return isIdling;
        }

        @Override
        public void setIdleThreshold(float idleThreshold) {
        } // never used

        @Override
        public String toString() {
            return "Connection " + id;
        }

        /** Only used when sending world data */
        public void addListener(NetListener listener) {
            if (listener == null)
                throw new IllegalArgumentException("listener cannot be null.");
            listeners.add(listener);
        }

        /** Only used when sending world data */
        public void removeListener(NetListener listener) {
            if (listener == null)
                throw new IllegalArgumentException("listener cannot be null.");
            listeners.remove(listener);
        }

        public void notifyConnected0() {
            listeners.each(l -> l.connected(this));
        }

        public void notifyDisconnected0(DcReason reason) {
            Log.debug("Disconnected connection " + id + " with reason " + reason);
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
}
