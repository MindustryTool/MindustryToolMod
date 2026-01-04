package mindustrytool.features.social.voice;

/**
 * Voice chat constants.
 */
public final class VoiceConstants {

    public static final int SAMPLE_RATE = 48000;
    public static final int CHANNELS = 1;
    public static final int BUFFER_SIZE = 1920; // 40ms at 48kHz (Lower latency)
    public static final int CAPTURE_INTERVAL_MS = 40;

    private VoiceConstants() {
    }
}
