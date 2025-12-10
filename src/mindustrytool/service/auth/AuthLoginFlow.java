package mindustrytool.service.auth;

import arc.Core;
import arc.func.Cons;
import arc.util.*;
import mindustry.io.JsonIO;
import mindustrytool.core.config.Config;
import mindustrytool.core.model.*;

/** Handles login flow via browser and token polling. */
public final class AuthLoginFlow {
    private static final long TIMEOUT_MS = 30_000;
    private AuthLoginFlow() {}

    public static void start(Cons<TokenResponse> ok, Cons<String> err) {
        Log.info("[Auth] Starting login...");
        Http.get(Config.API_URL + "auth/app/login-uri").error(e -> post(err, "Failed: " + e.getMessage())).submit(r -> handleUri(r, ok, err));
    }

    private static void handleUri(Http.HttpResponse r, Cons<TokenResponse> ok, Cons<String> err) {
        try {
            LoginUriResponse u = JsonIO.json.fromJson(LoginUriResponse.class, r.getResultAsString());
            if (u.loginUrl() == null || u.loginId() == null) { post(err, "Invalid response"); return; }
            Core.app.post(() -> Core.app.openURI(u.loginUrl()));
            poll(u.loginId(), ok, err, System.currentTimeMillis());
        } catch (Exception e) { Log.err("[Auth] URI error", e); post(err, "Error: " + e.getMessage()); }
    }

    private static void poll(String id, Cons<TokenResponse> ok, Cons<String> err, long start) {
        if (System.currentTimeMillis() - start > TIMEOUT_MS) { post(err, "Login timeout"); return; }
        Http.get(Config.API_URL + "auth/app/login-token?loginId=" + id).timeout(10000)
            .error(e -> retry(id, ok, err, start, e)).submit(r -> handle(r, id, ok, err, start));
    }

    private static void retry(String id, Cons<TokenResponse> ok, Cons<String> err, long start, Throwable e) {
        if (System.currentTimeMillis() - start < TIMEOUT_MS) Time.runTask(60f, () -> poll(id, ok, err, start));
        else post(err, "Failed: " + e.getMessage());
    }

    private static void handle(Http.HttpResponse r, String id, Cons<TokenResponse> ok, Cons<String> err, long start) {
        try {
            TokenResponse t = JsonIO.json.fromJson(TokenResponse.class, r.getResultAsString());
            if (t.accessToken() == null) { Time.runTask(60f, () -> poll(id, ok, err, start)); return; }
            Log.info("[Auth] Login OK"); Core.app.post(() -> ok.get(t));
        } catch (Exception e) { Log.err("[Auth] Token error", e); post(err, "Error: " + e.getMessage()); }
    }

    private static void post(Cons<String> c, String m) { Core.app.post(() -> c.get(m)); }
}
