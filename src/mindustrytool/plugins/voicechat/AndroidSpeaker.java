package mindustrytool.plugins.voicechat;

import arc.util.Log;

/**
 * Android speaker implementation using android.media.AudioTrack.
 * This class should only be loaded on Android platforms.
 * Uses reflection to avoid compile-time dependency on Android SDK.
 */
public class AndroidSpeaker {

    private static final String TAG = "[AndroidSpk]";

    private Object audioTrack; // android.media.AudioTrack
    private final int sampleRate;
    private int minBufferSize;

    // Android AudioTrack constants (via reflection to avoid compile dependency)
    private static final int CHANNEL_OUT_MONO = 4; // AudioFormat.CHANNEL_OUT_MONO
    private static final int ENCODING_PCM_16BIT = 2; // AudioFormat.ENCODING_PCM_16BIT
    private static final int STREAM_MUSIC = 3; // AudioManager.STREAM_MUSIC
    private static final int MODE_STREAM = 1; // AudioTrack.MODE_STREAM
    private static final int STATE_INITIALIZED = 1; // AudioTrack.STATE_INITIALIZED

    public AndroidSpeaker(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void open() {
        try {
            // Get minimum buffer size
            Class<?> audioTrackClass = Class.forName("android.media.AudioTrack");
            minBufferSize = (int) audioTrackClass.getMethod("getMinBufferSize", int.class, int.class, int.class)
                    .invoke(null, sampleRate, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT);

            if (minBufferSize <= 0) {
                throw new IllegalStateException("Invalid min buffer size: " + minBufferSize);
            }

            // Use larger buffer to avoid underruns
            int actualBufferSize = Math.max(minBufferSize, VoiceConstants.BUFFER_SIZE * 2 * 2);

            // Create AudioTrack instance
            audioTrack = audioTrackClass
                    .getConstructor(int.class, int.class, int.class, int.class, int.class, int.class)
                    .newInstance(STREAM_MUSIC, sampleRate, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT, actualBufferSize,
                            MODE_STREAM);

            // Check if initialized properly
            int state = (int) audioTrackClass.getMethod("getState").invoke(audioTrack);
            if (state != STATE_INITIALIZED) {
                throw new IllegalStateException("AudioTrack not initialized. State: " + state);
            }

            // Start playback
            audioTrack.getClass().getMethod("play").invoke(audioTrack);

            Log.info("@ Speaker opened (Android)", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to open speaker: @", TAG, e.getMessage());
        }
    }

    public void play(short[] audioData) {
        if (audioTrack == null)
            return;

        try {
            audioTrack.getClass()
                    .getMethod("write", short[].class, int.class, int.class)
                    .invoke(audioTrack, audioData, 0, audioData.length);
        } catch (Exception e) {
            Log.err("@ Failed to play audio: @", TAG, e.getMessage());
        }
    }

    public boolean isOpen() {
        if (audioTrack == null)
            return false;
        try {
            int state = (int) audioTrack.getClass().getMethod("getState").invoke(audioTrack);
            return state == STATE_INITIALIZED;
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        if (audioTrack == null)
            return;
        try {
            audioTrack.getClass().getMethod("stop").invoke(audioTrack);
            audioTrack.getClass().getMethod("release").invoke(audioTrack);
            audioTrack = null;
            Log.info("@ Speaker closed", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to close speaker: @", TAG, e.getMessage());
        }
    }
}
