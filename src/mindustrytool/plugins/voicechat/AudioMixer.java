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

    private static final float PAN_RANGE = 400f; // Range for full pan (in world units)

    // Spatial State
    private float listenerX, listenerY;
    private final ConcurrentHashMap<String, arc.math.geom.Vec2> playerPositions = new ConcurrentHashMap<>();

    // Reusable buffers to avoid allocation
    // Stereo output is 2x frame size
    // Using int for mixing to avoid overflow before clipping
    private int[] mixBufferL;
    private int[] mixBufferR;

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

            // Calculate Pan
            float pan = 0f; // -1 to 1
            if (playerPositions.containsKey(pid)) {
                arc.math.geom.Vec2 pos = playerPositions.get(pid);
                float dx = pos.x - listenerX;
                pan = arc.math.Mathf.clamp(dx / PAN_RANGE, -1f, 1f);
            }

            // Constant Power Pan Laws
            // L = cos((pan + 1) * PI / 4)
            // R = sin((pan + 1) * PI / 4)
            // Simplified Linear for speed:
            float gainL = 1.0f - (pan + 1.0f) / 2.0f; // 1 at -1, 0 at 1
            float gainR = (pan + 1.0f) / 2.0f; // 0 at -1, 1 at 1

            // Adjust loop for Volume
            float vol = (volumeMap != null) ? volumeMap.get(pid, 1f) : 1f;
            gainL *= vol;
            gainR *= vol;

            for (int s = 0; s < Math.min(frame.length, frameSize); s++) {
                short sample = frame[s];
                mixBufferL[s] += (int) (sample * gainL);
                mixBufferR[s] += (int) (sample * gainR);
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
    private short[] mixFrames(List<short[]> frames) {
        return new short[0];
    }

    /**
     * Soft clip to prevent distortion.
     */
    private short softClip(int sample) {
        if (sample >= Short.MAX_VALUE)
            return Short.MAX_VALUE;
        if (sample <= Short.MIN_VALUE)
            return Short.MIN_VALUE;

        final int threshold = 20000; // Lower threshold slightly for safer mixing
        if (Math.abs(sample) > threshold) {
            double normalized = sample / (double) Short.MAX_VALUE;
            double compressed = Math.tanh(normalized * 1.5) / Math.tanh(1.5);
            return (short) (compressed * Short.MAX_VALUE);
        }
        return (short) sample;
    }
}
