package mindustrytool.data.api;

import arc.util.Http;
import arc.util.Http.HttpRequest;
import mindustrytool.service.auth.AuthService;

/** Reusable HTTP request helpers with authentication. */
public final class AuthHttp {
    private AuthHttp() {}

    public static HttpRequest get(String url) {
        HttpRequest req = Http.get(url);
        String token = AuthService.getAccessToken();
        if (token != null) req.header("Authorization", "Bearer " + token);
        return req;
    }

    public static HttpRequest post(String url, String body) {
        HttpRequest req = Http.post(url, body);
        String token = AuthService.getAccessToken();
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null && !body.isEmpty()) req.header("Content-Type", "application/json");
        return req;
    }
}
