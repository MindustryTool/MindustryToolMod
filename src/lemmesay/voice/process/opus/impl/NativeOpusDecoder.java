package lemmesay.voice.process.opus.impl;

import lemmesay.voice.process.opus.OpusDecoder;

import java.io.IOException;
import java.lang.ref.Cleaner;

public class NativeOpusDecoder implements OpusDecoder, AutoCloseable {

    private static final Cleaner CLEANER = Cleaner.create();
    private final ResState state;
    private final Cleaner.Cleanable cleanable;

    public NativeOpusDecoder(int sampleRate, int channels) throws IOException, Exception {
        var decoder = new de.maxhenkel.opus4j.OpusDecoder(sampleRate, channels);
        this.state = new ResState(decoder);
        this.cleanable = CLEANER.register(this, state);
    }

    @Override
    public short[] decode(byte[] input) {
        return this.state.decoder.decode(input);
    }

    @Override
    public short[][] decode(byte[] input, int frames) {
        return this.state.decoder.decode(input, frames);
    }

    @Override
    public void resetState() {
        this.state.decoder.resetState();
    }

    @Override
    public void close() {
        this.cleanable.clean();
    }

    @Override
    public boolean isClosed() {
        return state.decoder.isClosed();
    }

    private static final class ResState implements Runnable {

        private final de.maxhenkel.opus4j.OpusDecoder decoder;

        private ResState(de.maxhenkel.opus4j.OpusDecoder decoder) {
            this.decoder = decoder;
        }

        @Override
        public void run() {
            this.decoder.close();
        }
    }
}
