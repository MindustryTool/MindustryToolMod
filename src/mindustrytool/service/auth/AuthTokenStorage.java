package mindustrytool.service.auth;

import arc.Core;
import arc.util.Log;

/** Handles token persistence to/from settings. */
public final class AuthTokenStorage {
    private static final String ACCESS_KEY = "mindustrytool.auth.accessToken";
    private static final String REFRESH_KEY = "mindustrytool.auth.refreshToken";

    private AuthTokenStorage() {}

    public static void save(String accessToken, String refreshToken) {
        if (accessToken != null) Core.settings.put(ACCESS_KEY, accessToken);
        else Core.settings.remove(ACCESS_KEY);
        if (refreshToken != null) Core.settings.put(REFRESH_KEY, refreshToken);
        else Core.settings.remove(REFRESH_KEY);
    }

    public static String loadAccessToken() {
        return Core.settings.getString(ACCESS_KEY, null);
    }

    public static String loadRefreshToken() {
        return Core.settings.getString(REFRESH_KEY, null);
    }

    public static void clear() {
        Core.settings.remove(ACCESS_KEY);
        Core.settings.remove(REFRESH_KEY);
        Log.info("[Auth] Tokens cleared");
    }
}
