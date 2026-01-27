package mindustrytool.features.auth;

import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import arc.Core;
import arc.Events;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.auth.dto.LoginEvent;
import mindustrytool.features.auth.dto.LogoutEvent;
import mindustrytool.features.auth.dto.UserSession;
import mindustrytool.services.ReactiveStore;
import arc.util.Http.HttpStatusException;

public class AuthService {
    private static AuthService instance;

    public static final String KEY_ACCESS_TOKEN = "mindustrytool.auth.accessToken";
    public static final String KEY_REFRESH_TOKEN = "mindustrytool.auth.refreshToken";
    public static final String KEY_LOGIN_ID = "mindustrytool.auth.loginId";

    public final ReactiveStore<UserSession> sessionStore;

    private CompletableFuture<Boolean> refreshFuture;
    private CompletableFuture<Void> loginFuture;
    private BaseDialog loginDialog;

    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    private AuthService() {
        sessionStore = new ReactiveStore<>(() -> {
            CompletableFuture<String> future = new CompletableFuture<>();

            AuthHttp.get(Config.API_v4_URL + "auth/session", res -> future.complete(res.getResultAsString()),
                    err -> future.completeExceptionally(err));

            return future.thenApply(json -> json.isEmpty() ? null : Utils.fromJson(UserSession.class, json));
        });

        sessionStore.subscribe((value, state, error) -> {
            if (state == ReactiveStore.LoadState.SUCCESS && value != null) {
                Events.fire(value);
            }
        });

        String logindId = Core.settings.getString(KEY_LOGIN_ID);

        if (logindId == null) {
            return;
        }

        pollLoginToken(logindId).exceptionally(e -> {
            Log.err("Background login polling failed", e);
            return null;
        });
    }

    public boolean isLoggedIn() {
        return sessionStore.getValue() != null && Core.settings.has(KEY_ACCESS_TOKEN)
                && Core.settings.has(KEY_REFRESH_TOKEN);
    }

    public synchronized CompletableFuture<Void> login() {
        if (loginFuture != null && !loginFuture.isDone()) {
            return loginFuture;
        }

        loginFuture = new CompletableFuture<>();

        if (loginDialog == null) {
            loginDialog = new BaseDialog("@login");
        }

        loginDialog.name = "loginDialog";

        loginDialog.buttons.button("@cancel", () -> {
            if (!loginFuture.isDone()) {
                loginFuture.completeExceptionally(new RuntimeException("Login cancelled"));
            }
            loginDialog.hide();
        }).width(230);

        loginDialog.cont.add("@generate-loading-link");

        Http.get(Config.API_v4_URL + "auth/app/login-uri")
                .timeout(10000)
                .error(err -> {
                    Core.app.post(() -> {
                        loginDialog.hide();
                        loginFuture.completeExceptionally(new RuntimeException("Failed to get login URI", err));
                    });
                })
                .submit(res -> {
                    try {
                        Jval json = Jval.read(res.getResultAsString());

                        String loginUrl = json.getString("loginUrl");
                        String loginId = json.getString("loginId");

                        loginDialog.cont.clear();
                        loginDialog.cont.button(loginUrl, () -> {
                            Core.app.setClipboardText(loginUrl);
                            Vars.ui.showInfoFade("@copied");
                        }).margin(40).growX().wrapLabel(true);

                        Core.settings.put(KEY_LOGIN_ID, loginId);

                        // Start polling for token
                        pollLoginToken(loginId).whenComplete((v, e) -> {
                            if (e != null) {
                                loginFuture.completeExceptionally(e);
                            } else {
                                loginFuture.complete(null);
                            }
                            Core.app.post(() -> loginDialog.hide());
                        });

                        if (!Core.app.openURI(loginUrl)) {
                            Core.app.setClipboardText(loginUrl);
                        }

                    } catch (Exception e) {
                        loginFuture.completeExceptionally(new RuntimeException("Failed to start login flow", e));
                        Core.app.post(() -> loginDialog.hide());
                    }
                });

        return loginFuture;
    }

