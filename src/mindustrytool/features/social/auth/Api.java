package mindustrytool.features.social.auth;

import arc.Core;
import arc.func.Cons;
import arc.util.Http;
import mindustry.io.JsonIO;

/** API client for auth-related endpoints */
public final class Api {
    private static final String API_URL = "https://api.mindustry-tool.com/api/v4/";

    private Api() {
    }

    public static void getSession(Cons<SessionData> ok, Cons<Throwable> err) {
        String token = AuthService.getAccessToken();
        if (token == null) {
            if (err != null)
                err.get(new Exception("Not logged in"));
            return;
        }

        Http.get(API_URL + "auth/session")
                .header("Authorization", "Bearer " + token)
                .error(e -> {
                    if (err != null)
                        Core.app.post(() -> err.get(e));
                })
                .submit(r -> {
                    String d = r.getResultAsString();
                    if (d != null && !d.isEmpty()) {
                        Core.app.post(() -> ok.get(JsonIO.json.fromJson(SessionData.class, d)));
                    }
                });
    }
}
