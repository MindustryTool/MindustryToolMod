package mindustrytool.features.playerconnect;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

import arc.Core;
import arc.func.Cons;
import arc.net.ArcNetException;
import arc.net.Client;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.FrameworkMessage;
import arc.net.NetListener;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Ratekeeper;
import arc.util.Reflect;
import arc.util.io.ByteBufferInput;
import arc.util.io.ByteBufferOutput;

import mindustry.Vars;
import mindustry.gen.Call;
import mindustrytool.ui.NoopRatekeeper;

public class NetworkProxy extends Client implements NetListener {
    public static String PROTOCOL_VERSION = "1";
    public static int defaultTimeout = 10000; // ms

    private final IntMap<VirtualConnection> connections = new IntMap<>();
    private final Seq<VirtualConnection> orderedConnections = new Seq<>(false);
    private final arc.net.Server server;
    private final NetListener serverDispatcher;

    private String password = "";
    private String roomId = null;
    private volatile boolean isShutdown;
    private Packets.RoomClosedPacket.CloseReason closeReason;

    private Cons<String> onRoomCreated;
    private Cons<Packets.RoomClosedPacket.CloseReason> onRoomClosed;

    private static final Ratekeeper noopRate = new NoopRatekeeper();

    public NetworkProxy(String password) {
        super(32768, 16384, new Serializer());
        addListener(this);

        mindustry.net.Net.NetProvider provider = Reflect.get(Vars.net, "provider");
        if (Vars.steam)
            provider = Reflect.get(provider, "provider");

        server = Reflect.get(provider, "server");
        // connections = Reflect.get(provider, "connections");
        serverDispatcher = Reflect.get(server, "dispatchListener");
        this.password = password;
    }

