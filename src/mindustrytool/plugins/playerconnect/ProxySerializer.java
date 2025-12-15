package mindustrytool.plugins.playerconnect;

import java.nio.ByteBuffer;
import arc.util.io.*;
import playerconnect.shared.Packets;

/** Serializer for Player Connect protocol packets. */
public class ProxySerializer extends mindustry.net.ArcNetProvider.PacketSerializer {
    @Override
    public Object read(ByteBuffer b) {
        if (b.get() == Packets.id) {
            Packets.Packet p = Packets.newPacket(b.get());
            p.read(new ByteBufferInput(b));
            if (p instanceof Packets.ConnectionPacketWrapPacket) 
                ((Packets.ConnectionPacketWrapPacket) p).object = super.read(b);
            return p;
        }
        b.position(b.position() - 1);
        return super.read(b);
    }

    @Override
    public void write(ByteBuffer b, Object o) {
        if (o instanceof Packets.Packet) {
            Packets.Packet p = (Packets.Packet) o;
            b.put(Packets.id).put(Packets.getId(p));
            p.write(new ByteBufferOutput(b));
            if (p instanceof Packets.ConnectionPacketWrapPacket) 
                super.write(b, ((Packets.ConnectionPacketWrapPacket) p).object);
            return;
        }
        super.write(b, o);
    }
}
