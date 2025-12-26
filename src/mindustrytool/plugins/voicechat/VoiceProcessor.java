package mindustrytool.plugins.voicechat;

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
    private static final int BITRATE = 24000; // 24 kbps - efficient voice quality
    private static final int COMPLEXITY = 3; // 0-10, lower = less CPU (Critical for Android)
    private static final int FRAME_SIZE = VoiceConstants.BUFFER_SIZE; // 20ms at 48kHz

    @Nullable
    private OpusEncoder encoder;
    @Nullable
    private OpusDecoder decoder;

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

            // Initialize decoder
            decoder = new OpusDecoder(
                    VoiceConstants.SAMPLE_RATE,
                    VoiceConstants.CHANNELS);

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
     * Decode Opus data to audio.
     */
    public short[] decode(byte[] input) {
        if (decoder == null) {
            throw new IllegalStateException("Decoder not initialized");
        }

        // Validate input
        if (input == null || input.length == 0) {
            return new short[0];
        }

        try {
            short[] output = new short[FRAME_SIZE];
            int decodedSamples = decoder.decode(input, 0, input.length, output, 0, FRAME_SIZE, false);

            if (decodedSamples <= 0) {
                Log.warn("@ Decode returned @ samples", TAG, decodedSamples);
                return new short[0];
            }

            // Return only the actual decoded samples
            if (decodedSamples < FRAME_SIZE) {
                short[] result = new short[decodedSamples];
                System.arraycopy(output, 0, result, 0, decodedSamples);
                return result;
            }
            return output;
        } catch (OpusException e) {
            Log.err("@ Decode error: @", TAG, e.getMessage());
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
        decoder = null;
        Log.info("@ Processor disposed", TAG);
    }
}
