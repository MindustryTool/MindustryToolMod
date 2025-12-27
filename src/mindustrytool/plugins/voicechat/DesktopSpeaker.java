package mindustrytool.plugins.voicechat;

import arc.util.Log;
import arc.util.Nullable;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Desktop speaker implementation using javax.sound.
 * 
 * Optimized approach:
 * - Separate high-priority playback thread (doesn't block network)
 * - Explicit buffer size for predictable latency
 * - Small queue to smooth minor jitter
 * - Drops old audio if queue is full (prevents latency buildup)
 */
public class DesktopSpeaker {

    private static final String TAG = "[DesktopSpk]";

    // Buffer size: 80ms for low latency
    private static final int BUFFER_SIZE_MS = 80;
    private static final int QUEUE_SIZE = 20; // Larger queue, no dropping

    @Nullable
    private SourceDataLine speaker;
    private final int sampleRate;
    private final LinkedBlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private Thread playbackThread;
    private volatile boolean running = false;

    public DesktopSpeaker(int sampleRate) {
        this.sampleRate = sampleRate;
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

            Log.info("@ Speaker opened (buffer=@ms, queue=@)", TAG, BUFFER_SIZE_MS, QUEUE_SIZE);
        } catch (Exception e) {
            Log.err("@ Failed to open speaker: @", TAG, e.getMessage());
        }
    }

    private void playbackLoop() {
        while (running) {
            try {
                // Block until audio is available
                byte[] data = audioQueue.take();
                if (speaker != null && speaker.isOpen()) {
                    // Write all data at once
                    speaker.write(data, 0, data.length);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.err("@ Playback error: @", TAG, e.getMessage());
            }
        }
    }

    /**
     * Queue audio for playback.
     * Uses large queue to avoid dropping audio.
     */
    public void play(short[] audioData) {
        if (speaker == null || !speaker.isOpen())
            return;

        byte[] bytes = shortsToBytes(audioData);

        // Simple offer - queue is large enough to handle bursts
        audioQueue.offer(bytes);
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
        audioQueue.clear();

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
