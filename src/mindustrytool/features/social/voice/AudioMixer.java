package mindustrytool.features.social.voice;

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
    private final ConcurrentHashMap<String, Float> playerVolume = new ConcurrentHashMap<>();

    public AudioMixer() {
        Log.info("@ Mixer created (Pull-based + PLC)", TAG);
    }

    public void setProcessor(VoiceProcessor processor) {
        this.processor = processor;
    }

    public void setVolume(String playerId, float volume) {
        playerVolume.put(playerId, volume);
    }

    /**
     * Queue a new audio packet for a specific player.
     * 
     * @param playerId Unique ID of sender
     * @param data     Encoded audio frame
     * @param sequence Packet sequence number for reordering
     */
    public void queueAudio(String playerId, byte[] data, int sequence) {
        playerBuffers.computeIfAbsent(playerId, k -> new JitterBuffer()).push(data, sequence);
        lastActiveTime.put(playerId, Time.millis()); // Update for PLC timeout
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

    private static final float PAN_RANGE = 400f; // 50 tiles (Stereo width)
    private static final float MAX_DISTANCE = 1600f; // 200 tiles (Hearing range)

    // Spatial State
    private float listenerX, listenerY;
    private final ConcurrentHashMap<String, arc.math.geom.Vec2> playerPositions = new ConcurrentHashMap<>();

    // Reusable buffers to avoid allocation
    // Stereo output is 2x frame size
    // Using int for mixing to avoid overflow before clipping
    // Stereo output is 2x frame size
    // Using int for mixing to avoid overflow before clipping
    private int[] mixBufferL;
    private int[] mixBufferR;

    private boolean spatialEnabled = true;

    public void setSpatialEnabled(boolean enabled) {
        this.spatialEnabled = enabled;
    }

    public void updateListener(float x, float y) {
        this.listenerX = x;
        this.listenerY = y;
    }

    public void updatePosition(String playerId, float x, float y) {
        playerPositions.computeIfAbsent(playerId, k -> new arc.math.geom.Vec2()).set(x, y);
    }

    /**
     * Pull one chunk of mixed audio (Stereo Interleaved).
     * Returns null if no audio is available.
     */
    public short[] mixOneChunk() {
        if (processor == null)
            return null;

        List<short[]> activeFrames = new ArrayList<>();
        List<String> activeIds = new ArrayList<>();
        long now = Time.millis();

        // 1. Gather all active frames
        // Iterate keys to handle PLC even if buffer empty
        for (String playerId : playerBuffers.keySet()) {
            JitterBuffer buffer = playerBuffers.get(playerId);
            if (buffer == null)
                continue;

            byte[] encoded = buffer.pop();
            short[] decoded = null;

            if (encoded != null) {
                decoded = processor.decode(playerId, encoded);
                lastActiveTime.put(playerId, now);
            } else {
                Long lastActive = lastActiveTime.get(playerId);
                if (lastActive != null && now - lastActive < PLC_TIMEOUT_MS) {
                    decoded = processor.decodePLC(playerId);
                }
            }

            if (decoded != null && decoded.length > 0) {
                activeFrames.add(decoded);
                activeIds.add(playerId);
            }
        }

        if (activeFrames.isEmpty())
            return null;

        // 2. Prepare Mix Buffers
        int frameSize = activeFrames.get(0).length;
        if (mixBufferL == null || mixBufferL.length != frameSize) {
            mixBufferL = new int[frameSize];
            mixBufferR = new int[frameSize];
        } else {
            java.util.Arrays.fill(mixBufferL, 0);
            java.util.Arrays.fill(mixBufferR, 0);
        }

        // 3. Mix into L/R with Panning
        for (int i = 0; i < activeFrames.size(); i++) {
            short[] frame = activeFrames.get(i);
            String pid = activeIds.get(i);

            // Calculate Pan (-1 to 1) and Distance
            float pan = 0f;
            float distVol = 1f;

            if (spatialEnabled && playerPositions.containsKey(pid)) {
                arc.math.geom.Vec2 pos = playerPositions.get(pid);
                float dx = pos.x - listenerX;
                float dy = pos.y - listenerY;

                // Panning based on X delta (using PAN_RANGE)
                pan = arc.math.Mathf.clamp(dx / PAN_RANGE, -1f, 1f);

                // Distance Attenuation (Linear dropoff using MAX_DISTANCE)
                float dist = (float) Math.hypot(dx, dy);

                // Clamp minimum volume to 0.5 (50%) at max distance
                distVol = arc.math.Mathf.clamp(1f - (dist / MAX_DISTANCE), 0.5f, 1f);
            }

            // Constant Power Pan Laws (Square Root)
            // Maintains consistent volume level across the stereo field
            float gainL = (float) Math.sqrt((1.0f - pan) * 0.5f);
            float gainR = (float) Math.sqrt((1.0f + pan) * 0.5f);

            // Apply Master Volume + Distance Volume
            float masterVol = playerVolume.getOrDefault(pid, 1f);
            float totalGainL = gainL * masterVol * distVol;
            float totalGainR = gainR * masterVol * distVol;

            for (int s = 0; s < Math.min(frame.length, frameSize); s++) {
                short sample = frame[s];
                mixBufferL[s] += (int) (sample * totalGainL);
                mixBufferR[s] += (int) (sample * totalGainR);
            }
        }

        // 4. Interleave and Clip to Stereo Output
        short[] stereoOut = new short[frameSize * 2];
        for (int i = 0; i < frameSize; i++) {
            stereoOut[i * 2] = softClip(mixBufferL[i]); // Left
            stereoOut[i * 2 + 1] = softClip(mixBufferR[i]); // Right
        }

        return stereoOut;
    }

    /**
     * Mix multiple audio frames into one.
     * DEPRECATED: Replaced by inline stereo mixing loop above.
     */
    @SuppressWarnings("unused") // Method kept for potential mono fallback reference
    private short[] mixFrames(List<short[]> frames) {
        return new short[0];
    }

    /**
     * Soft clip to prevent distortion.
     */
    private short softClip(int sample) {
        // Tanh Limiter (Dynamic Range Compression)
        // Handles huge inputs (Multi-speaker sums) without hard clipping or overflow.
        // Formula: output = MAX_VALUE * tanh(input / MAX_VALUE)
        // Since tanh approaches 1.0 asymptotically, output approaching MAX_VALUE but
        // never exceeds it.

        // Fast path for normal volumes
        if (sample >= -20000 && sample <= 20000) {
            return (short) sample;
        }

        // Limiter for loud sums
        double normalized = sample / 32768.0;
        double limited = Math.tanh(normalized);
        return (short) (limited * 32767.0);
    }
}
