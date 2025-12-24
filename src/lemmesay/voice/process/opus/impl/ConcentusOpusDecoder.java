package lemmesay.voice.process.opus.impl;

import lemmesay.voice.process.opus.OpusDecoder;

//TODO: Implement ConcentusOpusDecoder
public class ConcentusOpusDecoder implements OpusDecoder {
    @Override
    public short[] decode(byte[] input) {
        return new short[0];
    }

    @Override
    public short[][] decode(byte[] input, int frames) {
        return new short[0][];
    }

    @Override
    public void resetState() {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
