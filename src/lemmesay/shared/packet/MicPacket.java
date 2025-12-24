package lemmesay.shared.packet;

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

    public MicPacket() {
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
        TypeIO.writeBytes(write, this.audioData);
    }

    @Override
    public void handled() {
        BAIS.setBytes(this.DATA);
        this.audioData = TypeIO.readBytes(READ);
    }
}
