package lemmesay.voice.microphone;

public interface Microphone {

    void open() throws IllegalStateException;

    void start();

    void stop();

    void close();

    boolean isOpen();

    boolean isStarted();

    int available();

    short[] read();
}
