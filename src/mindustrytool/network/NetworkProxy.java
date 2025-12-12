package mindustrytool.network;

import arc.net.*;
import arc.struct.*;
import playerconnect.shared.Packets;

public class NetworkProxy extends NetworkProxyCore {
    private final ConnectionManager connMgr;
    private String password;

    public NetworkProxy(String pwd) {
        super(pwd);
        this.password = pwd;
        IntMap<VirtualConnection> conns = new IntMap<>();
        Seq<VirtualConnection> ordered = new Seq<>(false);
        mindustry.net.Net.NetProvider prov = arc.util.Reflect.get(mindustry.Vars.net, "provider");
        if (mindustry.Vars.steam) prov = arc.util.Reflect.get(prov, "provider");
        arc.net.Server srv = arc.util.Reflect.get(prov, "server");
        NetListener disp = arc.util.Reflect.get(srv, "dispatchListener");
        connMgr = new ConnectionManager(this, conns, ordered, disp);
    }

    public void closeRoom() {
        setRoomId(null);
        if (isConnected()) sendTCP(new Packets.RoomClosureRequestPacket());
        close();
    }

    @Override public void connected(Connection c) { connMgr.requestRoomCreation(password); }

    @Override public void disconnected(Connection c, DcReason r) {
        setRoomId(null);
        if (getOnRoomClosed() != null) getOnRoomClosed().get(getCloseReason());
        connMgr.closeAll(r);
    }

    @Override public void received(Connection c, Object o) {
        try { PacketHandler.handle(o, this); } 
        catch (Exception e) { arc.util.Log.info("Failed to handle: " + o); }
    }

    public VirtualConnection addConnection(int id) { return connMgr.add(id); }
    public String roomId() { return getRoomId(); }
}
