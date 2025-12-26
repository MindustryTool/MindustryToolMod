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

        // List all available mixers for debugging
        Log.info("@ Available Audio Mixers:", TAG);
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Log.info("  - @ (@)", info.getName(), info.getDescription());
        }

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, 1, 2, sampleRate, false);

        mic = getDefaultMicrophone(format);
        
        // Fallback to 44100Hz if 48000Hz fails
        if (mic == null && sampleRate == 48000) {
             Log.warn("@ 48000Hz not supported, trying 44100Hz...", TAG);
             AudioFormat fallbackFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100, 16, 1, 2, 44100, false);
             mic = getDefaultMicrophone(fallbackFormat);
        }

        if (mic == null) {
            throw new IllegalStateException("No microphone found on this system");
        }

        try {
            // Ensure we open with the format that worked (or the original if we didn't fallback)
            mic.open(mic.getFormat()); 
            Log.info("@ Microphone opened. Format: @", TAG, mic.getFormat());
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
        // Always return bufferSize to force read() to be called.
        // The read() method will block until data is available.
        return bufferSize;
    }

    public short[] read() {
        if (mic == null) {
            throw new IllegalStateException("Microphone is not opened");
        }

        byte[] byteBuffer = new byte[bufferSize * 2];
        int totalRead = 0;
        
        // Blocking read loop
        while (totalRead < byteBuffer.length && isOpen()) {
            int read = mic.read(byteBuffer, totalRead, byteBuffer.length - totalRead);
            if (read > 0) {
                totalRead += read;
            } else {
                // Avoid busy loop if something is wrong but mic is still open
                try { Thread.sleep(1); } catch (InterruptedException e) { break; }
            }
        }
        
        // Debug log (throttled)
        if (Math.random() < 0.01) { // Log ~1% of reads to avoid spam
             Log.info("@ Read @ bytes from mic", TAG, totalRead);
        }
        
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
