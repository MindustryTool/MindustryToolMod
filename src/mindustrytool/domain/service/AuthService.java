package mindustrytool.domain.service;

import arc.Core;
import arc.func.Cons;
import arc.util.*;
import mindustry.io.JsonIO;
import mindustrytool.core.config.Config;
import mindustrytool.core.model.*;

/**
 * Authentication service that manages JWT tokens for API requests.
 * Handles login, token storage, auto-refresh, and request authorization.
 */
public class AuthService {
    private static final String ACCESS_TOKEN_KEY = "mindustrytool.auth.accessToken";
    private static final String REFRESH_TOKEN_KEY = "mindustrytool.auth.refreshToken";
    private static final long REFRESH_MARGIN_MS = 60 * 1000; // Refresh 1 minute before expiry
    private static final long LOGIN_TIMEOUT_MS = 30 * 1000; // 30 seconds timeout for login

    private static String accessToken = null;
    private static String refreshToken = null;
    private static long accessTokenExpiry = 0;
    private static boolean isRefreshing = false;
    private static Runnable onLoginStateChanged = () -> {};

    static {
        loadTokens();
    }

    /** Check if user is logged in */
    public static boolean isLoggedIn() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /** Get current access token, refreshing if needed */
    public static String getAccessToken() {
        // If token is expired (not just near expiry), don't return it
        if (accessToken != null && accessTokenExpiry > 0 && System.currentTimeMillis() > accessTokenExpiry) {
            Log.info("[Auth] Access token expired, will try refresh on next request");
            if (refreshToken != null && !refreshToken.isEmpty()) {
                refreshTokenAsync(null, null);
            }
            return null; // Don't return expired token
        }
        // If token is near expiry, trigger refresh but still return current token
        if (accessToken != null && shouldRefresh()) {
            refreshTokenAsync(null, null);
        }
        return accessToken;
    }

    /** Get Authorization header value */
    public static String getAuthHeader() {
        String token = getAccessToken();
        return token != null ? "Bearer " + token : null;
    }

    /** Start login flow */
    public static void login(Cons<String> onSuccess, Cons<String> onError) {
        Log.info("[Auth] Starting login flow...");
        
        // Step 1: Get login URI
        Http.get(Config.API_URL + "auth/app/login-uri")
            .error(e -> {
                Log.err("[Auth] Failed to get login URI", e);
                Core.app.post(() -> onError.get("Failed to get login URL: " + e.getMessage()));
            })
            .submit(response -> {
                try {
                    String json = response.getResultAsString();
                    LoginUriResponse loginUri = JsonIO.json.fromJson(LoginUriResponse.class, json);
                    
                    if (loginUri.loginUrl() == null || loginUri.loginId() == null) {
                        Core.app.post(() -> onError.get("Invalid login response from server"));
                        return;
                    }

                    Log.info("[Auth] Opening login URL: " + loginUri.loginUrl());
                    
                    // Step 2: Open browser for user to login
                    Core.app.post(() -> Core.app.openURI(loginUri.loginUrl()));
                    
                    // Step 3: Poll for token with timeout
                    pollForToken(loginUri.loginId(), onSuccess, onError, System.currentTimeMillis());
                    
                } catch (Exception e) {
                    Log.err("[Auth] Error parsing login URI response", e);
                    Core.app.post(() -> onError.get("Error: " + e.getMessage()));
                }
            });
    }

    /** Poll for token after user logs in via browser */
    private static void pollForToken(String loginId, Cons<String> onSuccess, Cons<String> onError, long startTime) {
        if (System.currentTimeMillis() - startTime > LOGIN_TIMEOUT_MS) {
            Core.app.post(() -> onError.get("Login timeout. Please try again."));
            return;
        }

        Http.get(Config.API_URL + "auth/app/login-token?loginId=" + loginId)
            .timeout(10000)
            .error(e -> {
                // Retry after 1 second if not timed out
                if (System.currentTimeMillis() - startTime < LOGIN_TIMEOUT_MS) {
                    Time.runTask(60f, () -> pollForToken(loginId, onSuccess, onError, startTime));
                } else {
                    Core.app.post(() -> onError.get("Login failed: " + e.getMessage()));
                }
            })
            .submit(response -> {
                try {
                    String json = response.getResultAsString();
                    TokenResponse tokens = JsonIO.json.fromJson(TokenResponse.class, json);
                    
                    if (tokens.accessToken() == null) {
                        // Token not ready yet, retry
                        Time.runTask(60f, () -> pollForToken(loginId, onSuccess, onError, startTime));
                        return;
                    }

                    // Success! Save tokens
                    setTokens(tokens.accessToken(), tokens.refreshToken());
                    Log.info("[Auth] Login successful!");
                    Core.app.post(() -> onSuccess.get("Login successful!"));
                    
                } catch (Exception e) {
                    Log.err("[Auth] Error parsing token response", e);
                    Core.app.post(() -> onError.get("Error: " + e.getMessage()));
                }
            });
    }

