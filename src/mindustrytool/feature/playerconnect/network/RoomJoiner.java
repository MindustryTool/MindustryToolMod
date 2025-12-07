package mindustrytool.feature.playerconnect.network;

import java.nio.ByteBuffer;
import mindustry.Vars;
import playerconnect.shared.Packets;

public class RoomJoiner {
    private static arc.net.NetSerializer tmpSerializer;
    private static ByteBuffer tmpBuffer = ByteBuffer.allocate(256);

    public static void join(PlayerConnectLink link, String pwd, Runnable onOk) {
        if (link == null) return;
        Vars.logic.reset();
        Vars.net.reset();
        Vars.netClient.beginConnecting();
        Vars.net.connect(link.host, link.port, () -> {
            if (!Vars.net.client()) return;
            if (tmpSerializer == null) tmpSerializer = new ProxySerializer();
            tmpBuffer.clear();
            Packets.RoomJoinPacket p = new Packets.RoomJoinPacket();
            p.password = pwd;
            p.roomId = link.roomId;
            tmpSerializer.write(tmpBuffer, p);
            tmpBuffer.limit(tmpBuffer.position()).position(0);
            Vars.net.send(tmpBuffer, true);
            onOk.run();
        });
    }
}
