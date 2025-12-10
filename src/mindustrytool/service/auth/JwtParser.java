package mindustrytool.service.auth;

import arc.util.Log;

/** Parses and validates JWT tokens. */
public final class JwtParser {
    private static final long DEFAULT_EXPIRY_MS = 3600 * 1000;

    private JwtParser() {}

    /** Parse JWT token to get expiry time in milliseconds. */
    public static long parseExpiry(String token) {
        if (token == null) return 0;
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return 0;
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            int expIndex = payload.indexOf("\"exp\"");
            if (expIndex == -1) return 0;
            int colonIndex = payload.indexOf(":", expIndex);
            int endIndex = payload.indexOf(",", colonIndex);
            if (endIndex == -1) endIndex = payload.indexOf("}", colonIndex);
            String expStr = payload.substring(colonIndex + 1, endIndex).trim();
            return Long.parseLong(expStr) * 1000;
        } catch (Exception e) {
            Log.err("[Auth] Failed to parse token expiry", e);
            return System.currentTimeMillis() + DEFAULT_EXPIRY_MS;
        }
    }

    /** Check if token is expired. */
    public static boolean isExpired(long expiryMs) {
        return expiryMs > 0 && System.currentTimeMillis() > expiryMs;
    }

    /** Check if token should be refreshed (within margin). */
    public static boolean shouldRefresh(long expiryMs, long marginMs) {
        return expiryMs > 0 && System.currentTimeMillis() > expiryMs - marginMs;
    }
}
