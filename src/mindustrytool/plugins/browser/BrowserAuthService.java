package mindustrytool.plugins.browser;

import arc.Core;

/** Provides authentication token for API requests. Self-contained version. */
public final class BrowserAuthService {
    private static final String ACCESS_KEY = "mindustrytool.auth.accessToken";
    
    private BrowserAuthService() {}
    
    /** Get the access token if available. */
    public static String getAccessToken() {
        return Core.settings.getString(ACCESS_KEY, null);
    }
}
