package mindustrytool.plugins.voicechat;

import arc.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;
import arc.util.Time;

/**
 * Audio Mixer with PLC-enabled Jitter Buffers (Pull-Based).
 * 
 * Architecture:
 * - Stores encoded Opus packets in JitterBuffers.
 * - Decodes on-demand during mixing (PLC applied if packet missing).
 * - Driven by DesktopSpeaker thread.
 */
public class AudioMixer {

    private static final String TAG = "[AudioMixer]";

    // Per-player jitter buffers
    private final ConcurrentHashMap<String, JitterBuffer> playerBuffers = new ConcurrentHashMap<>();

    // Track last activity to know when to trigger PLC vs Silence
    private final ConcurrentHashMap<String, Long> lastActiveTime = new ConcurrentHashMap<>();
    private static final long PLC_TIMEOUT_MS = 200; // Stop PLC after 200ms of silence

    private VoiceProcessor processor;
    private arc.struct.ObjectMap<String, Float> volumeMap;

    public AudioMixer() {
        Log.info("@ Mixer created (Pull-based + PLC)", TAG);
    }

    public void setProcessor(VoiceProcessor processor) {
        this.processor = processor;
    }

    public void setVolumeMap(arc.struct.ObjectMap<String, Float> volumeMap) {
        this.volumeMap = volumeMap;
    }

    /**
     * Queue encoded Opus frame for a specific player.
     */
    public void queueAudio(String playerId, byte[] opusData) {
        JitterBuffer buffer = playerBuffers.computeIfAbsent(
                playerId,
                k -> new JitterBuffer());
        buffer.push(opusData);
        lastActiveTime.put(playerId, Time.millis());
    }

    /**
     * Remove a player's buffer.
     */
    public void removePlayer(String playerId) {
        if ("all".equals(playerId)) {
            playerBuffers.clear();
            lastActiveTime.clear();
            return;
        }

        JitterBuffer removed = playerBuffers.remove(playerId);
        if (removed != null) {
            removed.clear();
        }
        lastActiveTime.remove(playerId);
    }

    /**
     * Pull one chunk of mixed audio (typically 60ms).
     * Returns null if no audio is available.
     */
    public short[] mixOneChunk() {
        if (processor == null)
            return null;

        List<short[]> readyFrames = new ArrayList<>();
        long now = Time.millis();

        // Iterate keys to handle PLC even if buffer empty
        for (String playerId : playerBuffers.keySet()) {
            JitterBuffer buffer = playerBuffers.get(playerId);
            if (buffer == null)
                continue;

            byte[] encoded = buffer.pop();
            short[] decoded = null;

            if (encoded != null) {
                // Packet available - decode normally
                decoded = processor.decode(playerId, encoded);
                lastActiveTime.put(playerId, now);
            } else {
                // Packet missing - check if we should do PLC
                Long lastActive = lastActiveTime.get(playerId);
                if (lastActive != null && now - lastActive < PLC_TIMEOUT_MS) {
                    // Generate PLC frame
                    decoded = processor.decodePLC(playerId);
                }
            }

            if (decoded != null && decoded.length > 0) {
                // Apply Volume (if map available)
                if (volumeMap != null) {
                    float vol = volumeMap.get(playerId, 1f);
                    if (vol != 1f) {
                        for (int i = 0; i < decoded.length; i++) {
                            decoded[i] = (short) (decoded[i] * vol);
                        }
                    }
                }

                readyFrames.add(decoded);
            }
        }

        // No audio ready from any player
        if (readyFrames.isEmpty())
            return null;

        // Single player: return directly
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
