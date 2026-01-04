package mindustrytool.features.social.voice.protocol.shared;

/**
 * Shared constants for Voice Chat protocol.
 */
public final class VoiceProtocol {

    /**
     * Current protocol version. Must match between client and server.
     */
    public static final int PROTOCOL_VERSION = 1;

    /**
     * Magic number used to identify Voice Chat connections.
     * Sent via PingCallPacket time field.
     */
    public static final long PING_MAGIC = -291103L;

    // Response codes for VoiceResponsePacket
    public static final byte RESPONSE_ACCEPTED = 1;
    public static final byte RESPONSE_CLIENT_OUTDATED = 2;
    public static final byte RESPONSE_SERVER_OUTDATED = 3;
    public static final byte RESPONSE_UNKNOWN = 4;

    private VoiceProtocol() {
        // Prevent instantiation
    }
}