    private CompletableFuture<Void> pollLoginToken(String loginId) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Http.get(Config.API_v4_URL + "auth/app/login-token?loginId=" + loginId)
                .error(e -> {
                    if (e instanceof SocketTimeoutException) {
                        future.completeExceptionally(e);
                        return;
                    }

                    Core.settings.remove(KEY_LOGIN_ID);
                    future.completeExceptionally(new RuntimeException("Failed to get login token", e));
                })
                .timeout(15000)
                .submit(res -> {
                    Core.settings.remove(KEY_LOGIN_ID);

                    Jval json = Jval.read(res.getResultAsString());

                    if (json.has("accessToken") && json.has("refreshToken")) {
                        String accessToken = json.getString("accessToken");
                        String refreshToken = json.getString("refreshToken");

                        saveTokens(accessToken, refreshToken);

                        sessionStore.fetch().whenComplete((v, e) -> {
                            if (e != null) {
                                future.completeExceptionally(e);
                            } else {
                                Core.app.post(() -> Events.fire(new LoginEvent()));
                                future.complete(null);
                            }
                        });
                    } else {
                        future.completeExceptionally(new RuntimeException("Invalid response: missing tokens"));
                    }
                });
        return future;
    }

    public void saveTokens(String accessToken, String refreshToken) {
        Core.settings.put(KEY_ACCESS_TOKEN, accessToken);
        Core.settings.put(KEY_REFRESH_TOKEN, refreshToken);
        Core.settings.forceSave();
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
                    .error(err -> {
                        Core.app.post(() -> Vars.ui.showInfo("Logout failed: " + err.getMessage()));
                    })
                    .submit(res -> {
                        Core.app.post(() -> Vars.ui.showInfoFade("Logout successful!"));
                    });
        }

        Core.settings.remove(KEY_ACCESS_TOKEN);
        Core.settings.remove(KEY_REFRESH_TOKEN);
        Core.settings.remove(KEY_LOGIN_ID);

        sessionStore.fetch();

        Events.fire(new LogoutEvent());

        Log.info("Logged out");
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

    public synchronized CompletableFuture<Boolean> refreshTokenIfNeeded() {
        if (refreshFuture != null && !refreshFuture.isDone()) {
            return refreshFuture;
        }

        refreshFuture = new CompletableFuture<>();

        String accessToken = getAccessToken();
        String refreshToken = getRefreshToken();

        if (refreshToken == null) {
            refreshFuture.complete(false);
            return refreshFuture;
        }

        if (accessToken != null && !isTokenNearExpiry(accessToken)) {
            refreshFuture.complete(false);
            return refreshFuture;
        }

        Jval json = Jval.newObject();

        json.put("refreshToken", refreshToken);

        Http.post(Config.API_v4_URL + "auth/app/refresh", json.toString())
                .header("Content-Type", "application/json")
                .timeout(10000)
                .error(err -> {
                    Log.err("Failed to refresh token", err);

                    // If refresh failed (e.g. 401), logout

                    if (err instanceof HttpStatusException httpError) {
                        if (httpError.status.code == 401) {
                            Core.settings.remove(KEY_ACCESS_TOKEN);
                            Core.settings.remove(KEY_REFRESH_TOKEN);
                            Log.info("Remove tokens");
                        }

                        Log.info(httpError.response.getResultAsString());
                    }
                    refreshFuture.completeExceptionally(err);
                })
                .submit(res -> {
                    try {
                        String str = res.getResultAsString();
                        Jval resJson = Jval.read(str);
                        if (resJson.has("accessToken") && resJson.has("refreshToken")) {
                            saveTokens(resJson.getString("accessToken"), resJson.getString("refreshToken"));

                            Log.info("Token refreshed successfully");
                            refreshFuture.complete(true);
                        } else {
                            refreshFuture.completeExceptionally(
                                    new RuntimeException("Invalid refresh response: " + resJson));
                        }
                    } catch (Exception e) {
                        if (e instanceof HttpStatusException httpError) {
                            if (httpError.status.code == 401) {
                                logout();
                            }
                        }
                        refreshFuture
                                .completeExceptionally(new RuntimeException("Failed to refresh token: exception", e));
                    }
                });

        return refreshFuture;
    }
}
