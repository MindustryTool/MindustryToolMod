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
    public int playerid;

    public MicPacket() {
        this.DATA = NODATA;
    }

    @Override
    public int getPriority() {
        return priorityLow;
    }

    @Override
    public void read(Reads read, int length) {
        this.playerid = read.i();
        this.DATA = read.b(length - 4);
    }

    @Override
    public void write(Writes write) {
        write.i(this.playerid);
        TypeIO.writeBytes(write, this.audioData);
    }

    @Override
    public void handled() {
        // Server-side handling: if this is received on server, we might want to relay
        // it.
        // But for client, we just play it.
        // Wait, handleClient parses it? No, Client uses the packet directly or
        // handleClient lambda.
        // BAIS.setBytes is used if we parse explicitly?
        // Let's keep existing handled() logic but aware of reading?
        // actually handled() is often used for packet logic in standard Mindustry,
        // but here we use a listener. The read method does the work.
        // handled() logic in original file was:
        // BAIS.setBytes(this.DATA); this.audioData = TypeIO.readBytes(READ);

        BAIS.setBytes(this.DATA);
        this.audioData = TypeIO.readBytes(READ);
    }
}
