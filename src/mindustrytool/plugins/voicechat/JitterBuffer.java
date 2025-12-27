package mindustrytool.plugins.voicechat;

import java.util.LinkedList;

/**
 * Per-player Jitter Buffer for stabilizing incoming audio streams.
 * 
 * Based on WebRTC/Discord approach:
 * 1. Buffer incoming frames with arrival timestamps
 * 2. Reorder if needed (not implemented yet - TCP ensures order)
 * 3. Output stabilized frames at consistent rate
 * 4. Handle late/missing frames gracefully
 * 
 * This allows mixing multiple streams without timing conflicts.
 */
public class JitterBuffer {

    private static final int TARGET_BUFFER_MS = 80; // Target buffer size in ms
    private static final int FRAME_DURATION_MS = 60; // Each frame is 60ms
    private static final int MAX_BUFFER_FRAMES = 4; // Max frames to buffer (~240ms)

    // Frame with timestamp
    private static class TimestampedFrame {
        final short[] samples;
        final long arrivalTime;

        TimestampedFrame(short[] samples, long arrivalTime) {
            this.samples = samples;
            this.arrivalTime = arrivalTime;
        }
    }

    private final LinkedList<TimestampedFrame> buffer = new LinkedList<>();
    private long lastOutputTime = 0;
    private boolean primed = false; // Has buffer reached target size?

    /**
     * Add a decoded audio frame to the buffer.
     */
    public synchronized void push(short[] samples) {
        long now = System.currentTimeMillis();

        // If buffer is full, drop oldest frame
        while (buffer.size() >= MAX_BUFFER_FRAMES) {
            buffer.pollFirst();
        }

        buffer.addLast(new TimestampedFrame(samples, now));

        // Prime the buffer: wait until we have enough frames
        if (!primed && buffer.size() >= 2) {
            primed = true;
            lastOutputTime = now;
        }
    }

    /**
     * Get the next stabilized frame for playback.
     * Returns null if buffer is empty or not ready yet.
     * 
     * Call this at a consistent rate (e.g., every 60ms).
     */
    /**
     * Get the next stabilized frame for playback.
     * Returns null if buffer is empty or not ready yet.
     * 
     * Call this at a consistent rate (e.g., every 60ms).
     */
    public synchronized short[] pop() {
        // Not primed yet - wait for more frames
        if (!primed || buffer.isEmpty()) {
            return null;
        }

        // Return the oldest frame immediately (Mixer thread handles timing)
        TimestampedFrame frame = buffer.pollFirst();
        if (frame != null) {
            return frame.samples;
        }

        return null;
    }

    /**
     * Check if buffer has frames ready for output.
     */
    public synchronized boolean hasFrames() {
        return primed && !buffer.isEmpty();
    }

    /**
     * Get current buffer size in frames.
     */
    public synchronized int size() {
        return buffer.size();
    }

    /**
     * Clear the buffer and reset state.
     */
    public synchronized void clear() {
        buffer.clear();
        primed = false;
        lastOutputTime = 0;
    }

    /**
     * Check if buffer is primed (has received enough frames to start output).
     */
    public synchronized boolean isPrimed() {
        return primed;
    }
}
