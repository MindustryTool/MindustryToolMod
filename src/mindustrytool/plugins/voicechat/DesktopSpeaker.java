package mindustrytool.plugins.voicechat;

import arc.util.Log;
import arc.util.Nullable;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Desktop speaker implementation using javax.sound.
 * This class should only be loaded on Desktop platforms.
 * 
 * Now includes simple mixing: When multiple audio streams arrive within
 * the same playback window, they are mixed together instead of interleaved.
 */
public class DesktopSpeaker {

    private static final String TAG = "[DesktopSpk]";
    private static final int MIX_BUFFER_SIZE = VoiceConstants.BUFFER_SIZE; // 60ms of samples
    private static final int MIX_INTERVAL_MS = 60; // Flush buffer every 60ms

    @Nullable
    private SourceDataLine speaker;
    private final int sampleRate;
    private Thread playbackThread;
    private volatile boolean running = false;

    // Mixing buffer: accumulate samples here, mix if multiple arrive
    private final int[] mixBuffer = new int[MIX_BUFFER_SIZE]; // Use int to prevent overflow during mix
    private int mixBufferSamples = 0; // Number of sources mixed into current buffer
    private long lastFlushTime = 0;
    private final Object mixLock = new Object();

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
            speaker.open(format, MIX_BUFFER_SIZE * 4); // Larger buffer for smoother playback
            speaker.start();

            running = true;
            lastFlushTime = System.currentTimeMillis();

            // Playback thread with timed flushing
            playbackThread = new Thread(this::mixingPlaybackLoop, "VoiceChat-Playback");
            playbackThread.setDaemon(true);
            playbackThread.start();

            Log.info("@ Speaker opened (mixing mode)", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to open speaker: @", TAG, e.getMessage());
        }
    }

    /**
     * Mixing playback loop: flushes mixed audio every MIX_INTERVAL_MS.
     */
    private void mixingPlaybackLoop() {
        while (running) {
            try {
                Thread.sleep(MIX_INTERVAL_MS);
                flushMixBuffer();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.err("@ Playback error: @", TAG, e.getMessage());
            }
        }
    }

    /**
     * Add audio to the mixing buffer.
     * If other audio is already in the buffer, it will be mixed (summed).
     */
    public void play(short[] audioData) {
        if (speaker == null || !speaker.isOpen())
            return;

        synchronized (mixLock) {
            int len = Math.min(audioData.length, MIX_BUFFER_SIZE);
            for (int i = 0; i < len; i++) {
                mixBuffer[i] += audioData[i]; // Add to existing (mixing)
            }
            mixBufferSamples++;
        }
    }

    /**
     * Flush the mixing buffer to the speaker.
     * Applies soft clipping to prevent distortion.
     */
    private void flushMixBuffer() {
        synchronized (mixLock) {
            if (mixBufferSamples == 0) {
                // No audio received, clear buffer
                java.util.Arrays.fill(mixBuffer, 0);
                return;
            }

            // Convert mixed int samples to short with soft clipping
            short[] output = new short[MIX_BUFFER_SIZE];
            for (int i = 0; i < MIX_BUFFER_SIZE; i++) {
                output[i] = softClip(mixBuffer[i]);
                mixBuffer[i] = 0; // Clear for next cycle
            }
            mixBufferSamples = 0;

            // Write to speaker
            if (speaker != null && speaker.isOpen()) {
                byte[] bytes = shortsToBytes(output);
                speaker.write(bytes, 0, bytes.length);
            }
        }
    }

    /**
     * Soft clip to prevent harsh distortion when mixing multiple sources.
     */
    private short softClip(int sample) {
        if (sample >= Short.MAX_VALUE)
            return Short.MAX_VALUE;
        if (sample <= Short.MIN_VALUE)
            return Short.MIN_VALUE;

        final int threshold = 24000;
        if (Math.abs(sample) > threshold) {
            double normalized = sample / (double) Short.MAX_VALUE;
            double compressed = Math.tanh(normalized * 1.5) / Math.tanh(1.5);
            return (short) (compressed * Short.MAX_VALUE);
        }
        return (short) sample;
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
