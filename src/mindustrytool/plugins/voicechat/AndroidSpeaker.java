package mindustrytool.plugins.voicechat;

import arc.util.Log;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Android speaker implementation using android.media.AudioTrack.
 * This class should only be loaded on Android platforms.
 * Uses reflection to avoid compile-time dependency on Android SDK.
 * 
 * Optimized to prevent Main Thread blocking and Reflection overhead.
 */
public class AndroidSpeaker {

    private static final String TAG = "[AndroidSpk]";

    private Object audioTrack; // android.media.AudioTrack
    private final int sampleRate;
    private int minBufferSize;

    // Reflection Cache (Performance Optimization)
    private Method writeMethod;
    private Method playMethod;
    private Method stopMethod;
    private Method releaseMethod;

    // Threading to avoid blocking Main Thread
    private final BlockingQueue<short[]> audioQueue = new LinkedBlockingQueue<>(20);
    private Thread playbackThread;
    private volatile boolean running = false;

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
        if (isOpen())
            return;

        try {
            Class<?> audioTrackClass = Class.forName("android.media.AudioTrack");

            // Get minimum buffer size
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

            // Cache methods
            writeMethod = audioTrackClass.getMethod("write", short[].class, int.class, int.class);
            playMethod = audioTrackClass.getMethod("play");
            stopMethod = audioTrackClass.getMethod("stop");
            releaseMethod = audioTrackClass.getMethod("release");

            // Start playback
            playMethod.invoke(audioTrack);

            // Start processing thread
            running = true;
            playbackThread = new Thread(this::playbackLoop, "AndroidSpeaker-Thread");
            playbackThread.setDaemon(true);
            playbackThread.start();

            Log.info("@ Speaker opened (Android/Optimized)", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to open speaker: @", TAG, e.getMessage());
        }
    }

    private void playbackLoop() {
        while (running) {
            try {
                // Blocking take - waits if empty
                short[] data = audioQueue.take();

                if (audioTrack != null && writeMethod != null) {
                    // This call might block if hardware buffer is full, but it's fine on this
                    // thread
                    writeMethod.invoke(audioTrack, data, 0, data.length);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.err("@ Playback error: @", TAG, e.getMessage());
            }
        }
    }

    public void play(short[] audioData) {
        // Non-blocking offer. Drop if full (better than freezing game)
        if (!audioQueue.offer(audioData)) {
            // Optional: Log drop or remove oldest
            audioQueue.poll();
            audioQueue.offer(audioData);
        }
    }

    public boolean isOpen() {
        if (audioTrack == null)
            return false;
        try {
            // For checking state, we might still use reflection or just assume valid if not
            // null
            // Re-using getState method if cached would be safer but might thread-conflict?
            // Actually, AudioTrack methods are thread-safe.
            int state = (int) audioTrack.getClass().getMethod("getState").invoke(audioTrack); // Simple check
            return state == STATE_INITIALIZED;
        } catch (Exception e) {
            return false;
        }
    }

    public void close() {
        running = false;
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }

        if (audioTrack == null)
            return;
        try {
            if (stopMethod != null)
                stopMethod.invoke(audioTrack);
            if (releaseMethod != null)
                releaseMethod.invoke(audioTrack);
            audioTrack = null;
            Log.info("@ Speaker closed", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to close speaker: @", TAG, e.getMessage());
        }
    }
}
