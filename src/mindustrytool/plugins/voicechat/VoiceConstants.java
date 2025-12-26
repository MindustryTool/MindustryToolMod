package mindustrytool.plugins.voicechat;

/**
 * Voice chat constants.
 */
public final class VoiceConstants {

    public static final int SAMPLE_RATE = 48000;
    public static final int CHANNELS = 1;
    public static final int BUFFER_SIZE = 2880; // 60ms at 48kHz (Reduced PPS for CLaJ)
    public static final int CAPTURE_INTERVAL_MS = 60;

    private VoiceConstants() {
    }
}
