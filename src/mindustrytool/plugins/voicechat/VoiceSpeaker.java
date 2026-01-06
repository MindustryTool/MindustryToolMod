package mindustrytool.plugins.voicechat;

import arc.Core;
import arc.util.Log;
import arc.util.Nullable;

/**
 * Cross-platform voice speaker interface.
 * Automatically detects platform and loads correct implementation:
 * - Desktop: DesktopSpeaker (javax.sound)
 * - Android: AndroidSpeaker (android.media.AudioTrack)
 */
public class VoiceSpeaker {

    private static final String TAG = "[VoiceSpk]";

    @Nullable
    private Object platformSpeaker; // Platform-specific speaker implementation
    private final int sampleRate;
    private float volume = 0.8f;
    private boolean isOpen = false;
    private final boolean isDesktop;

    public VoiceSpeaker() {
        this.sampleRate = VoiceConstants.SAMPLE_RATE;
        this.isDesktop = !Core.app.isMobile();
        open();
    }

    public void open() {
        if (isOpen)
            return;

        try {
            if (isDesktop) {
                // Load Desktop implementation
                Class<?> implClass = Class.forName("mindustrytool.plugins.voicechat.DesktopSpeaker");
                platformSpeaker = implClass.getDeclaredConstructor(int.class).newInstance(sampleRate);
                implClass.getMethod("open").invoke(platformSpeaker);
                Log.info("@ Speaker opened (Desktop)", TAG);
            } else {
                // Load Android implementation
                Class<?> implClass = Class.forName("mindustrytool.plugins.voicechat.AndroidSpeaker");
                platformSpeaker = implClass.getDeclaredConstructor(int.class).newInstance(sampleRate);
                implClass.getMethod("open").invoke(platformSpeaker);
                Log.info("@ Speaker opened (Android)", TAG);
            }
            isOpen = true;
        } catch (Exception e) {
            Log.err("@ Failed to open speaker: @", TAG, e.getMessage());
        }
    }

    public void play(short[] audioData) {
        if (platformSpeaker == null || !isOpen)
            return;

        // Apply volume
        short[] adjustedData = new short[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            adjustedData[i] = (short) (audioData[i] * volume);
        }

        try {
            platformSpeaker.getClass().getMethod("play", short[].class).invoke(platformSpeaker, adjustedData);
        } catch (Exception e) {
            Log.err("@ Failed to play audio: @", TAG, e.getMessage());
        }
    }

    public boolean isOpen() {
        return isOpen && platformSpeaker != null;
    }

    public void close() {
        if (platformSpeaker == null)
            return;
        try {
            platformSpeaker.getClass().getMethod("close").invoke(platformSpeaker);
        } catch (Exception e) {
            Log.err("@ Failed to close speaker: @", TAG, e.getMessage());
        }
        isOpen = false;
        platformSpeaker = null;
        Log.info("@ Speaker closed", TAG);
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
    }

    public boolean isAvailable() {
        return true; // Now available on both platforms
    }

    /**
     * Set the audio mixer (Desktop only).
     * Used for Pull-based architecture.
     */
    public void setMixer(AudioMixer mixer) {
        if (platformSpeaker == null)
            return;

        try {
            // Available on both DesktopSpeaker and AndroidSpeaker
            platformSpeaker.getClass().getMethod("setMixer", AudioMixer.class).invoke(platformSpeaker, mixer);
        } catch (Exception e) {
            Log.err("@ Failed to set mixer: @", TAG, e.getMessage());
        }
    }
}
