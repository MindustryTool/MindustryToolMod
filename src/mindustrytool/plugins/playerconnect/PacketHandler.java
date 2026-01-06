package mindustrytool.plugins.playerconnect;

import arc.net.*;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Call;
import playerconnect.shared.Packets;

/** Handles incoming packets from the Player Connect proxy. */
public class PacketHandler {
    public static void handle(Object obj, NetworkProxy proxy) {
        if (!(obj instanceof Packets.Packet)) return;
        if (obj instanceof Packets.MessagePacket) { Call.sendMessage("[scarlet][[Server]:[] " + ((Packets.MessagePacket) obj).message); return; }
        if (obj instanceof Packets.Message2Packet) { Call.sendMessage("[scarlet][[Server]:[] " + arc.Core.bundle.get("claj.message." + arc.util.Strings.camelToKebab(((Packets.Message2Packet) obj).message.name()))); return; }
        if (obj instanceof Packets.PopupPacket) { Vars.ui.showText("[scarlet][[Server][] ", ((Packets.PopupPacket) obj).message); return; }
        if (obj instanceof Packets.RoomClosedPacket) { proxy.setCloseReason(((Packets.RoomClosedPacket) obj).reason); return; }
        if (obj instanceof Packets.RoomLinkPacket) {
            if (proxy.getRoomId() != null) return;
            String rid = ((Packets.RoomLinkPacket) obj).roomId;
            proxy.setRoomId(rid);
            if (rid != null && proxy.getOnRoomCreated() != null) proxy.getOnRoomCreated().get(rid);
            return;
        }
        if (obj instanceof Packets.ConnectionWrapperPacket) handleConnection((Packets.ConnectionWrapperPacket) obj, proxy);
    }

    private static void handleConnection(Packets.ConnectionWrapperPacket w, NetworkProxy p) {
        if (p.getRoomId() == null) return;
        int id = w.connectionId;
        VirtualConnection con = p.getConnection(id);
        if (con == null) {
            if (w instanceof Packets.ConnectionJoinPacket) {
                if (!((Packets.ConnectionJoinPacket) w).roomId.equals(p.getRoomId())) {
                    Packets.ConnectionClosedPacket pk = new Packets.ConnectionClosedPacket();
                    pk.connectionId = id; pk.reason = DcReason.error;
                    p.sendTCP(pk); return;
                }
                con = p.addConnection(id);
                con.notifyConnected0();
                ((mindustry.net.NetConnection) con.getArbitraryData()).packetRate = new NoopRatekeeper();
                ((mindustry.net.NetConnection) con.getArbitraryData()).chatRate = new NoopRatekeeper();
            }
        } else if (w instanceof Packets.ConnectionPacketWrapPacket) con.notifyReceived0(((Packets.ConnectionPacketWrapPacket) w).object);
        else if (w instanceof Packets.ConnectionIdlingPacket) con.setIdle();
        else if (w instanceof Packets.ConnectionClosedPacket) { con.closeQuietly(((Packets.ConnectionClosedPacket) w).reason); Log.info("Close connection from server"); }
    }
}
