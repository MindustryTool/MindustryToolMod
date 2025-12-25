package mindustrytool.plugins.voicechat;

import arc.util.Log;

/**
 * Android microphone implementation using android.media.AudioRecord.
 * This class should only be loaded on Android platforms.
 * Uses reflection to avoid compile-time dependency on Android SDK.
 */
public class AndroidMicrophone {

    private static final String TAG = "[AndroidMic]";

    private Object audioRecord; // android.media.AudioRecord
    private final int sampleRate;
    private final int bufferSize;
    private int minBufferSize;
    private boolean isRecording = false;

    // Android AudioRecord constants (via reflection to avoid compile dependency)
    private static final int CHANNEL_IN_MONO = 16; // AudioFormat.CHANNEL_IN_MONO
    private static final int ENCODING_PCM_16BIT = 2; // AudioFormat.ENCODING_PCM_16BIT
    private static final int SOURCE_MIC = 1; // MediaRecorder.AudioSource.MIC
    private static final int STATE_INITIALIZED = 1; // AudioRecord.STATE_INITIALIZED
    private static final int RECORDSTATE_RECORDING = 3; // AudioRecord.RECORDSTATE_RECORDING

    public AndroidMicrophone(int sampleRate, int bufferSize) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
    }

    public void open() {
        try {
            // Get minimum buffer size
            Class<?> audioRecordClass = Class.forName("android.media.AudioRecord");
            minBufferSize = (int) audioRecordClass.getMethod("getMinBufferSize", int.class, int.class, int.class)
                    .invoke(null, sampleRate, CHANNEL_IN_MONO, ENCODING_PCM_16BIT);

            if (minBufferSize <= 0) {
                throw new IllegalStateException("Invalid min buffer size: " + minBufferSize);
            }

            // Use larger buffer to avoid underruns
            int actualBufferSize = Math.max(minBufferSize, bufferSize * 2 * 2); // bufferSize samples * 2 bytes * 2

            // Create AudioRecord instance
            audioRecord = audioRecordClass.getConstructor(int.class, int.class, int.class, int.class, int.class)
                    .newInstance(SOURCE_MIC, sampleRate, CHANNEL_IN_MONO, ENCODING_PCM_16BIT, actualBufferSize);

            // Check if initialized properly
            int state = (int) audioRecordClass.getMethod("getState").invoke(audioRecord);
            if (state != STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord not initialized. State: " + state);
            }

            Log.info("@ Microphone opened (Android)", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to open microphone: @", TAG, e.getMessage());
            throw new RuntimeException("Failed to open Android microphone", e);
        }
    }

    public void start() {
        if (audioRecord == null)
            return;
        try {
            audioRecord.getClass().getMethod("startRecording").invoke(audioRecord);
            isRecording = true;
            Log.info("@ Recording started", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to start recording: @", TAG, e.getMessage());
        }
    }

    public void stop() {
        if (audioRecord == null)
            return;
        try {
            audioRecord.getClass().getMethod("stop").invoke(audioRecord);
            isRecording = false;
            Log.info("@ Recording stopped", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to stop recording: @", TAG, e.getMessage());
        }
    }

    public void close() {
        if (audioRecord == null)
            return;
        try {
            stop();
            audioRecord.getClass().getMethod("release").invoke(audioRecord);
            audioRecord = null;
            Log.info("@ Microphone closed", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to close microphone: @", TAG, e.getMessage());
        }
    }

    public boolean isOpen() {
        if (audioRecord == null)
            return false;
        try {
            int state = (int) audioRecord.getClass().getMethod("getState").invoke(audioRecord);
            return state == STATE_INITIALIZED;
        } catch (Exception e) {
            return false;
        }
    }

    public int available() {
        // Android AudioRecord doesn't have an "available" method like javax.sound
        // Return buffer size if recording
        return isRecording ? bufferSize : 0;
    }

    public short[] read() {
        if (audioRecord == null || !isRecording) {
            throw new IllegalStateException("Microphone is not recording");
        }

        try {
            short[] buffer = new short[bufferSize];
            int samplesRead = (int) audioRecord.getClass()
                    .getMethod("read", short[].class, int.class, int.class)
                    .invoke(audioRecord, buffer, 0, bufferSize);

            if (samplesRead < 0) {
                Log.err("@ Read error: @", TAG, samplesRead);
                return new short[0];
            }

            if (samplesRead < bufferSize) {
                // Return only what we got
                short[] result = new short[samplesRead];
                System.arraycopy(buffer, 0, result, 0, samplesRead);
                return result;
            }

            return buffer;
        } catch (Exception e) {
            Log.err("@ Failed to read audio: @", TAG, e.getMessage());
            return new short[0];
        }
    }
}
