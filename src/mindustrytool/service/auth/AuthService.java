package mindustrytool.service.auth;

import arc.func.Cons; import arc.util.Log;
import mindustrytool.core.model.TokenResponse;

/** Facade for authentication - delegates to smaller auth classes. */
public class AuthService {
    private static final long REFRESH_MARGIN_MS = 60_000;
    private static String accessToken, refreshToken;
    private static long accessTokenExpiry = 0;
    private static Runnable onLoginStateChanged = () -> {};

    static { loadTokens(); }

    public static boolean isLoggedIn() { return accessToken != null && !accessToken.isEmpty(); }

    public static String getAccessToken() {
        if (accessToken != null && JwtParser.isExpired(accessTokenExpiry)) { if (refreshToken != null) refreshTokenAsync(null, null); return null; }
        if (accessToken != null && JwtParser.shouldRefresh(accessTokenExpiry, REFRESH_MARGIN_MS)) refreshTokenAsync(null, null);
        return accessToken;
    }

    public static String getAuthHeader() { String t = getAccessToken(); return t != null ? "Bearer " + t : null; }

    public static void login(Cons<String> ok, Cons<String> err) { AuthLoginFlow.start(t -> { setTokens(t); ok.get("Login successful!"); }, err); }

    public static void refreshTokenAsync(Cons<Boolean> ok, Cons<String> err) {
        AuthTokenRefresher.refresh(refreshToken, t -> { if (t != null) setTokens(t); if (ok != null) ok.get(t != null); }, e -> { logout(); if (err != null) err.get(e); });
    }

    public static void logout() { accessToken = refreshToken = null; accessTokenExpiry = 0; AuthTokenStorage.clear(); onLoginStateChanged.run(); }
    public static void onLoginStateChanged(Runnable cb) { onLoginStateChanged = cb; }

    private static void setTokens(TokenResponse t) {
        accessToken = t.accessToken(); if (t.refreshToken() != null) refreshToken = t.refreshToken();
        accessTokenExpiry = JwtParser.parseExpiry(accessToken); AuthTokenStorage.save(accessToken, refreshToken); onLoginStateChanged.run();
    }

    private static void loadTokens() {
        accessToken = AuthTokenStorage.loadAccessToken(); refreshToken = AuthTokenStorage.loadRefreshToken();
        if (accessToken != null) { accessTokenExpiry = JwtParser.parseExpiry(accessToken); if (JwtParser.isExpired(accessTokenExpiry)) { accessToken = null; accessTokenExpiry = 0; } }
        Log.info("[Auth] Loaded. Logged in: " + isLoggedIn());
    }
}
