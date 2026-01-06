package mindustrytool.plugins.browser;

import arc.Core;
import arc.util.Log;
import mindustrytool.Main;

/**
 * Provides authentication token for API requests.
 * Uses AuthService when available, falls back to settings.
 */
public final class BrowserAuthService {
    private static final String ACCESS_KEY = "mindustrytool.auth.accessToken";

    private BrowserAuthService() {
    }

    /** Get the access token if available. Returns null if no valid token. */
    public static String getAccessToken() {
        // Try to use AuthService if AuthPlugin is loaded
        if (Main.hasPlugin("mindustrytool.plugins.auth.AuthPlugin")) {
            try {
                // Use reflection to avoid hard dependency
                Class<?> authService = Class.forName("mindustrytool.plugins.auth.AuthService");
                Object token = authService.getMethod("getAccessToken").invoke(null);
                if (token != null)
                    return (String) token;
            } catch (Exception e) {
                Log.err("[BrowserAuth] Failed to get token from AuthService", e);
            }
        }

        // Fallback: read directly from settings
        String token = Core.settings.getString(ACCESS_KEY, null);
        return (token != null && !token.isEmpty()) ? token : null;
    }

    public static void invalidateToken() {
        // Clear settings
        Core.settings.remove(ACCESS_KEY);

        // Also notify AuthService if present
        if (Main.hasPlugin("mindustrytool.plugins.auth.AuthPlugin")) {
            try {
                Class<?> authService = Class.forName("mindustrytool.plugins.auth.AuthService");
                authService.getMethod("logout").invoke(null);
            } catch (Exception e) {
                Log.err("[BrowserAuth] Failed to logout AuthService", e);
            }
        }
    }
}
