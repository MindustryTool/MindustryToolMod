package lemmesay.voice.process.opus;

public class OpusManager {

    private static OpusEncoder encoder;
    private static OpusDecoder decoder;

    public static OpusEncoder getEncoder() {
        return encoder;
    }

    public static OpusDecoder getDecoder() {
        return decoder;
    }


}
