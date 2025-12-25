package mindustrytool.plugins.voicechat;

import arc.util.Log;
import arc.util.Nullable;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Desktop microphone implementation using javax.sound.
 * This class should only be loaded on Desktop platforms.
 */
public class DesktopMicrophone {

    private static final String TAG = "[DesktopMic]";

    @Nullable
    private TargetDataLine mic;
    private final int sampleRate;
    private final int bufferSize;

    public DesktopMicrophone(int sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
    }

    public void open() {
        if (isOpen()) {
            throw new IllegalStateException("Microphone is already open");
        }

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, 1, 2, sampleRate, false);

        mic = getDefaultMicrophone(format);
        if (mic == null) {
            throw new IllegalStateException("No microphone found on this system");
        }

        try {
            mic.open(format);
            Log.info("@ Microphone opened", TAG);
        } catch (LineUnavailableException e) {
            throw new IllegalStateException("Failed to open microphone", e);
        }

        // Fix accumulating audio issue on some Linux systems
        mic.start();
        mic.stop();
        mic.flush();
    }

    public void start() {
        if (!isOpen() || mic == null)
            return;
        mic.start();
    }

    public void stop() {
        if (!isOpen() || mic == null)
            return;
        mic.stop();
        mic.flush();
    }

    public void close() {
        if (mic == null)
            return;
        mic.stop();
        mic.flush();
        mic.close();
        mic = null;
        Log.info("@ Microphone closed", TAG);
    }

    public boolean isOpen() {
        return mic != null && mic.isOpen();
    }

    public int available() {
        if (mic == null)
            return 0;
        return mic.available() / 2;
    }

    public short[] read() {
        if (mic == null) {
            throw new IllegalStateException("Microphone is not opened");
        }

        int available = available();
        if (bufferSize > available) {
            throw new IllegalStateException("Not enough data. Required: " + bufferSize + ", available: " + available);
        }

        byte[] byteBuffer = new byte[bufferSize * 2];
        mic.read(byteBuffer, 0, byteBuffer.length);
        return bytesToShorts(byteBuffer);
    }

    private static short[] bytesToShorts(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        short[] shorts = new short[bytes.length / 2];
        bb.asShortBuffer().get(shorts);
        return shorts;
    }

    @Nullable
    private static TargetDataLine getDefaultMicrophone(AudioFormat format) {
        try {
            DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);
            return (TargetDataLine) AudioSystem.getLine(lineInfo);
        } catch (Exception e) {
            Log.err("@ Failed to get microphone: @", TAG, e.getMessage());
            return null;
        }
    }
}
