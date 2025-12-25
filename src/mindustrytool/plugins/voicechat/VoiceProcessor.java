package mindustrytool.plugins.voicechat;

import arc.util.Log;
import arc.util.Nullable;
import org.concentus.OpusApplication;
import org.concentus.OpusDecoder;
import org.concentus.OpusEncoder;
import org.concentus.OpusException;

/**
 * Cross-platform voice audio processor using Concentus (pure Java Opus).
 * Works on both Desktop and Android without native libraries.
 */
public class VoiceProcessor {

    private static final String TAG = "[VoiceProc]";

    @Nullable
    private OpusEncoder encoder;
    @Nullable
    private OpusDecoder decoder;

    // Denoising disabled for cross-platform (rnnoise4j requires native libs)
    private boolean denoiseEnabled = false;

    public VoiceProcessor() {
        try {
            encoder = new OpusEncoder(
                    VoiceConstants.SAMPLE_RATE,
                    VoiceConstants.CHANNELS,
                    OpusApplication.OPUS_APPLICATION_VOIP);
            decoder = new OpusDecoder(
                    VoiceConstants.SAMPLE_RATE,
                    VoiceConstants.CHANNELS);
            Log.info("@ Processor initialized (Concentus pure Java)", TAG);
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
        try {
            // Concentus returns encoded bytes - allocate buffer for max frame size
            byte[] output = new byte[VoiceConstants.BUFFER_SIZE * 2];
            int encodedLength = encoder.encode(input, 0, VoiceConstants.BUFFER_SIZE, output, 0, output.length);

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
        try {
            short[] output = new short[VoiceConstants.BUFFER_SIZE];
            int decodedSamples = decoder.decode(input, 0, input.length, output, 0, VoiceConstants.BUFFER_SIZE, false);

            // Return only the actual decoded samples
            if (decodedSamples < VoiceConstants.BUFFER_SIZE) {
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
