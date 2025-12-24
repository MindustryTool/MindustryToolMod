package lemmesay.voice.process.opus;

import arc.util.Nullable;

public interface OpusDecoder {

    short[] decode(@Nullable byte[] input);

    short[][] decode(@Nullable byte[] input, int frames);

    void resetState();

    void close();

    boolean isClosed();
}
