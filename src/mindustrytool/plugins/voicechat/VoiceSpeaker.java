package mindustrytool.plugins.voicechat;

import arc.util.Log;
import arc.util.Nullable;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Voice speaker playback using javax.sound.
 * Desktop-only implementation.
 */
public class VoiceSpeaker {

    private static final String TAG = "[VoiceSpk]";

    @Nullable
    private SourceDataLine speaker;
    private final int sampleRate;
    private float volume = 0.8f;

    public VoiceSpeaker() {
        this.sampleRate = VoiceConstants.SAMPLE_RATE;
        open();
    }

    public void open() {
        if (isOpen())
            return;

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, 1, 2, sampleRate, false);

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(format);
            speaker.start();
            Log.info("@ Speaker opened", TAG);
        } catch (Exception e) {
            Log.err("@ Failed to open speaker: @", TAG, e.getMessage());
        }
    }

    public void play(short[] audioData) {
        if (speaker == null || !speaker.isOpen())
            return;

        // Apply volume
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] = (short) (audioData[i] * volume);
        }

        byte[] bytes = shortsToBytes(audioData);
        speaker.write(bytes, 0, bytes.length);
    }

    public boolean isOpen() {
        return speaker != null && speaker.isOpen();
    }

    public void close() {
        if (speaker == null)
            return;
        speaker.stop();
        speaker.flush();
        speaker.close();
        speaker = null;
        Log.info("@ Speaker closed", TAG);
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
    }

    private static byte[] shortsToBytes(short[] shorts) {
        ByteBuffer bb = ByteBuffer.allocate(shorts.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : shorts) {
            bb.putShort(s);
        }
        return bb.array();
    }
}
