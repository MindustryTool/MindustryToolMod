package lemmesay.voice.process.opus.impl;

import de.maxhenkel.opus4j.UnknownPlatformException;
import lemmesay.voice.process.opus.OpusEncoder;

import java.io.IOException;
import java.lang.ref.Cleaner;

public class NativeOpusEncoder implements OpusEncoder, AutoCloseable {

    private static final Cleaner CLEANER = Cleaner.create();
    private final ResState state;
    private final Cleaner.Cleanable cleanable;

    public NativeOpusEncoder(int sampleRate, int channels, de.maxhenkel.opus4j.OpusEncoder.Application application) throws IOException, UnknownPlatformException {
        var encoder = new de.maxhenkel.opus4j.OpusEncoder(sampleRate, channels, application);
        encoder.setMaxPacketLossPercentage(0.05F);
        this.state = new ResState(encoder);
        this.cleanable = CLEANER.register(this, state);
    }

    public void setMaxPayloadSize(int size) {
        this.state.encoder.setMaxPayloadSize(size);
    }

    @Override
    public byte[] encode(short[] rawAudio) {
        return this.state.encoder.encode(rawAudio);
    }

    @Override
    public void resetState() {
        this.state.encoder.resetState();
    }

    @Override
    public void close() {
        this.cleanable.clean();
    }

    @Override
    public boolean isClosed() {
        return state.encoder.isClosed();
    }

    private static final class ResState implements Runnable {

        private final de.maxhenkel.opus4j.OpusEncoder encoder;

        private ResState(de.maxhenkel.opus4j.OpusEncoder encoder) {
            this.encoder = encoder;
        }

        @Override
        public void run() {
            this.encoder.close();
        }
    }
}
