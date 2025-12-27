package mindustrytool.plugins.voicechat;

import arc.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Audio Mixer with per-player Jitter Buffers (Pull-Based).
 * 
 * Architecture:
 * - Passive component (no thread).
 * - Driven by the Audio Output thread (DesktopSpeaker).
 * - Provides 'mixOneChunk()' which returns mixed audio or silence.
 * 
 * This ensures perfect synchronization with hardware clock, preventing
 * drift/looping.
 */
public class AudioMixer {

    private static final String TAG = "[AudioMixer]";

    // Per-player jitter buffers
    private final ConcurrentHashMap<String, JitterBuffer> playerBuffers = new ConcurrentHashMap<>();

    public AudioMixer() {
        Log.info("@ Mixer created (Pull-based)", TAG);
    }

    /**
     * Queue decoded audio for a specific player.
     * Audio is added to that player's jitter buffer.
     */
    public void queueAudio(String playerId, short[] audioData) {
        JitterBuffer buffer = playerBuffers.computeIfAbsent(
                playerId,
                k -> new JitterBuffer());
        buffer.push(audioData);
    }

    /**
     * Remove a player's buffer.
     */
    public void removePlayer(String playerId) {
        JitterBuffer removed = playerBuffers.remove(playerId);
        if (removed != null) {
            removed.clear();
        }
    }

    /**
     * Pull one chunk of mixed audio (typically 60ms).
     * Returns null if no audio is available.
     */
    public short[] mixOneChunk() {
        // Collect ready frames from all player jitter buffers
        List<short[]> readyFrames = new ArrayList<>();

        for (JitterBuffer buffer : playerBuffers.values()) {
            short[] frame = buffer.pop();
            if (frame != null && frame.length > 0) {
                readyFrames.add(frame);
            }
        }

        // No audio ready from any player
        if (readyFrames.isEmpty())
            return null;

        // Single player: return directly (no mixing needed)
        if (readyFrames.size() == 1) {
            return readyFrames.get(0);
        }

        // Multiple players: mix
        return mixFrames(readyFrames);
    }

    /**
     * Mix multiple audio frames into one.
     */
    private short[] mixFrames(List<short[]> frames) {
        // Find longest frame
        int maxLength = 0;
        for (short[] frame : frames) {
            if (frame.length > maxLength)
                maxLength = frame.length;

            // Safety cap: don't mix huge frames
            if (maxLength > VoiceConstants.BUFFER_SIZE * 2)
                maxLength = VoiceConstants.BUFFER_SIZE * 2;
        }

        // Sum all samples (using int to prevent overflow)
        int[] mixed = new int[maxLength];
        for (short[] frame : frames) {
            for (int i = 0; i < Math.min(frame.length, maxLength); i++) {
                mixed[i] += frame[i];
            }
        }

        // Convert back with soft clipping
        short[] output = new short[maxLength];
        for (int i = 0; i < maxLength; i++) {
            output[i] = softClip(mixed[i]);
        }

        return output;
    }

    /**
     * Soft clip to prevent distortion.
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
}
