package lemmesay.voice.process.denoiser;

public interface Denoiser {
    short[] denoise(short[] rawAudio);

    float denoiseInPlace(short[] rawAudio);

    int getFrameSize();

    float getSpeechProbability(short[] audio);

    public void close();

    public boolean isClosed();
}
