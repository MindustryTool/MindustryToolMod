package mindustrytool.features.social.voice;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Adaptive Jitter Buffer for UDP Audio Stream.
 * Features:
 * 1. Packet Reordering (via Sequence Number).
 * 2. Adaptive Buffering (Dynamic size based on network jitter).
 * 3. Gap handling (Loss or Late arrival).
 */
public class JitterBuffer {

    private static final int MAX_BUFFER_SIZE = 25; // Max 500ms latency cap
    private static final int MIN_BUFFER_SIZE = 2; // Min 40ms latency

    // Initial target buffer size (frames)
    private int currentTargetSize = 4; // Start with ~80ms buffer

    // Stats for adaptation
    private int underrunCount = 0;
    private long lastStatsTime = 0;

    private static class Frame {
        final byte[] data;
        final int sequence;

        Frame(byte[] data, int sequence, long arrivalTime) {
            this.data = data;
            this.sequence = sequence;
            // arrivalTime not used currently but kept in constructor for future RTT
            // calculation
        }
    }

    // Queue ordered by sequence number (Ascending)
    private final PriorityQueue<Frame> buffer = new PriorityQueue<>(Comparator.comparingInt(f -> f.sequence));

    // Tracking state
    private int lastPoppedSequence = -1;
    private boolean primed = false;

    /**
     * Add a packet to the buffer.
     * 
     * @param data     Opus encoded audio
     * @param sequence Packet sequence number
     */
    public synchronized void push(byte[] data, int sequence) {
        // 1. Late packet check: If this sequence is older than what we just played,
        // discard it.
        // Handling wrap-around (int overflow) is technically needed but unlikely to be
        // reached in session.
        if (lastPoppedSequence != -1 && sequence <= lastPoppedSequence) {
            return; // Discard late packet
        }

        // 2. Add to buffer
        buffer.add(new Frame(data, sequence, System.currentTimeMillis()));

        // 3. Overflow protection: If buffer too huge, drop oldest (lowest sequence)
        // But wait, PriorityQueue head is lowest sequence (correct).
        // If we have too many future packets, we are lagging behind.
        // We should skip forward.
        if (buffer.size() > MAX_BUFFER_SIZE) {
            Frame skipped = buffer.poll(); // Skip/Fast-forward
            if (skipped != null) {
                lastPoppedSequence = skipped.sequence;
            }
        }

        // 4. Priming: Wait until we have enough packets to start playing
        if (!primed && buffer.size() >= currentTargetSize) {
            primed = true;
        }

        // 5. Adaptation Logic (Per push check or periodic?)
        // Done in pop() usually.
    }

    /**
     * Get the next frame to play.
     * 
     * @return Frame data or null (if missing/buffering)
     */
    public synchronized byte[] pop() {
        long now = System.currentTimeMillis();

        // Periodic Adaptation (every 2 seconds)
        if (now - lastStatsTime > 2000) {
            updateAdaptation();
            lastStatsTime = now;
        }

        if (!primed) {
            // Still buffering
            return null;
        }

        if (buffer.isEmpty()) {
            // UNDERRUN: We ran out of data!
            // Network is jittery. Increase buffer size.
            underrunCount++;
            primed = false; // Stop playback, re-buffer
            return null; // Trigger PLC
        }

        // Check sequence continuity
        Frame next = buffer.peek();

        // If this is the very first frame we pop
        if (lastPoppedSequence == -1) {
            lastPoppedSequence = next.sequence;
            buffer.poll();
            return next.data;
        }

        int expectedSeq = lastPoppedSequence + 1;

        if (next.sequence == expectedSeq) {
            // Perfect! Next frame is here.
            lastPoppedSequence = next.sequence;
            buffer.poll();
            return next.data;
        } else if (next.sequence > expectedSeq) {
            // GAP detected (Packet Loss).
            // Example: We played 100, Next in buffer is 102. Missing 101.
            // Option A: Wait for 101? (Increase latency)
            // Option B: Skip 101 and play 102? (Glitch)
            // Option C: Return null to trigger PLC for 101, keep 102 for next pop.

            // We choose Option C (PLC) but we must increment lastPoppedSequence
            // so next time we ask for 102.
            lastPoppedSequence = expectedSeq; // Fake that we played 101 (via PLC)
            return null; // Mixer will do PLC
        } else {
            // next.sequence < expectedSeq
            // This case shouldn't happen due to push() check, but for safety:
            buffer.poll(); // Discard duplicate/old
            return pop(); // Try again
        }
    }

    private void updateAdaptation() {
        if (underrunCount > 0) {
            // We had underruns -> Increase latency
            currentTargetSize = Math.min(currentTargetSize + 1, MAX_BUFFER_SIZE);
            // Limit max increases per cycle
        } else {
            // Smooth sailing -> Try to reduce latency
            if (currentTargetSize > MIN_BUFFER_SIZE) {
                currentTargetSize--;
            }
        }
        underrunCount = 0; // Reset counter
    }

    public synchronized int size() {
        return buffer.size();
    }

    public synchronized void clear() {
        buffer.clear();
        primed = false;
        lastPoppedSequence = -1;
        currentTargetSize = 4; // Reset to default
    }
}
