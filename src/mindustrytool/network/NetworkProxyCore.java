package mindustrytool.network;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import arc.func.Cons;
import arc.net.*;
import arc.struct.*;
import arc.util.Reflect;
import mindustry.Vars;
import playerconnect.shared.Packets;

public class NetworkProxyCore extends Client implements NetListener {
    public static int defaultTimeout = 10000;
    private final IntMap<VirtualConnection> conns = new IntMap<>();
    private final Seq<VirtualConnection> ordered = new Seq<>(false);
    private final arc.net.Server server;
    private final NetListener serverDispatcher;
    @SuppressWarnings("unused") private volatile String password = "", roomId = null;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private volatile Packets.RoomClosedPacket.CloseReason closeReason;
    private volatile Cons<String> onRoomCreated;
    private volatile Cons<Packets.RoomClosedPacket.CloseReason> onRoomClosed;

    public NetworkProxyCore(String pwd) {
        super(32768, 16384, new ProxySerializer());
        addListener(this);
        mindustry.net.Net.NetProvider prov = Reflect.get(Vars.net, "provider");
        if (Vars.steam) prov = Reflect.get(prov, "provider");
        server = Reflect.get(prov, "server"); serverDispatcher = Reflect.get(server, "dispatchListener"); this.password = pwd;
    }

    public void connect(String host, int port, Cons<String> onCreate, Cons<Packets.RoomClosedPacket.CloseReason> onClose) throws IOException { this.onRoomCreated = onCreate; this.onRoomClosed = onClose; connect(defaultTimeout, host, port, port); }

    @Override public void run() {
        isShutdown.set(false);
        while (!isShutdown.get()) {
            try { update(250); for (int i = 0; i < ordered.size; i++) { VirtualConnection c = ordered.get(i); if (c.isConnected() && c.isIdle()) c.notifyIdle0(); } }
            catch (IOException ex) { close(); } catch (ArcNetException ex) { if (roomId == null) { close(); throw ex; } }
        }
    }

    @Override public void stop() { if (isShutdown.get()) return; close(); isShutdown.set(true); java.nio.channels.Selector sel = Reflect.get(Client.class, this, "selector"); sel.wakeup(); }
    public String getRoomId() { return roomId; } public void setRoomId(String r) { roomId = r; } public void setCloseReason(Packets.RoomClosedPacket.CloseReason r) { closeReason = r; }
    public Cons<String> getOnRoomCreated() { return onRoomCreated; } public Cons<Packets.RoomClosedPacket.CloseReason> getOnRoomClosed() { return onRoomClosed; }
    public Packets.RoomClosedPacket.CloseReason getCloseReason() { return closeReason; } public VirtualConnection getConnection(int id) { return conns.get(id); }
    public VirtualConnection addConnection(int id, NetListener d) { VirtualConnection c = new VirtualConnection(this, id, d); conns.put(id, c); ordered.add(c); return c; }
    public NetListener getServerDispatcher() { return serverDispatcher; }
}
