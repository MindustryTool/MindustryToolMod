package mindustrytool.plugins.voicechat;

import arc.util.Log;
import arc.util.Nullable;
import de.maxhenkel.opus4j.OpusDecoder;
import de.maxhenkel.opus4j.OpusEncoder;
import de.maxhenkel.rnnoise4j.Denoiser;

/**
 * Voice audio processor with Opus encoding/decoding and RNNoise denoising.
 */
public class VoiceProcessor {

    private static final String TAG = "[VoiceProc]";

    @Nullable
    private OpusEncoder encoder;
    @Nullable
    private OpusDecoder decoder;
    @Nullable
    private Denoiser denoiser;

    private boolean denoiseEnabled = true;

    public VoiceProcessor() {
        try {
            encoder = new OpusEncoder(
                    VoiceConstants.SAMPLE_RATE,
                    VoiceConstants.CHANNELS,
                    OpusEncoder.Application.VOIP);
            decoder = new OpusDecoder(
                    VoiceConstants.SAMPLE_RATE,
                    VoiceConstants.CHANNELS);
            denoiser = new Denoiser();
            Log.info("@ Processor initialized", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to initialize processor: @", TAG, e.getMessage());
            throw new RuntimeException("Failed to initialize voice processor", e);
        }
    }

    /**
     * Denoise audio data using RNNoise.
     */
    public short[] denoise(short[] input) {
        if (!denoiseEnabled || denoiser == null) {
            return input;
        }
        return denoiser.denoise(input);
    }

    /**
     * Encode audio data to Opus format.
     */
    public byte[] encode(short[] input) {
        if (encoder == null) {
            throw new IllegalStateException("Encoder not initialized");
        }
        return encoder.encode(input);
    }

    /**
     * Decode Opus data to audio.
     */
    public short[] decode(byte[] input) {
        if (decoder == null) {
            throw new IllegalStateException("Decoder not initialized");
        }
        return decoder.decode(input);
    }

    public boolean isDenoiseEnabled() {
        return denoiseEnabled;
    }

    public void setDenoiseEnabled(boolean enabled) {
        this.denoiseEnabled = enabled;
        Log.info("@ Denoise: @", TAG, enabled);
    }

    public void dispose() {
        if (encoder != null) {
            encoder.close();
            encoder = null;
        }
        if (decoder != null) {
            decoder.close();
            decoder = null;
        }
        if (denoiser != null) {
            denoiser.close();
            denoiser = null;
        }
        Log.info("@ Processor disposed", TAG);
    }
}
