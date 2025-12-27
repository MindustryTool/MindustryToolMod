package mindustrytool.plugins.voicechat;

import arc.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * 120ms-Aware Audio Mixer for multi-speaker support.
 * 
 * Design rationale:
 * - Packets arrive every 120ms (2 x 60ms frames batched together)
 * - Each packet results in 2 play() calls (one per frame)
 * - Mixer accumulates ALL frames from ALL players within a 120ms window
 * - At end of window, mixes accumulated frames and plays result
 * 
 * This prevents interleaving (ABAB) and produces proper mixing (A+B).
 */
public class AudioMixer {

    private static final String TAG = "[AudioMixer]";

    // Match packet timing: 2 frames x 60ms = 120ms
    private static final int MIX_INTERVAL_MS = 120;
    private static final int FRAME_SIZE = VoiceConstants.BUFFER_SIZE; // 2880 samples (60ms)
    private static final int FRAMES_PER_WINDOW = 2; // 2 frames per 120ms window
    private static final int WINDOW_SAMPLES = FRAME_SIZE * FRAMES_PER_WINDOW; // 5760 samples (120ms)

    // Per-player frame accumulator
    private final ConcurrentHashMap<String, List<short[]>> playerFrames = new ConcurrentHashMap<>();

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

        mixerThread = new Thread(this::mixLoop, "VoiceChat-Mixer120");
        mixerThread.setDaemon(true);
        mixerThread.start();

        Log.info("@ 120ms Mixer started", TAG);
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
        playerFrames.clear();
        Log.info("@ Mixer stopped", TAG);
    }

    /**
     * Queue decoded audio for a specific player.
     * Frames are accumulated until next mix window.
     */
    public void queueAudio(String playerId, short[] audioData) {
        List<short[]> frames = playerFrames.computeIfAbsent(
                playerId,
                k -> new ArrayList<>(4));

        synchronized (frames) {
            frames.add(audioData);
        }
    }

    /**
     * Remove a player's buffer.
     */
    public void removePlayer(String playerId) {
        playerFrames.remove(playerId);
    }

    /**
     * Main mixer loop - runs every 120ms to match packet timing.
     */
    private void mixLoop() {
        while (running) {
            try {
                Thread.sleep(MIX_INTERVAL_MS);
                mixAndPlay();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.err("@ Mix error: @", TAG, e.getMessage());
            }
        }
    }

    /**
     * Mix all accumulated frames and play.
     */
    private void mixAndPlay() {
        // Collect all frames from all players
        List<short[]> allFrames = new ArrayList<>();

        for (List<short[]> playerFrameList : playerFrames.values()) {
            synchronized (playerFrameList) {
                allFrames.addAll(playerFrameList);
                playerFrameList.clear();
            }
        }

        // No audio received
        if (allFrames.isEmpty())
            return;

        // If only one source, play directly (no mixing needed)
        if (allFrames.size() == 1) {
            speaker.play(allFrames.get(0));
            return;
        }

        // Multiple sources: mix all frames
        // First, determine output length (longest frame)
        int maxLength = 0;
        for (short[] frame : allFrames) {
            if (frame.length > maxLength)
                maxLength = frame.length;
        }

        // Mix using int[] to prevent overflow
        int[] mixed = new int[maxLength];
        for (short[] frame : allFrames) {
            for (int i = 0; i < frame.length; i++) {
                mixed[i] += frame[i];
            }
        }

        // Convert back to short[] with soft clipping
        short[] output = new short[maxLength];
        for (int i = 0; i < maxLength; i++) {
            output[i] = softClip(mixed[i]);
        }

        speaker.play(output);
    }

    /**
     * Soft clip to prevent distortion when mixing.
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
}
