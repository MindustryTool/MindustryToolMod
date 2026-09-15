package mindustrytool.features.translation;

import arc.util.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class DevXSecrets {
	private static final byte[] MASK = new byte[]{(byte) 0x5A, (byte) 0xA5, (byte) 0x3C, (byte) 0xC3, (byte) 0x69, (byte) 0x96, (byte) 0x0F, (byte) 0xF0};
	private static @Nullable String cachedKey;

	private DevXSecrets() {
	}

	public static String getApiKey() {
		if (cachedKey != null) {
			return cachedKey;
		}

		try (InputStream in = DevXSecrets.class.getResourceAsStream("/devx.secret")) {
			if (in == null) {
				cachedKey = "";
				return "";
			}
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buf = new byte[256];
			int r;
			while ((r = in.read(buf)) != -1) {
				bout.write(buf, 0, r);
			}
			byte[] encoded = bout.toByteArray();
			if (encoded.length == 0) {
				cachedKey = "";
				return "";
			}
			byte[] decoded = new byte[encoded.length];
			for (int i = 0; i < encoded.length; i++) {
				decoded[i] = (byte) (encoded[i] ^ MASK[i % MASK.length]);
			}
			cachedKey = new String(decoded, StandardCharsets.UTF_8).trim();
			return cachedKey;
		} catch (Exception e) {
			cachedKey = "";
			return "";
		}
	}

	public static boolean hasBuiltInKey() {
		return !getApiKey().isEmpty();
	}
}
