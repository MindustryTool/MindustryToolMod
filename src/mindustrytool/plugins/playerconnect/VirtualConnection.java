package mindustrytool.plugins.playerconnect;

import arc.net.*;
import arc.struct.Seq;
import java.net.InetSocketAddress;
import playerconnect.shared.Packets;

/** Virtual connection wrapper for proxied client connections. */
public class VirtualConnection extends Connection {
    public final Seq<NetListener> listeners = new Seq<>();
    public final int id;
    public volatile boolean isConnected = true, isIdling = true;
    private NetworkProxy proxy;

    public VirtualConnection(NetworkProxy p, int i, NetListener d) { proxy = p; id = i; addListener(d); }

    @Override public int sendTCP(Object o) {
        if (o == null) throw new IllegalArgumentException("object cannot be null.");
        isIdling = false;
        Packets.ConnectionPacketWrapPacket p = new Packets.ConnectionPacketWrapPacket();
        p.connectionId = id; p.isTCP = true; p.object = o; return proxy.sendTCP(p);
    }

    @Override public int sendUDP(Object o) {
        if (o == null) throw new IllegalArgumentException("object cannot be null.");
        isIdling = false;
        Packets.ConnectionPacketWrapPacket p = new Packets.ConnectionPacketWrapPacket();
        p.connectionId = id; p.isTCP = false; p.object = o; return proxy.sendUDP(p);
    }

    @Override public void close(DcReason r) {
        boolean was = isConnected; isConnected = isIdling = false;
        if (was) { Packets.ConnectionClosedPacket p = new Packets.ConnectionClosedPacket(); p.connectionId = id; p.reason = r; proxy.sendTCP(p); for (NetListener l : listeners) l.disconnected(this, r); }
    }

    public void closeQuietly(DcReason r) { boolean was = isConnected; isConnected = isIdling = false; if (was) for (NetListener l : listeners) l.disconnected(this, r); }
    @Override public int getID() { return id; } @Override public boolean isConnected() { return isConnected; }
    @Override public boolean isIdle() { return isIdling; } public void setIdle() { isIdling = true; }
    @Override public void setKeepAliveTCP(int ms) {} @Override public void setTimeout(int ms) {} @Override public void setIdleThreshold(float t) {}
    @Override public InetSocketAddress getRemoteAddressTCP() { return isConnected() ? proxy.getRemoteAddressTCP() : null; }
    @Override public InetSocketAddress getRemoteAddressUDP() { return isConnected() ? proxy.getRemoteAddressUDP() : null; }
    @Override public int getTcpWriteBufferSize() { return 0; }
    @Override public void addListener(NetListener l) { if (l == null) throw new IllegalArgumentException("listener cannot be null."); listeners.add(l); }
    @Override public void removeListener(NetListener l) { if (l == null) throw new IllegalArgumentException("listener cannot be null."); listeners.remove(l); }
    void notifyConnected0() { for (NetListener l : listeners) l.connected(this); }
    void notifyReceived0(Object o) { for (NetListener l : listeners) l.received(this, o); }
    void notifyIdle0() { for (NetListener l : listeners) if (isIdle()) l.idle(this); }
}
