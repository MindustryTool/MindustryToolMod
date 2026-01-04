package mindustrytool.features.content.browser;

import arc.util.Http;
import arc.util.Http.HttpRequest;

/** Reusable HTTP request helpers with authentication. */
public final class AuthHttp {
    private AuthHttp() {}

    public static HttpRequest get(String url) {
        HttpRequest req = Http.get(url);
        String token = BrowserAuthService.getAccessToken();
        if (token != null) req.header("Authorization", "Bearer " + token);
        return req;
    }

    public static HttpRequest post(String url, String body) {
        HttpRequest req = Http.post(url, body);
        String token = BrowserAuthService.getAccessToken();
        if (token != null) req.header("Authorization", "Bearer " + token);
        if (body != null && !body.isEmpty()) req.header("Content-Type", "application/json");
        return req;
    }
}
