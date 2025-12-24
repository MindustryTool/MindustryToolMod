package lemmesay.voice.speaker;

public interface Speaker {

    void open() throws IllegalStateException;

    void play(short[] audio, float volume);

    void close();
}
