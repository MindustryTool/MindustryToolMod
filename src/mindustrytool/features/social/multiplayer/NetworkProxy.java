package mindustrytool.features.social.multiplayer;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import arc.Core;
import arc.func.Cons;
import arc.net.*;
import arc.struct.*;
import arc.util.Reflect;
import mindustry.Vars;
import playerconnect.shared.Packets;

/** Unified network proxy handling room connections and virtual connections. */
public class NetworkProxy extends Client implements NetListener {
    public static int defaultTimeout = 10000;
    private final IntMap<VirtualConnection> conns = new IntMap<>();
    private final Seq<VirtualConnection> ordered = new Seq<>(false);
    private final NetListener serverDispatcher;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private volatile String password, roomId;
    private volatile Packets.RoomClosedPacket.CloseReason closeReason;
    private volatile Cons<String> onRoomCreated;
    private volatile Cons<Packets.RoomClosedPacket.CloseReason> onRoomClosed;

    public NetworkProxy(String pwd) {
        super(32768, 16384, new ProxySerializer());
        addListener(this);
        this.password = pwd;
        mindustry.net.Net.NetProvider prov = Reflect.get(Vars.net, "provider");
        if (Vars.steam) prov = Reflect.get(prov, "provider");
        arc.net.Server srv = Reflect.get(prov, "server");
        serverDispatcher = Reflect.get(srv, "dispatchListener");
    }

    // Connection lifecycle
    public void connect(String host, int port, Cons<String> onCreate, Cons<Packets.RoomClosedPacket.CloseReason> onClose) throws IOException {
        this.onRoomCreated = onCreate;
        this.onRoomClosed = onClose;
        connect(defaultTimeout, host, port, port);
    }

    @Override public void run() {
        isShutdown.set(false);
        while (!isShutdown.get()) {
            try {
                update(250);
                for (int i = 0; i < ordered.size; i++) {
                    VirtualConnection c = ordered.get(i);
                    if (c.isConnected() && c.isIdle()) c.notifyIdle0();
                }
            } catch (IOException ex) { close(); }
            catch (ArcNetException ex) { if (roomId == null) { close(); throw ex; } }
        }
    }

    @Override public void stop() {
        if (isShutdown.get()) return;
        close();
        isShutdown.set(true);
        java.nio.channels.Selector sel = Reflect.get(Client.class, this, "selector");
        sel.wakeup();
    }

    public void closeRoom() {
        setRoomId(null);
        if (isConnected()) sendTCP(new Packets.RoomClosureRequestPacket());
        close();
    }

    // NetListener callbacks
    @Override public void connected(Connection c) { requestRoomCreation(); }

    @Override public void disconnected(Connection c, DcReason r) {
        setRoomId(null);
        if (onRoomClosed != null) onRoomClosed.get(closeReason);
        closeAllConnections(r);
    }

    @Override public void received(Connection c, Object o) {
        try { PacketHandler.handle(o, this); }
        catch (Exception e) { arc.util.Log.info("Failed to handle: " + o); }
    }

    // Virtual connection management
    public VirtualConnection addConnection(int id) {
        VirtualConnection conn = new VirtualConnection(this, id, serverDispatcher);
        conns.put(id, conn);
        ordered.add(conn);
        return conn;
    }

    public VirtualConnection getConnection(int id) { return conns.get(id); }

    private void closeAllConnections(DcReason reason) {
        ordered.each(c -> c.closeQuietly(reason));
        conns.clear();
        ordered.clear();
    }

    private void requestRoomCreation() {
        Core.app.post(() -> {
            Packets.RoomCreationRequestPacket packet = new Packets.RoomCreationRequestPacket();
            packet.version = "1";
            packet.password = password;
            packet.data = PlayerConnect.getRoomStats();
            sendTCP(packet);
        });
    }

    // Accessors
    public String roomId() { return roomId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String r) { roomId = r; }
    public void setCloseReason(Packets.RoomClosedPacket.CloseReason r) { closeReason = r; }
    public Cons<String> getOnRoomCreated() { return onRoomCreated; }
    public Cons<Packets.RoomClosedPacket.CloseReason> getOnRoomClosed() { return onRoomClosed; }
    public Packets.RoomClosedPacket.CloseReason getCloseReason() { return closeReason; }
    public NetListener getServerDispatcher() { return serverDispatcher; }
}
