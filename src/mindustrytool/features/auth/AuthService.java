package mindustrytool.features.auth;

import java.net.SocketTimeoutException;
import java.util.Base64;

import arc.Core;
import arc.Events;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustrytool.Config;
import mindustrytool.features.auth.dto.UserSession;
import arc.util.Http.HttpStatusException;

public class AuthService {
    private static AuthService instance;

    public static final String KEY_ACCESS_TOKEN = "mindustrytool.auth.accessToken";
    public static final String KEY_REFRESH_TOKEN = "mindustrytool.auth.refreshToken";
    public static final String KEY_LOGIN_ID = "mindustrytool.auth.loginId";

    private UserSession currentUser;
    private boolean isRefreshing = false;

    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    private AuthService() {
        String logindId = Core.settings.getString(KEY_LOGIN_ID);

        if (logindId == null) {
            return;
        }

        pollLoginToken(logindId);

    }

    public boolean isLoggedIn() {
        return Core.settings.has(KEY_ACCESS_TOKEN) && Core.settings.has(KEY_REFRESH_TOKEN);
    }

    public void login(Runnable onSuccess, Runnable onFailure) {
        Http.get(Config.API_v4_URL + "auth/app/login-uri")
                .error(err -> {
                    Log.err("Failed to get login URI", err);
                    if (onFailure != null)
                        onFailure.run();
                })
                .submit(res -> {
                    try {
                        Jval json = Jval.read(res.getResultAsString());
                        String loginUrl = json.getString("loginUrl");
                        String loginId = json.getString("loginId");

                        Core.settings.put(KEY_LOGIN_ID, loginId);

                        // Start polling for token
                        pollLoginToken(loginId);

                        // Open browser
                        if (!Core.app.openURI(loginUrl)) {
                            Core.app.setClipboardText(loginUrl);
                        }

                    } catch (Exception e) {
                        Log.err("Failed to start login flow", e);
                        if (onFailure != null)
                            onFailure.run();
                    }
                });
    }

    private void pollLoginToken(String loginId) {
        Http.get(Config.API_v4_URL + "auth/app/login-token?loginId=" + loginId)
                .error(e -> {
                    if (e instanceof SocketTimeoutException) {
                        return;
                    }
                    Log.err("Failed to get login token", e);
                    Core.settings.remove(KEY_LOGIN_ID);
                }) // Ignore errors while polling (404/400 expected until user logs in)
                .timeout(15000)
                .submit(res -> {
                    Core.settings.remove(KEY_LOGIN_ID);

                    try {
                        Jval json = Jval.read(res.getResultAsString());
                        if (json.has("accessToken") && json.has("refreshToken")) {
                            String accessToken = json.getString("accessToken");
                            String refreshToken = json.getString("refreshToken");

                            saveTokens(accessToken, refreshToken);

                            fetchUserSession(() -> {
                            }, () -> {
                            });
                        }
                    } catch (Exception e) {
                        Log.err("Failed to parse login token response", e);
                    }
                });

    }

    public void saveTokens(String accessToken, String refreshToken) {
        Core.settings.put(KEY_ACCESS_TOKEN, accessToken);
        Core.settings.put(KEY_REFRESH_TOKEN, refreshToken);
    }

    public void logout() {
        String accessToken = Core.settings.getString(KEY_ACCESS_TOKEN, "");
        String refreshToken = Core.settings.getString(KEY_REFRESH_TOKEN, "");

        if (!accessToken.isEmpty() && !refreshToken.isEmpty()) {
            Jval json = Jval.newObject();
            json.put("accessToken", accessToken);
            json.put("refreshToken", refreshToken);

            Http.post(Config.API_v4_URL + "auth/app/logout", json.toString())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .submit(res -> {
                        Vars.ui.showInfoFade("Logout successful!");
                    });
        }

        Core.settings.remove(KEY_ACCESS_TOKEN);
        Core.settings.remove(KEY_REFRESH_TOKEN);
        Core.settings.remove(KEY_LOGIN_ID);
        currentUser = null;

        Log.info("Logged out");
    }

    public void fetchUserSession(Runnable onSuccess, Runnable onFailure) {
        AuthHttp.get(Config.API_v4_URL + "auth/session", res -> {
            try {
                Jval json = Jval.read(res.getResultAsString());
                currentUser = new UserSession(json.getString("name", "Unknown"), json.getString("imageUrl", ""));

                Events.fire(currentUser);
                onSuccess.run();
            } catch (Exception e) {
                Log.err("Failed to parse user session", e);
                onFailure.run();
            }
        }, err -> {
            Log.err("Failed to fetch user session", err);
            onFailure.run();
        });
    }

    public UserSession getCurrentUser() {
        return currentUser;
    }

    public String getAccessToken() {
        return Core.settings.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return Core.settings.getString(KEY_REFRESH_TOKEN, null);
    }

    public boolean isTokenNearExpiry(String token) {
        if (token == null) {
            return true;
        }

        try {
            String[] parts = token.split("\\.");

            if (parts.length < 2)
                return true;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Jval json = Jval.read(payload);
            long exp = json.getLong("exp", 0);
            long now = System.currentTimeMillis() / 1000;

            // "near expire (1 min)" -> 60 seconds
            return (exp - now) < 60;
        } catch (Exception e) {
            Log.err("Failed to parse token expiry", e);
            return true;
        }
    }

    public void refreshTokenIfNeeded(Runnable onSuccess, Runnable onFailure) {
        String accessToken = getAccessToken();
        String refreshToken = getRefreshToken();

        if (accessToken == null || refreshToken == null) {
            if (onFailure != null) {
                Log.err("No access token or refresh token found");
                onFailure.run();
            }
            return;
        }

        if (!isTokenNearExpiry(accessToken)) {
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }

        if (isRefreshing) {
            // Simple debounce/wait could be implemented here, but for now just fail or
            // wait?
            // We'll just proceed to avoid complex queueing, assuming single threaded main
            // loop mostly
        }

        isRefreshing = true;
        Jval json = Jval.newObject();

        json.put("refreshToken", refreshToken);

        Http.post(Config.API_v4_URL + "auth/app/refresh", json.toString())
                .header("Content-Type", "application/json")
                .error(err -> {
                    Log.err("Failed to refresh token", err);

                    isRefreshing = false;
                    // If refresh failed (e.g. 401), logout
                    if (onFailure != null) {
                        onFailure.run();
                    }

                    if (err instanceof HttpStatusException httpError) {
                        if (httpError.status.code == 401) {
                            Core.settings.remove(KEY_ACCESS_TOKEN);
                            Core.settings.remove(KEY_REFRESH_TOKEN);
                        }

                        Log.info(httpError.response.getResultAsString());
                    }
                })
                .submit(res -> {
                    isRefreshing = false;
                    try {
                        Jval resJson = Jval.read(res.getResultAsString());
                        if (resJson.has("accessToken") && resJson.has("refreshToken")) {
                            saveTokens(resJson.getString("accessToken"), resJson.getString("refreshToken"));

                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                        } else {
                            if (onFailure != null) {
                                onFailure.run();
                            }
                            logout();

                            Log.err("Failed to refresh token: response does not contain accessToken or refreshToken");
                        }
                    } catch (Exception e) {
                        if (onFailure != null) {
                            onFailure.run();
                        }
                        logout();
                        Log.err("Failed to refresh token: exception", e);
                    }
                });
    }
}
