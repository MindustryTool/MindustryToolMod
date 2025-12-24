package mindustrytool.plugins.voicechat;

/**
 * Voice chat constants.
 */
public final class VoiceConstants {

    public static final int SAMPLE_RATE = 48000;
    public static final int CHANNELS = 1;
    public static final int BUFFER_SIZE = 960; // 20ms at 48kHz
    public static final int CAPTURE_INTERVAL_MS = 20;

    private VoiceConstants() {
    }
}
