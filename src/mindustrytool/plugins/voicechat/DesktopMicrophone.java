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
    private final int targetSampleRate; // The rate we WANT (48000)
    private int actualSampleRate; // The rate we GOT
    private final int bufferSize;

    public DesktopMicrophone(int sampleRate, int bufferSize) {
        this.targetSampleRate = sampleRate;
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

        // Try 48000Hz first (Best quality, native Opus rate)
        mic = tryOpen(48000);
        
        // Fallback: 44100Hz (Standard CD quality)
        if (mic == null) mic = tryOpen(44100);
        
        // Fallback: 16000Hz (Wideband VoIP)
        if (mic == null) mic = tryOpen(16000);
        
        // Fallback: 8000Hz (Narrowband, last resort)
        if (mic == null) mic = tryOpen(8000);

        if (mic == null) {
            throw new IllegalStateException("No microphone found on this system. Checked 48k, 44.1k, 16k, 8k.");
        }

        try {
            mic.open(mic.getFormat()); 
            mic.start(); // Start immediately
            Log.info("@ Microphone opened. Format: @", TAG, mic.getFormat());
            
            actualSampleRate = (int) mic.getFormat().getSampleRate();
            if (actualSampleRate != targetSampleRate) {
                Log.warn("@ Sample rate mismatch! Target: @, Actual: @. Resampling enabled.", TAG, targetSampleRate, actualSampleRate);
            }
            
        } catch (LineUnavailableException e) {
            throw new IllegalStateException("Failed to open microphone line", e);
        }
    }
    
    private TargetDataLine tryOpen(int rate) {
        try {
            AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                rate, 16, 1, 2, rate, false); // 16-bit, mono, signed, little-endian
            
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (AudioSystem.isLineSupported(info)) {
                Log.info("@ Found supported format: @Hz", TAG, rate);
                return (TargetDataLine) AudioSystem.getLine(info);
            }
        } catch (Exception e) {
            // Ignore and try next
        }
        return null;
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
        // If we need to resample, we might need more or less data, 
        // but for simplicity we just report what the mic has.
        return mic.available();
    }

    public short[] read() {
        if (mic == null) {
            throw new IllegalStateException("Microphone is not opened");
        }

        // Calculate how many bytes to read to produce 'bufferSize' samples at target rate
        // If rates match, it's just bufferSize * 2 bytes.
        // If resampling, we need (actual / target) * bufferSize samples.
        
        int samplesNeeded = bufferSize;
        if (actualSampleRate != targetSampleRate) {
            samplesNeeded = (int) (bufferSize * ((double) actualSampleRate / targetSampleRate));
        }
        
        // Ensure even number of bytes (16-bit samples)
        int bytesNeeded = samplesNeeded * 2;
        if (bytesNeeded % 2 != 0) bytesNeeded++;

        byte[] byteBuffer = new byte[bytesNeeded];
        int totalRead = 0;
        
        // Blocking read loop
        while (totalRead < bytesNeeded && isOpen()) {
            int read = mic.read(byteBuffer, totalRead, bytesNeeded - totalRead);
            if (read > 0) {
                totalRead += read;
            } else {
                try { Thread.sleep(1); } catch (InterruptedException e) { break; }
            }
        }
        
        short[] rawShorts = bytesToShorts(byteBuffer);
        
        // Resample if necessary
        if (actualSampleRate != targetSampleRate) {
            return resample(rawShorts, actualSampleRate, targetSampleRate);
        }
        
        return rawShorts;
    }
    
    /**
     * Simple linear interpolation resampler.
     */
    private short[] resample(short[] input, int fromRate, int toRate) {
        if (input.length == 0) return input;
        
        double ratio = (double) fromRate / toRate;
        int newLength = (int) (input.length / ratio);
        short[] output = new short[newLength];
        
        for (int i = 0; i < newLength; i++) {
            double index = i * ratio;
            int left = (int) index;
            int right = left + 1;
            
            if (left >= input.length) left = input.length - 1;
            if (right >= input.length) right = input.length - 1;
            
            double weight = index - left;
            
            // Linear interpolation
            output[i] = (short) (input[left] * (1.0 - weight) + input[right] * weight);
        }
        
        return output;
    }

    private static short[] bytesToShorts(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        short[] shorts = new short[bytes.length / 2];
        bb.asShortBuffer().get(shorts);
        return shorts;
    }
}
