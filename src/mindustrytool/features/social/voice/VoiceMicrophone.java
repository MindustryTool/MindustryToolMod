package mindustrytool.features.social.voice;

import arc.Core;
import arc.util.Log;
import arc.util.Nullable;

/**
 * Cross-platform voice microphone interface.
 * Automatically detects platform and loads correct implementation:
 * - Desktop: DesktopMicrophone (javax.sound)
 * - Android: AndroidMicrophone (android.media.AudioRecord)
 */
public class VoiceMicrophone {

    private static final String TAG = "[VoiceMic]";

    @Nullable
    private Object platformMic; // Platform-specific microphone implementation
    private final int sampleRate;
    private final int bufferSize;
    private boolean isOpen = false;
    private boolean isRecording = false;
    private final boolean isDesktop;

    public VoiceMicrophone() {
        this.sampleRate = VoiceConstants.SAMPLE_RATE;
        this.bufferSize = VoiceConstants.BUFFER_SIZE;
        this.isDesktop = !Core.app.isMobile();
    }

    public void open() {
        if (isOpen) {
            throw new IllegalStateException("Microphone is already open");
        }

        try {
            if (isDesktop) {
                // Load Desktop implementation
                Class<?> implClass = Class.forName("mindustrytool.features.social.voice.DesktopMicrophone");
                platformMic = implClass.getDeclaredConstructor(int.class, int.class).newInstance(sampleRate,
                        bufferSize);
                implClass.getMethod("open").invoke(platformMic);
                Log.info("@ Microphone opened (Desktop)", TAG);
            } else {
                // Load Android implementation
                Class<?> implClass = Class.forName("mindustrytool.features.social.voice.AndroidMicrophone");
                platformMic = implClass.getDeclaredConstructor(int.class, int.class).newInstance(sampleRate,
                        bufferSize);
                implClass.getMethod("open").invoke(platformMic);
                Log.info("@ Microphone opened (Android)", TAG);
            }
            isOpen = true;
        } catch (Exception e) {
            Log.err("@ Failed to open microphone: @", TAG, e.getMessage());
            throw new IllegalStateException("Failed to open microphone", e);
        }
    }

    public void start() {
        if (!isOpen || platformMic == null)
            return;
        try {
            platformMic.getClass().getMethod("start").invoke(platformMic);
            isRecording = true;
        } catch (Exception e) {
            Log.err("@ Failed to start recording: @", TAG, e.getMessage());
        }
    }

    public void stop() {
        if (!isOpen || platformMic == null)
            return;
        try {
            platformMic.getClass().getMethod("stop").invoke(platformMic);
            isRecording = false;
        } catch (Exception e) {
            Log.err("@ Failed to stop recording: @", TAG, e.getMessage());
        }
    }

    public void close() {
        if (platformMic == null || !isOpen)
            return;

        try {
            platformMic.getClass().getMethod("close").invoke(platformMic);
        } catch (Exception e) {
            Log.err("@ Failed to close microphone: @", TAG, e.getMessage());
        }

        isRecording = false;
        isOpen = false;
        platformMic = null;
        Log.info("@ Microphone closed", TAG);
    }

    public boolean isOpen() {
        return isOpen && platformMic != null;
    }

    public int available() {
        if (!isRecording || platformMic == null)
            return 0;
        try {
            return (int) platformMic.getClass().getMethod("available").invoke(platformMic);
        } catch (Exception e) {
            return 0;
        }
    }

    public short[] read() {
        if (platformMic == null) {
            throw new IllegalStateException("Microphone is not opened");
        }
        if (!isRecording) {
            throw new IllegalStateException("Microphone is not recording");
        }
        try {
            return (short[]) platformMic.getClass().getMethod("read").invoke(platformMic);
        } catch (Exception e) {
            Log.err("@ Failed to read audio: @", TAG, e.getMessage());
            return new short[0];
        }
    }

    public boolean isAvailable() {
        return true; // Now available on both platforms
    }
}
