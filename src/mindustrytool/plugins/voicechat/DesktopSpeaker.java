package mindustrytool.plugins.voicechat;

import arc.util.Log;
import arc.util.Nullable;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Desktop speaker implementation using javax.sound.
 * 
 * Optimized Pull-Based Architecture:
 * - Playback thread polls AudioMixer for data.
 * - This ensures mixing happens at the exact rate of hardware consumption.
 * - Prevents drift, looping, and stuttering.
 */
public class DesktopSpeaker {

    private static final String TAG = "[DesktopSpk]";

    // Buffer size: 80ms for low latency
    private static final int BUFFER_SIZE_MS = 80;

    @Nullable
    private SourceDataLine speaker;
    private final int sampleRate;
    private Thread playbackThread;
    private volatile boolean running = false;

    // Reference to mixer (Source of audio)
    private AudioMixer mixer;

    public DesktopSpeaker(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void setMixer(AudioMixer mixer) {
        this.mixer = mixer;
    }

    public void open() {
        if (isOpen())
            return;

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, 1, 2, sampleRate, false);

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            speaker = (SourceDataLine) AudioSystem.getLine(info);

            // Explicit buffer size for predictable latency
            int bufferSize = (sampleRate * 2 * BUFFER_SIZE_MS) / 1000;
            speaker.open(format, bufferSize);
            speaker.start();

            running = true;
            playbackThread = new Thread(this::playbackLoop, "VoiceChat-Playback");
            playbackThread.setDaemon(true);
            playbackThread.setPriority(Thread.MAX_PRIORITY); // High priority for audio
            playbackThread.start();

            Log.info("@ Speaker opened (Pull-mode, buffer=@ms)", TAG, BUFFER_SIZE_MS);
        } catch (Exception e) {
            Log.err("@ Failed to open speaker: @", TAG, e.getMessage());
        }
    }

    private void playbackLoop() {
        while (running) {
            try {
                if (speaker == null || !speaker.isOpen()) {
                    Thread.sleep(100);
                    continue;
                }

                short[] data = null;

                // Pull from mixer if available
                if (mixer != null) {
                    data = mixer.mixOneChunk();
                }

                if (data != null) {
                    // Play mixed chunk
                    byte[] bytes = shortsToBytes(data);
                    speaker.write(bytes, 0, bytes.length); // Blocking write
                } else {
                    // No audio available - wait a bit to avoid CPU spin
                    // Small sleep (10ms) allows checking for new audio frequently
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.err("@ Playback error: @", TAG, e.getMessage());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    /**
     * Legacy method for direct play (if mixer not used).
     * Since we switched to pull-based, this does nothing or could be fallback?
     * Ideally, VoiceChatManager should ONLY use mixer.queueAudio.
     */
    public void play(short[] audioData) {
        // Direct queueing is deprecated in Pull-mode.
        // Data should go to Mixer.
    }

    public boolean isOpen() {
        return speaker != null && speaker.isOpen();
    }

    public void close() {
        running = false;
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }

        if (speaker == null)
            return;
        speaker.stop();
        speaker.flush();
        speaker.close();
        speaker = null;
        Log.info("@ Speaker closed", TAG);
    }

    private static byte[] shortsToBytes(short[] shorts) {
        ByteBuffer bb = ByteBuffer.allocate(shorts.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : shorts) {
            bb.putShort(s);
        }
        return bb.array();
    }
}