    /** This method must be used instead of others connect methods */
    public void connect(String host, int udpTcpPort,
            Cons<String> onRoomCreated,
            Cons<Packets.RoomClosedPacket.CloseReason> onRoomClosed//
    ) throws IOException {
        this.onRoomCreated = onRoomCreated;
        this.onRoomClosed = onRoomClosed;

        connect(defaultTimeout, host, udpTcpPort, udpTcpPort);
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
                    if (con.isConnected() && con.isIdle())
                        con.notifyIdle0();
                }
            } catch (IOException ex) {
                Log.err("IOException in NetworkProxy.run(): @", ex);
                close();
            } catch (ArcNetException ex) {
                if (roomId == null) {
                    close();
                    // Reflect.set(Connection.class, this, "lastProtocolError", ex);
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
        if (isShutdown)
            return;

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
        Log.debug("Connected: " + connection.getID());

        Core.app.post(() -> {
            Packets.RoomCreationRequestPacket p = new Packets.RoomCreationRequestPacket();
            Packets.RoomStats stats = PlayerConnect.getRoomStats();
            p.version = PROTOCOL_VERSION;
            p.password = password;
            p.data = stats;

            sendTCP(p);
        });
    }

    @Override
    public void disconnected(Connection connection, DcReason reason) {
        roomId = null;

        if (onRoomClosed != null) {
            onRoomClosed.get(closeReason);
        }
        // We cannot communicate with the server anymore, so close all virtual
        // connections
        orderedConnections.each(c -> c.closeQuietly(reason));
        connections.clear();
        orderedConnections.clear();

        Log.debug("Room closed: @ @ @", connection.getID(), reason, closeReason);

        Thread.dumpStack();
    }

    @Override
    public void received(Connection connection, Object object) {

        if (!(object instanceof FrameworkMessage)) {
            Log.debug("Received: " + object);
        }

        try {
            if (!(object instanceof Packets.Packet)) {
                return;

            } else if (object instanceof Packets.MessagePacket) {
                Call.sendMessage("[scarlet][[Server]:[] " + ((Packets.MessagePacket) object).message);

            } else if (object instanceof Packets.Message2Packet) {
                Call.sendMessage("[scarlet][[Server]:[] " + arc.Core.bundle.get("claj.message." +
                        arc.util.Strings.camelToKebab(((Packets.Message2Packet) object).message.name())));

            } else if (object instanceof Packets.PopupPacket) {
                Vars.ui.showText("[scarlet][[Server][] ", ((Packets.PopupPacket) object).message);

            } else if (object instanceof Packets.RoomClosedPacket) {
                closeReason = ((Packets.RoomClosedPacket) object).reason;

            } else if (object instanceof Packets.RoomLinkPacket) {
                // Ignore if the room id is received twice
                if (roomId != null)
                    return;

                roomId = ((Packets.RoomLinkPacket) object).roomId;
                // -1 is not allowed since it's used to specify an uncreated room
                if (roomId != null && onRoomCreated != null)
                    onRoomCreated.get(roomId);

            } else if (object instanceof Packets.ConnectionWrapperPacket) {
                // Ignore packets until the room id is received
                if (roomId == null)
                    return;

                int id = ((Packets.ConnectionWrapperPacket) object).connectionId;
                VirtualConnection con = connections.get(id);

                if (con == null) {
                    // Create a new connection
                    if (object instanceof Packets.ConnectionJoinPacket) {
                        // Check if the link is the right
                        if (!((Packets.ConnectionJoinPacket) object).roomId.equals(roomId)) {

                            Packets.ConnectionClosedPacket p = new Packets.ConnectionClosedPacket();
                            p.connectionId = id;
                            p.reason = DcReason.error;
                            sendTCP(p);
                            return;
                        }

                        addConnection(con = new VirtualConnection(this, id));
                        con.notifyConnected0();
                        // Change the packet rate and chat rate to a no-op version
                        ((mindustry.net.NetConnection) con.getArbitraryData()).packetRate = noopRate;
                        ((mindustry.net.NetConnection) con.getArbitraryData()).chatRate = noopRate;
                    }

                } else if (object instanceof Packets.ConnectionPacketWrapPacket) {
                    con.notifyReceived0(((Packets.ConnectionPacketWrapPacket) object).object);

                } else if (object instanceof Packets.ConnectionIdlingPacket) {
                    con.setIdle();

                } else if (object instanceof Packets.ConnectionClosedPacket) {
                    con.closeQuietly(((Packets.ConnectionClosedPacket) object).reason);
                    Log.debug("Close connection from server");
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
                Packets.Packet p = Packets.newPacket(buffer.get());
                p.read(new ByteBufferInput(buffer));
                if (p instanceof Packets.ConnectionPacketWrapPacket) // This one is special
                    ((Packets.ConnectionPacketWrapPacket) p).object = super.read(buffer);

                return p;
            }

            buffer.position(buffer.position() - 1);
            var result = super.read(buffer);

            Log.debug("Read packet " + result);

            return result;
        }

        @Override
        public void write(ByteBuffer buffer, Object object) {
            if (object instanceof Packets.Packet) {
                Packets.Packet p = (Packets.Packet) object;
                buffer.put(Packets.id).put(Packets.getId(p));
                p.write(new ByteBufferOutput(buffer));
                if (p instanceof Packets.ConnectionPacketWrapPacket) // This one is special
                    super.write(buffer, ((Packets.ConnectionPacketWrapPacket) p).object);

                Log.debug("Write packet " + p);

                return;
            }

            if (!(object instanceof FrameworkMessage)) {
                Log.debug("Write " + object);
            }

            super.write(buffer, object);
        }
    }

    /**
     * We can safely remove and hook things, the networking has been reverse
     * engineered.
     */
    public static class VirtualConnection extends Connection {
        final Seq<NetListener> listeners = new Seq<>();
        final int id;
        /**
         * A virtual connection is always connected until we closing it,
         * so the proxy will notify the server to close the connection in turn,
         * or when the server notifies that the connection has been closed.
         */
        volatile boolean isConnected = true;
        /** The server will notify if the client is idling */
        volatile boolean isIdling = true;
        NetworkProxy proxy;

        public VirtualConnection(NetworkProxy proxy, int id) {
            this.proxy = proxy;
            this.id = id;
            addListener(proxy.serverDispatcher);
        }

        @Override
        public int sendTCP(Object object) {
            if (object == null)
                throw new IllegalArgumentException("object cannot be null.");
            isIdling = false;

            Log.debug("TCP: Send " + object + " to " + id);

            Packets.ConnectionPacketWrapPacket p = new Packets.ConnectionPacketWrapPacket();
            p.connectionId = id;
            p.isTCP = true;
            p.object = object;
            return proxy.sendTCP(p);
        }

        @Override
        public int sendUDP(Object object) {
            if (object == null)
                throw new IllegalArgumentException("object cannot be null.");
            isIdling = false;

            Log.debug("UDP: Send " + object + " to " + id);

            Packets.ConnectionPacketWrapPacket p = new Packets.ConnectionPacketWrapPacket();
            p.connectionId = id;
            p.isTCP = false;
            p.object = object;
            return proxy.sendUDP(p);
        }

        @Override
        public void close(DcReason reason) {
            boolean wasConnected = isConnected;
            isConnected = isIdling = false;
            if (wasConnected) {
                Packets.ConnectionClosedPacket p = new Packets.ConnectionClosedPacket();
                p.connectionId = id;
                p.reason = reason;
                proxy.sendTCP(p);

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
            if (wasConnected)
                notifyDisconnected0(reason);
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