    /** Refresh the access token using refresh token */
    public static void refreshTokenAsync(Cons<Boolean> onComplete, Cons<String> onError) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            if (onError != null) Core.app.post(() -> onError.get("No refresh token available"));
            return;
        }

        if (isRefreshing) {
            if (onComplete != null) Core.app.post(() -> onComplete.get(false));
            return;
        }

        isRefreshing = true;
        Log.info("[Auth] Refreshing token...");

        String body = "{\"refreshToken\":\"" + refreshToken + "\"}";
        
        Http.post(Config.API_URL + "auth/app/refresh", body)
            .header("Content-Type", "application/json")
            .error(e -> {
                isRefreshing = false;
                Log.err("[Auth] Failed to refresh token", e);
                // Clear tokens on refresh failure
                logout();
                if (onError != null) Core.app.post(() -> onError.get("Session expired. Please login again."));
            })
            .submit(response -> {
                isRefreshing = false;
                try {
                    String json = response.getResultAsString();
                    TokenResponse tokens = JsonIO.json.fromJson(TokenResponse.class, json);
                    
                    if (tokens.accessToken() != null) {
                        setTokens(tokens.accessToken(), tokens.refreshToken());
                        Log.info("[Auth] Token refreshed successfully");
                        if (onComplete != null) Core.app.post(() -> onComplete.get(true));
                    } else {
                        logout();
                        if (onError != null) Core.app.post(() -> onError.get("Failed to refresh token"));
                    }
                } catch (Exception e) {
                    Log.err("[Auth] Error parsing refresh response", e);
                    logout();
                    if (onError != null) Core.app.post(() -> onError.get("Error: " + e.getMessage()));
                }
            });
    }

    /** Logout and clear all tokens */
    public static void logout() {
        accessToken = null;
        refreshToken = null;
        accessTokenExpiry = 0;
        saveTokens();
        onLoginStateChanged.run();
        Log.info("[Auth] Logged out");
    }

    /** Set callback for login state changes */
    public static void onLoginStateChanged(Runnable callback) {
        onLoginStateChanged = callback;
    }

    /** Set tokens and calculate expiry */
    private static void setTokens(String access, String refresh) {
        accessToken = access;
        if (refresh != null) {
            refreshToken = refresh;
        }
        accessTokenExpiry = parseTokenExpiry(access);
        saveTokens();
        onLoginStateChanged.run();
    }

    /** Parse JWT token to get expiry time */
    private static long parseTokenExpiry(String token) {
        if (token == null) return 0;
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return 0;
            
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            // Simple parsing for "exp" field
            int expIndex = payload.indexOf("\"exp\"");
            if (expIndex == -1) return 0;
            
            int colonIndex = payload.indexOf(":", expIndex);
            int endIndex = payload.indexOf(",", colonIndex);
            if (endIndex == -1) endIndex = payload.indexOf("}", colonIndex);
            
            String expStr = payload.substring(colonIndex + 1, endIndex).trim();
            return Long.parseLong(expStr) * 1000; // Convert to milliseconds
        } catch (Exception e) {
            Log.err("[Auth] Failed to parse token expiry", e);
            return System.currentTimeMillis() + 3600 * 1000; // Default 1 hour
        }
    }

    /** Check if token should be refreshed */
    private static boolean shouldRefresh() {
        return accessTokenExpiry > 0 && System.currentTimeMillis() > accessTokenExpiry - REFRESH_MARGIN_MS;
    }

    /** Save tokens to settings */
    private static void saveTokens() {
        if (accessToken != null) {
            Core.settings.put(ACCESS_TOKEN_KEY, accessToken);
        } else {
            Core.settings.remove(ACCESS_TOKEN_KEY);
        }
        if (refreshToken != null) {
            Core.settings.put(REFRESH_TOKEN_KEY, refreshToken);
        } else {
            Core.settings.remove(REFRESH_TOKEN_KEY);
        }
    }

    /** Load tokens from settings */
    private static void loadTokens() {
        accessToken = Core.settings.getString(ACCESS_TOKEN_KEY, null);
        refreshToken = Core.settings.getString(REFRESH_TOKEN_KEY, null);
        if (accessToken != null) {
            accessTokenExpiry = parseTokenExpiry(accessToken);
            // Check if access token is expired but refresh token might still be valid
            if (System.currentTimeMillis() > accessTokenExpiry) {
                accessToken = null;
                if (refreshToken != null) {
                    // Try to refresh on next API call
                    accessTokenExpiry = 0;
                }
            }
        }
        Log.info("[Auth] Loaded tokens. Logged in: " + isLoggedIn());
    }
}
