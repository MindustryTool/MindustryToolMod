package lemmesay.voice;

import arc.util.Log;
import lemmesay.shared.packet.MicPacket;
import lemmesay.voice.process.denoiser.NativeRNNDenoiser;
import lemmesay.voice.process.opus.impl.NativeOpusDecoder;
import lemmesay.voice.process.opus.impl.NativeOpusEncoder;
import lemmesay.voice.speaker.JavaxSpeaker;
import mindustry.Vars;

public class VoiceProcessing {

    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 1;
    private static final int BUFFER_SIZE = 960;

    private static VoiceProcessing _instance;

    public static VoiceProcessing getInstance() {
        if (_instance == null) {
            _instance = new VoiceProcessing();
        }
        return _instance;
    }

    private NativeRNNDenoiser denoiser;
    private NativeOpusEncoder encoder;
    private NativeOpusDecoder decoder;
    private JavaxSpeaker speaker;

    private VoiceProcessing() {
        try {
            denoiser = new NativeRNNDenoiser();
            encoder = new NativeOpusEncoder(SAMPLE_RATE, CHANNELS, de.maxhenkel.opus4j.OpusEncoder.Application.VOIP);
            decoder = new NativeOpusDecoder(SAMPLE_RATE, CHANNELS);
            speaker = new JavaxSpeaker();
            speaker.open();
            Log.info("Speaker initialized for voice chat.");
        } catch (Exception e) {
            Log.err(e);
        }
    }

    public void handleAudio(short[] audioData) {
        short[] denoisedData = denoiser.denoise(audioData);
        byte[] encodedData = encoder.encode(denoisedData);
        var packet = new MicPacket();
        packet.audioData = encodedData;
        Vars.net.send(packet, false);
    }

    public void handleMicPacket(MicPacket packet) {
        var data = packet.audioData;
        short[] decodedData = decoder.decode(data);
        speaker.play(decodedData, 80);
    }
}
