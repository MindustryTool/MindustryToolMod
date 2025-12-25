package lemmesay.shared.packet;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

/**
 * Packet sent by server to request voice chat capability from client.
 * This is the shared definition; handlers are implemented separately.
 */
public class VoiceRequestPacket extends Packet {
    private byte[] DATA;
    public int protocolVersion;

    public VoiceRequestPacket() {
        this.DATA = NODATA;
    }

    @Override
    public int getPriority() {
        return priorityLow;
    }

    @Override
    public void read(Reads read, int length) {
        this.DATA = read.b(length);
    }

    @Override
    public void write(Writes write) {
        write.i(protocolVersion);
    }

    @Override
    public void handled() {
        BAIS.setBytes(this.DATA);
        this.protocolVersion = READ.i();
    }
}
