package lemmesay.voice.process.opus;

public interface OpusEncoder {

    byte[] encode(short[] rawAudio);

    void resetState();

    void close();

    boolean isClosed();
}
