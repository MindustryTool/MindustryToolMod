package mindustrytool.plugins.auth;

import arc.Core;
import arc.func.Cons;
import arc.util.*;
import mindustry.io.JsonIO;

/** Unified authentication service handling login, tokens and refresh. */
public class AuthService {
    private static final String API_URL = "https://api.mindustry-tool.com/api/v4/";
    private static final long REFRESH_MARGIN_MS = 60_000, DEFAULT_EXPIRY_MS = 3600 * 1000, LOGIN_TIMEOUT_MS = 30_000;
    private static final String ACCESS_KEY = "mindustrytool.auth.accessToken", REFRESH_KEY = "mindustrytool.auth.refreshToken";
    private static String accessToken, refreshToken;
    private static long accessTokenExpiry = 0;
    private static boolean isRefreshing = false;
    private static Runnable onLoginStateChanged = () -> {};

    static { loadTokens(); }

    public static boolean isLoggedIn() { return accessToken != null && !accessToken.isEmpty(); }

    public static String getAccessToken() {
        if (accessToken != null && isExpired(accessTokenExpiry)) { if (refreshToken != null) refreshTokenAsync(null, null); return null; }
        if (accessToken != null && shouldRefresh(accessTokenExpiry, REFRESH_MARGIN_MS)) refreshTokenAsync(null, null);
        return accessToken;
    }

    public static String getAuthHeader() { String t = getAccessToken(); return t != null ? "Bearer " + t : null; }

    public static void login(Cons<String> ok, Cons<String> err) {
        Log.info("[Auth] Starting login...");
        Http.get(API_URL + "auth/app/login-uri")
            .error(e -> post(err, "Failed: " + e.getMessage()))
            .submit(r -> handleLoginUri(r, t -> { setTokens(t); ok.get("Login successful!"); }, err));
    }

    private static void handleLoginUri(Http.HttpResponse r, Cons<TokenResponse> ok, Cons<String> err) {
        try {
            LoginUriResponse u = JsonIO.json.fromJson(LoginUriResponse.class, r.getResultAsString());
            if (u.loginUrl == null || u.loginId == null) { post(err, "Invalid response"); return; }
            Core.app.post(() -> Core.app.openURI(u.loginUrl));
            pollToken(u.loginId, ok, err, System.currentTimeMillis());
        } catch (Exception e) { Log.err("[Auth] URI error", e); post(err, "Error: " + e.getMessage()); }
    }

    private static void pollToken(String id, Cons<TokenResponse> ok, Cons<String> err, long start) {
        if (System.currentTimeMillis() - start > LOGIN_TIMEOUT_MS) { post(err, "Login timeout"); return; }
        Http.get(API_URL + "auth/app/login-token?loginId=" + id).timeout(10000)
            .error(e -> retryPoll(id, ok, err, start, e))
            .submit(r -> handleTokenResponse(r, id, ok, err, start));
    }

    private static void retryPoll(String id, Cons<TokenResponse> ok, Cons<String> err, long start, Throwable e) {
        if (System.currentTimeMillis() - start < LOGIN_TIMEOUT_MS) Time.runTask(60f, () -> pollToken(id, ok, err, start));
        else post(err, "Failed: " + e.getMessage());
    }

    private static void handleTokenResponse(Http.HttpResponse r, String id, Cons<TokenResponse> ok, Cons<String> err, long start) {
        try {
            TokenResponse t = JsonIO.json.fromJson(TokenResponse.class, r.getResultAsString());
            if (t.accessToken == null) { Time.runTask(60f, () -> pollToken(id, ok, err, start)); return; }
            Log.info("[Auth] Login OK"); Core.app.post(() -> ok.get(t));
        } catch (Exception e) { Log.err("[Auth] Token error", e); post(err, "Error: " + e.getMessage()); }
    }

    public static void refreshTokenAsync(Cons<Boolean> ok, Cons<String> err) {
        if (refreshToken == null || refreshToken.isEmpty()) { if (err != null) Core.app.post(() -> err.get("No refresh token available")); return; }
        if (isRefreshing) { if (ok != null) Core.app.post(() -> ok.get(null)); return; }
        isRefreshing = true; Log.info("[Auth] Refreshing token...");
        Http.post(API_URL + "auth/app/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}")
            .header("Content-Type", "application/json")
            .error(e -> { isRefreshing = false; Log.err("[Auth] Refresh failed", e); if (err != null) Core.app.post(() -> err.get("Session expired")); })
            .submit(res -> {
                isRefreshing = false;
                try { TokenResponse tokens = JsonIO.json.fromJson(TokenResponse.class, res.getResultAsString());
                    if (tokens.accessToken != null) { Log.info("[Auth] Token refreshed"); setTokens(tokens); if (ok != null) Core.app.post(() -> ok.get(true)); }
                    else { logout(); if (err != null) Core.app.post(() -> err.get("Failed to refresh")); }
                } catch (Exception e) { Log.err("[Auth] Error parsing refresh", e); logout(); if (err != null) Core.app.post(() -> err.get("Error: " + e.getMessage())); }
            });
    }

    public static void logout() { accessToken = refreshToken = null; accessTokenExpiry = 0; Core.settings.remove(ACCESS_KEY); Core.settings.remove(REFRESH_KEY); Log.info("[Auth] Tokens cleared"); onLoginStateChanged.run(); }
    public static void onLoginStateChanged(Runnable cb) { onLoginStateChanged = cb; }

    private static void setTokens(TokenResponse t) {
        accessToken = t.accessToken; if (t.refreshToken != null) refreshToken = t.refreshToken;
        accessTokenExpiry = parseExpiry(accessToken);
        if (accessToken != null) Core.settings.put(ACCESS_KEY, accessToken); else Core.settings.remove(ACCESS_KEY);
        if (refreshToken != null) Core.settings.put(REFRESH_KEY, refreshToken); else Core.settings.remove(REFRESH_KEY);
        onLoginStateChanged.run();
    }

    private static void loadTokens() {
        accessToken = Core.settings.getString(ACCESS_KEY, null); refreshToken = Core.settings.getString(REFRESH_KEY, null);
        if (accessToken != null) { accessTokenExpiry = parseExpiry(accessToken); if (isExpired(accessTokenExpiry)) { accessToken = null; accessTokenExpiry = 0; } }
        Log.info("[Auth] Loaded. Logged in: " + isLoggedIn());
    }

    private static long parseExpiry(String token) {
        if (token == null) return 0;
        try { String[] parts = token.split("\\."); if (parts.length < 2) return 0;
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            int expIndex = payload.indexOf("\"exp\""); if (expIndex == -1) return 0;
            int colonIndex = payload.indexOf(":", expIndex); int endIndex = payload.indexOf(",", colonIndex);
            if (endIndex == -1) endIndex = payload.indexOf("}", colonIndex);
            return Long.parseLong(payload.substring(colonIndex + 1, endIndex).trim()) * 1000;
        } catch (Exception e) { Log.err("[Auth] Failed to parse token expiry", e); return System.currentTimeMillis() + DEFAULT_EXPIRY_MS; }
    }

    private static boolean isExpired(long expiryMs) { return expiryMs > 0 && System.currentTimeMillis() > expiryMs; }
    private static boolean shouldRefresh(long expiryMs, long marginMs) { return expiryMs > 0 && System.currentTimeMillis() > expiryMs - marginMs; }
    private static void post(Cons<String> c, String m) { Core.app.post(() -> c.get(m)); }
}
