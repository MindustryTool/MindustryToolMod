package mindustrytool.features.social.voice.protocol.shared.packet;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.io.TypeIO;
import mindustry.net.Packet;

/**
 * Packet for transmitting microphone audio data.
 * This is the shared definition; handlers are implemented separately.
 */
public class MicPacket extends Packet {
    private byte[] DATA;
    public byte[] audioData;
    public int playerid;
    public int sequence; // Sequence number for jitter buffer reordering

    public MicPacket() {
        this.DATA = NODATA;
    }

    @Override
    public int getPriority() {
        return priorityNormal;
    }

    @Override
    public void read(Reads read, int length) {
        this.playerid = read.i();
        this.sequence = read.i();
        this.DATA = read.b(length - 8); // id(4) + seq(4) = 8 bytes header
    }

    @Override
    public void write(Writes write) {
        write.i(this.playerid);
        write.i(this.sequence);
        TypeIO.writeBytes(write, this.audioData);
    }

    @Override
    public void handled() {
        BAIS.setBytes(this.DATA);
        this.audioData = TypeIO.readBytes(READ);
    }
}
