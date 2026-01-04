package mindustrytool.features.social.voice.protocol.shared.packet;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

/**
 * Packet sent by client in response to VoiceRequestPacket.
 * This is the shared definition; handlers are implemented separately.
 */
public class VoiceResponsePacket extends Packet {
    // Response codes are defined in VoiceProtocol

    private byte[] DATA;
    public byte responseCode;

    public VoiceResponsePacket() {
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

    public void write(Writes write) {
        write.b(responseCode);
    }

    public void handled() {
        BAIS.setBytes(this.DATA);
        this.responseCode = READ.b();
    }
}
