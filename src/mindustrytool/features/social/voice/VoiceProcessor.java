package mindustrytool.features.social.voice;

import arc.util.Log;
import arc.util.Nullable;
import org.concentus.OpusApplication;
import org.concentus.OpusDecoder;
import org.concentus.OpusEncoder;
import org.concentus.OpusException;
import org.concentus.OpusSignal;

/**
 * Cross-platform voice audio processor using Concentus (pure Java Opus).
 * Works on both Desktop and Android without native libraries.
 * Optimized for voice quality with proper bitrate and complexity settings.
 */
public class VoiceProcessor {

    private static final String TAG = "[VoiceProc]";

    // Opus encoder settings for good voice quality
    private static final int BITRATE = 36000; // 36 kbps - Very high voice quality (Crisp)
    private static final int COMPLEXITY = 6; // Balance between quality and CPU (Mobile friendly)
    private static final int FRAME_SIZE = VoiceConstants.BUFFER_SIZE; // 40ms at 48kHz
    public static final double VAD_THRESHOLD = 20.0; // Lowered to 20.0 to fix choppy audio/PC mics

    @Nullable
    private OpusEncoder encoder;
    @Nullable
    // Per-player decoders to maintain state
    private final java.util.concurrent.ConcurrentHashMap<String, OpusDecoder> decoders = new java.util.concurrent.ConcurrentHashMap<>();

    // Denoising disabled for cross-platform (rnnoise4j requires native libs)
    private boolean denoiseEnabled = false;

    public VoiceProcessor() {
        try {
            // Initialize encoder with optimized settings for voice
            encoder = new OpusEncoder(
                    VoiceConstants.SAMPLE_RATE,
                    VoiceConstants.CHANNELS,
                    OpusApplication.OPUS_APPLICATION_VOIP);

            // Configure encoder for better quality
            encoder.setBitrate(BITRATE);
            encoder.setComplexity(COMPLEXITY);
            encoder.setSignalType(OpusSignal.OPUS_SIGNAL_VOICE);
            encoder.setForceMode(org.concentus.OpusMode.MODE_HYBRID); // Best for voice
            encoder.setUseVBR(true); // Variable bitrate for better quality
            encoder.setUseConstrainedVBR(true); // More consistent bitrate
            encoder.setUseDTX(false); // Disable DTX for continuous transmission
            encoder.setPacketLossPercent(5); // Optimize for some packet loss

            Log.info("@ Encoder configured: bitrate=@, complexity=@", TAG, BITRATE, COMPLEXITY);
            // Decoding is now per-player, initialized on demand
            Log.info("@ Processor initialized (Concentus pure Java, optimized)", TAG);
        } catch (OpusException e) {
            Log.err("@ Failed to initialize processor: @", TAG, e.getMessage());
            throw new RuntimeException("Failed to initialize voice processor", e);
        }
    }

    /**
     * Denoise audio data.
     * Note: RNNoise disabled for cross-platform compatibility.
     * Returns input unchanged.
     */
    public short[] denoise(short[] input) {
        // Denoising disabled - rnnoise4j requires native libs not available on Android
        return input;
    }

    /**
     * Encode audio data to Opus format.
     */
    public byte[] encode(short[] input) {
        if (encoder == null) {
            throw new IllegalStateException("Encoder not initialized");
        }

        // Validate input size
        if (input == null || input.length == 0) {
            return new byte[0];
        }

        // Voice Activity Detection (VAD) - Noise Gate
        // Filter out silence/static noise to improve clarity and save bandwidth
        double rms = calculateRMS(input);
        if (rms < VAD_THRESHOLD) {
            return new byte[0]; // Send silence (Decoder will generate silence)
        }

        try {
            // Concentus returns encoded bytes - allocate buffer for max frame size
            byte[] output = new byte[FRAME_SIZE * 2];
            int inputLength = Math.min(input.length, FRAME_SIZE);

            int encodedLength = encoder.encode(input, 0, inputLength, output, 0, output.length);

            if (encodedLength <= 0) {
                Log.warn("@ Encode returned @ bytes", TAG, encodedLength);
                return new byte[0];
            }

            // Return only the actual encoded bytes
            byte[] result = new byte[encodedLength];
            System.arraycopy(output, 0, result, 0, encodedLength);
            return result;
        } catch (OpusException e) {
            Log.err("@ Encode error: @", TAG, e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Decode Opus data to audio for specific player.
     * Synchronized per-decoder via Map.
     */
    public short[] decode(String playerId, byte[] input) {
        // Validate input
        if (input == null || input.length == 0) {
            return new short[0];
        }

        // Get or create decoder for this player
        OpusDecoder decoder = decoders.computeIfAbsent(playerId, id -> {
            try {
                return new OpusDecoder(
                        VoiceConstants.SAMPLE_RATE,
                        VoiceConstants.CHANNELS);
            } catch (OpusException e) {
                Log.err("@ Failed to create decoder for @: @", TAG, id, e.getMessage());
                return null;
            }
        });

        if (decoder == null)
            return new short[0];

        try {
            // Synchronize on the specific decoder instance
            synchronized (decoder) {
                short[] output = new short[FRAME_SIZE];
                int decodedSamples = decoder.decode(input, 0, input.length, output, 0, FRAME_SIZE, false);

                if (decodedSamples <= 0) {
                    return new short[0];
                }

                // Return only the actual decoded samples
                if (decodedSamples < FRAME_SIZE) {
                    short[] result = new short[decodedSamples];
                    System.arraycopy(output, 0, result, 0, decodedSamples);
                    return result;
                }
                return output;
            }
        } catch (OpusException e) {
            Log.err("@ Decode error for @: @", TAG, playerId, e.getMessage());
            // Reset decoder on error
            decoders.remove(playerId);
            return new short[0];
        }
    }

    /**
     * Decode with Packet Loss Concealment (PLC).
     * Call this when a packet is expected but missing.
     * Generates a frame based on previous audio state.
     */
    public short[] decodePLC(String playerId) {
        OpusDecoder decoder = decoders.get(playerId);
        if (decoder == null)
            return new short[0];

        try {
            synchronized (decoder) {
                short[] output = new short[FRAME_SIZE];
                // Passing null to decode triggers PLC
                int decodedSamples = decoder.decode(null, 0, 0, output, 0, FRAME_SIZE, false);

                if (decodedSamples <= 0)
                    return new short[0];

                return output;
            }
        } catch (OpusException e) {
            Log.err("@ PLC error for @: @", TAG, playerId, e.getMessage());
            return new short[0];
        }
    }

    public boolean isDenoiseEnabled() {
        return denoiseEnabled;
    }

    public void setDenoiseEnabled(boolean enabled) {
        // Denoising is disabled for cross-platform compatibility
        // Keep this method for API compatibility but don't actually enable it
        this.denoiseEnabled = false;
        Log.info("@ Denoise unavailable (cross-platform mode)", TAG);
    }

    public void dispose() {
        // Concentus encoders/decoders don't need explicit close
        encoder = null;
        decoders.clear();
        Log.info("@ Processor disposed", TAG);
    }

    /**
     * Calculate Root Mean Square (RMS) amplitude of audio buffer.
     * Used for Voice Activity Detection (VAD).
     */
    public double calculateRMS(short[] buffer) {
        if (buffer == null || buffer.length == 0)
            return 0;

        long sum = 0;
        for (short sample : buffer) {
            sum += sample * sample;
        }
        return Math.sqrt(sum / (double) buffer.length);
    }
}
