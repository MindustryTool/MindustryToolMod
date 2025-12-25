package lemmesay.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AudioUtils {

    public static short[] bytes2shorts(byte[] bytes) {
        if (bytes.length % 2 != 0) {
            throw new IllegalArgumentException("Byte array length must be even to convert to shorts.");
        }
        ShortBuffer sBuff = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] out = new short[sBuff.remaining()];
        sBuff.get(out);
        return out;
    }

    public static byte[] shorts2bytes(short[] shorts) {
        ByteBuffer bBuff = ByteBuffer.allocate(shorts.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : shorts) {
            bBuff.putShort(s);
        }
        return bBuff.array();
    }
}
