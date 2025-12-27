package mindustrytool.plugins.voicechat;

import arc.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * Audio Mixer with per-player Jitter Buffers.
 * 
 * Architecture (based on WebRTC/Discord):
 * 1. Each player has their own JitterBuffer
 * 2. JitterBuffer stabilizes incoming frames (handles network jitter)
 * 3. Mixer pulls stabilized frames from all buffers at consistent rate
 * 4. Mixes all ready frames and plays result
 * 
 * This fixes the timing issues with previous fixed-interval approaches.
 */
public class AudioMixer {

    private static final String TAG = "[AudioMixer]";
    private static final int OUTPUT_INTERVAL_MS = 60; // Output every 60ms (matches frame size)

    // Per-player jitter buffers
    private final ConcurrentHashMap<String, JitterBuffer> playerBuffers = new ConcurrentHashMap<>();

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

        mixerThread = new Thread(this::mixLoop, "VoiceChat-JitterMixer");
        mixerThread.setDaemon(true);
        mixerThread.start();

        Log.info("@ Jitter Buffer Mixer started", TAG);
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
     * Main mixer loop - runs every OUTPUT_INTERVAL_MS.
     * Pulls stabilized frames from all jitter buffers and mixes.
     */
    private void mixLoop() {
        long lastOutputTime = System.currentTimeMillis();

        while (running) {
            try {
                // Sleep to maintain consistent output rate
                long now = System.currentTimeMillis();
                long elapsed = now - lastOutputTime;
                long sleepTime = OUTPUT_INTERVAL_MS - elapsed;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
                lastOutputTime = System.currentTimeMillis();

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
                    continue;

                // Single player: play directly (no mixing needed)
                if (readyFrames.size() == 1) {
                    speaker.play(readyFrames.get(0));
                    continue;
                }

                // Multiple players: mix and play
                short[] mixed = mixFrames(readyFrames);
                speaker.play(mixed);

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
     */
    private short[] mixFrames(List<short[]> frames) {
        // Find longest frame
        int maxLength = 0;
        for (short[] frame : frames) {
            if (frame.length > maxLength)
                maxLength = frame.length;
        }

        // Sum all samples (using int to prevent overflow)
        int[] mixed = new int[maxLength];
        for (short[] frame : frames) {
            for (int i = 0; i < frame.length; i++) {
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
