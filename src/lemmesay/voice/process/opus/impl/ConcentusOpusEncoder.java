package lemmesay.voice.process.opus.impl;

import lemmesay.voice.process.opus.OpusEncoder;

public class ConcentusOpusEncoder implements OpusEncoder {
    @Override
    public byte[] encode(short[] rawAudio) {
        return new byte[0];
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
    //TODO: Implement ConcentusOpusEncoder
}
