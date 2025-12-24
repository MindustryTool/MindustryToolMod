package lemmesay.voice.speaker;

import arc.util.Log;
import lemmesay.utils.AudioUtils;

import javax.sound.sampled.*;

public class JavaxSpeaker implements Speaker {
    private static final int SAMPLE_RATE = 48000;
    private final AudioFormat format;
    private SourceDataLine line;
    private boolean opened;

    public JavaxSpeaker() {
        // default to stereo 16-bit PCM, little-endian at 48000 Hz
        this.format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    }

    // Optional: allow specifying channel count
    public JavaxSpeaker(int channels) {
        this.format = new AudioFormat(SAMPLE_RATE, 16, channels, true, false);
    }

    @Override
    public void open() throws IllegalStateException {
        if (opened) throw new IllegalStateException("Already opened");
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            opened = true;
            Log.info("Speaker opened with name" + line.getLineInfo().toString());
        } catch (LineUnavailableException e) {
            throw new IllegalStateException("Cannot open audio line", e);
        }
    }

    @Override
    public void play(short[] audio, float volume) {
        if (!opened || line == null) throw new IllegalStateException("Speaker not opened");
        if (audio == null || audio.length == 0) return;

        float vol = Math.max(0f, Math.min(1f, volume));
        // Try hardware volume; if not available, scale samples
        boolean controlOk = applyControlVolume(vol);
        if (!controlOk && vol < 1f) {
            for (int i = 0; i < audio.length; i++) {
                audio[i] = (short) (audio[i] * vol);
            }
        }
        byte[] buffer = AudioUtils.shorts2bytes(audio);
        line.write(buffer, 0, buffer.length);
    }

    private boolean applyControlVolume(float volume) {
        if (line == null) return false;
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gain.getMinimum();
                float max = gain.getMaximum();
                float dB;
                if (volume <= 0f) {
                    dB = min;
                } else {
                    dB = 20f * (float) Math.log10(volume);
                    dB = Math.max(min, Math.min(max, dB));
                }
                gain.setValue(dB);
                return true;
            } else if (line.isControlSupported(FloatControl.Type.VOLUME)) {
                FloatControl vol = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
                float v = Math.max(vol.getMinimum(), Math.min(vol.getMaximum(), volume));
                vol.setValue(v);
                return true;
            }
        } catch (IllegalArgumentException e) {
            Log.err(e);
        }
        return false;
    }

    @Override
    public void close() {
        if (!opened) return;
        try {
            line.drain();
        } catch (Exception ignored) {}
        try {
            line.stop();
        } catch (Exception ignored) {}
        try {
            line.close();
        } catch (Exception ignored) {}
        opened = false;
        line = null;
    }
}
