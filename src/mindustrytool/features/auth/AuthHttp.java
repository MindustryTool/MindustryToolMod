package mindustrytool.features.auth;

import java.util.HashMap;
import java.util.Map;

import arc.func.Cons;
import arc.func.ConsT;
import arc.util.Http;
import arc.util.Http.HttpMethod;
import arc.util.Http.HttpRequest;
import arc.util.Http.HttpResponse;

public class AuthHttp {

    public static AuthRequest get(String url) {
        return new AuthRequest(url, HttpMethod.GET);
    }

    public static AuthRequest post(String url, String content) {
        return new AuthRequest(url, HttpMethod.POST).content(content);
    }

    // Convenient overload for callback style
    public static void get(String url, ConsT<HttpResponse, Exception> success, Cons<Throwable> failure) {
        get(url).submit(success, failure);
    }

    public static class AuthRequest {
        String url;
        HttpMethod method;
        String content;
        Map<String, String> headers = new HashMap<>();
        Cons<Throwable> error;

        public AuthRequest(String url, HttpMethod method) {
            this.url = url;
            this.method = method;
        }

        public AuthRequest content(String content) {
            this.content = content;
            return this;
        }

        public AuthRequest header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public AuthRequest error(Cons<Throwable> error) {
            this.error = error;
            return this;
        }

        public void submit(ConsT<HttpResponse, Exception> listener) {
            submit(listener, error);
        }

        public void submit(ConsT<HttpResponse, Exception> listener, Cons<Throwable> errorListener) {
            AuthService.getInstance().refreshTokenIfNeeded()
                    .thenRun(() -> {

                        HttpRequest req;
                        if (method == HttpMethod.GET) {
                            req = Http.get(url);
                        } else {
                            req = Http.post(url, content);
                        }

                        String token = AuthService.getInstance().getAccessToken();

                        if (token != null) {
                            req.header("Authorization", "Bearer " + token);
                        }

                        headers.forEach(req::header);

                        if (errorListener != null)
                            req.error(errorListener);

                        req.submit(listener);

                    })
                    .exceptionally(e -> {
                        if (errorListener != null)
                            errorListener.get(e);
                        return null;
                    });
        }
    }
}
