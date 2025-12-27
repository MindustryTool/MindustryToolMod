package mindustrytool.plugins.voicechat;

import arc.util.Log;
import arc.util.Nullable;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Desktop speaker implementation using javax.sound.
 * 
 * Simplified direct-write approach:
 * - No queue, no separate thread
 * - play() directly calls speaker.write() which is blocking
 * - SourceDataLine handles timing internally
 * 
 * This is simpler and avoids queue-related timing issues.
 */
public class DesktopSpeaker {

    private static final String TAG = "[DesktopSpk]";

    // Buffer size: ~100ms at 48kHz mono 16-bit = 9600 bytes
    private static final int BUFFER_SIZE_MS = 100;

    @Nullable
    private SourceDataLine speaker;
    private final int sampleRate;

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

            // Calculate buffer size for ~100ms latency
            int bufferSize = (sampleRate * 2 * BUFFER_SIZE_MS) / 1000;
            speaker.open(format, bufferSize);
            speaker.start();

            Log.info("@ Speaker opened (direct mode, buffer=@ms)", TAG, BUFFER_SIZE_MS);
        } catch (Exception e) {
            Log.err("@ Failed to open speaker: @", TAG, e.getMessage());
        }
    }

    /**
     * Play audio data directly.
     * This method BLOCKS until data is written to the speaker buffer.
     * The blocking behavior naturally paces the audio playback.
     */
    public void play(short[] audioData) {
        if (speaker == null || !speaker.isOpen())
            return;

        byte[] bytes = shortsToBytes(audioData);

        // Direct write - speaker.write() is blocking and handles timing
        speaker.write(bytes, 0, bytes.length);
    }

    public boolean isOpen() {
        return speaker != null && speaker.isOpen();
    }

    public void close() {
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
