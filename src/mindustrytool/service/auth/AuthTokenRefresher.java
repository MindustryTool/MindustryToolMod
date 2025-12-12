package mindustrytool.service.auth;

import arc.Core;
import arc.func.Cons;
import arc.util.*;
import mindustry.io.JsonIO;
import mindustrytool.core.config.Config;
import mindustrytool.core.model.TokenResponse;

/** Handles token refresh logic. */
public final class AuthTokenRefresher {
    private static boolean isRefreshing = false;

    private AuthTokenRefresher() {}

    public static void refresh(String refreshToken, Cons<TokenResponse> onSuccess, Cons<String> onError) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            if (onError != null) Core.app.post(() -> onError.get("No refresh token available"));
            return;
        }
        if (isRefreshing) { if (onSuccess != null) Core.app.post(() -> onSuccess.get(null)); return; }
        isRefreshing = true;
        Log.info("[Auth] Refreshing token...");
        Http.post(Config.API_URL + "auth/app/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}")
            .header("Content-Type", "application/json")
            .error(e -> { isRefreshing = false; Log.err("[Auth] Refresh failed", e); if (onError != null) Core.app.post(() -> onError.get("Session expired")); })
            .submit(res -> handleResponse(res, onSuccess, onError));
    }

    private static void handleResponse(Http.HttpResponse res, Cons<TokenResponse> onSuccess, Cons<String> onError) {
        isRefreshing = false;
        try {
            TokenResponse tokens = JsonIO.json.fromJson(TokenResponse.class, res.getResultAsString());
            if (tokens.accessToken() != null) {
                Log.info("[Auth] Token refreshed");
                if (onSuccess != null) Core.app.post(() -> onSuccess.get(tokens));
            } else if (onError != null) Core.app.post(() -> onError.get("Failed to refresh"));
        } catch (Exception e) {
            Log.err("[Auth] Error parsing refresh", e);
            if (onError != null) Core.app.post(() -> onError.get("Error: " + e.getMessage()));
        }
    }

    public static boolean isRefreshing() { return isRefreshing; }
}
