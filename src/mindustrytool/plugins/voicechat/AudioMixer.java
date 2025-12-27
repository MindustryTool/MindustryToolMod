package mindustrytool.plugins.voicechat;

import arc.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Discord-style Audio Mixer for multi-stream playback.
 * Combines audio from multiple players into a single output stream.
 * 
 * Benefits:
 * - No speaker buffer contention when multiple people speak
 * - Consistent timing through central mixer thread
 * - Clipping protection to prevent distortion
 */
public class AudioMixer {

    private static final String TAG = "[AudioMixer]";
    private static final int MIX_INTERVAL_MS = 60; // Mix every 60ms (matches frame size)
    private static final int BUFFER_CAPACITY = 8; // Frames per player buffer
    private static final int FRAME_SIZE = VoiceConstants.BUFFER_SIZE; // Samples per frame

    // Per-player audio buffers
    private final ConcurrentHashMap<String, LinkedBlockingQueue<short[]>> playerBuffers = new ConcurrentHashMap<>();

    // Output speaker
    private final VoiceSpeaker speaker;

    // Mixer thread
    private Thread mixerThread;
    private volatile boolean running = false;

    public AudioMixer(VoiceSpeaker speaker) {
        this.speaker = speaker;
    }

    /**
     * Start the mixer thread.
     */
    public void start() {
        if (running)
            return;
        running = true;

        mixerThread = new Thread(this::mixLoop, "VoiceChat-Mixer");
        mixerThread.setDaemon(true);
        mixerThread.start();

        Log.info("@ Mixer started", TAG);
    }

    /**
     * Stop the mixer thread.
     */
    public void stop() {
        running = false;
        if (mixerThread != null) {
            mixerThread.interrupt();
            mixerThread = null;
        }
        playerBuffers.clear();
        Log.info("@ Mixer stopped", TAG);
    }

    /**
     * Queue decoded audio for a specific player.
     * Called by VoiceChatManager when receiving voice packets.
     */
    public void queueAudio(String playerId, short[] audioData) {
        LinkedBlockingQueue<short[]> buffer = playerBuffers.computeIfAbsent(
                playerId,
                k -> new LinkedBlockingQueue<>(BUFFER_CAPACITY));

        // If buffer is full, drop oldest frame to prevent latency buildup
        if (!buffer.offer(audioData)) {
            buffer.poll();
            buffer.offer(audioData);
        }
    }

    /**
     * Remove a player's buffer (when they disconnect or stop speaking).
     */
    public void removePlayer(String playerId) {
        playerBuffers.remove(playerId);
    }

    /**
     * Main mixer loop - runs every MIX_INTERVAL_MS.
     * Reads from all player buffers, mixes, and plays.
     */
    private void mixLoop() {
        long lastMixTime = System.currentTimeMillis();

        while (running) {
            try {
                // Wait for next mix interval
                long now = System.currentTimeMillis();
                long elapsed = now - lastMixTime;
                if (elapsed < MIX_INTERVAL_MS) {
                    Thread.sleep(MIX_INTERVAL_MS - elapsed);
                }
                lastMixTime = System.currentTimeMillis();

                // Collect frames from all players
                short[][] frames = new short[playerBuffers.size()][];
                int frameCount = 0;

                for (LinkedBlockingQueue<short[]> buffer : playerBuffers.values()) {
                    short[] frame = buffer.poll();
                    if (frame != null && frame.length > 0) {
                        frames[frameCount++] = frame;
                    }
                }

                // If no audio, skip
                if (frameCount == 0)
                    continue;

                // Mix all frames into one
                short[] mixed = mixFrames(frames, frameCount);

                // Play mixed audio
                if (speaker != null && speaker.isOpen()) {
                    speaker.play(mixed);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.err("@ Mix error: @", TAG, e.getMessage());
            }
        }
    }

    /**
     * Mix multiple audio frames into one.
     * Uses simple sum with soft clipping to prevent distortion.
     */
    private short[] mixFrames(short[][] frames, int count) {
        if (count == 0)
            return new short[0];
        if (count == 1)
            return frames[0];

        // Find the longest frame
        int maxLength = 0;
        for (int i = 0; i < count; i++) {
            if (frames[i] != null && frames[i].length > maxLength) {
                maxLength = frames[i].length;
            }
        }

        short[] result = new short[maxLength];

        for (int i = 0; i < maxLength; i++) {
            // Sum all samples at this position
            int sum = 0;
            for (int j = 0; j < count; j++) {
                if (frames[j] != null && i < frames[j].length) {
                    sum += frames[j][i];
                }
            }

            // Soft clipping to prevent distortion
            result[i] = softClip(sum);
        }

        return result;
    }

    /**
     * Soft clip to prevent harsh distortion when mixing.
     * Uses tanh-like curve for smooth limiting.
     */
    private short softClip(int sample) {
        // Hard limits
        if (sample >= Short.MAX_VALUE)
            return Short.MAX_VALUE;
        if (sample <= Short.MIN_VALUE)
            return Short.MIN_VALUE;

        // For values within range, apply mild compression if approaching limits
        final int threshold = 24000; // Start compressing near this level
        if (Math.abs(sample) > threshold) {
            // Simple soft knee compression
            double normalized = sample / (double) Short.MAX_VALUE;
            double compressed = Math.tanh(normalized * 1.5) / Math.tanh(1.5);
            return (short) (compressed * Short.MAX_VALUE);
        }

        return (short) sample;
    }

    /**
     * Check if mixer is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get number of active player buffers.
     */
    public int getActivePlayerCount() {
        return playerBuffers.size();
    }
}
