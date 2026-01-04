package mindustrytool.features.content.browser;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.io.JsonIO;

public final class ApiRequest {
    private ApiRequest() {
    }

    public static <T> void get(String url, Class<T> cls, Cons<T> ok) {
        request(url, r -> {
            String d = r.getResultAsString();
            if (d != null && !d.isEmpty())
                Core.app.post(() -> ok.get(JsonIO.json.fromJson(cls, d)));
            else
                Core.app.post(() -> ok.get(null));
        }, e -> Core.app.post(() -> ok.get(null)));
    }

    public static <T> void getWithError(String url, Class<T> cls, Cons<T> ok, Cons<Throwable> err) {
        request(url, r -> {
            String d = r.getResultAsString();
            Core.app.post(() -> ok.get(JsonIO.json.fromJson(cls, d)));
        }, err);
    }

    @SuppressWarnings("unchecked")
    public static <T> void getList(String url, Class<T> cls, Cons<Seq<T>> ok) {
        request(url, r -> {
            String d = r.getResultAsString();
            Core.app.post(() -> ok.get(JsonIO.json.fromJson(Seq.class, cls, d)));
        }, e -> Core.app.post(() -> ok.get(null)));
    }

    @SuppressWarnings("unchecked")
    public static <T> void getList(String url, Class<T> cls, Cons<Seq<T>> ok, Cons<Throwable> err) {
        request(url, r -> {
            String d = r.getResultAsString();
            Core.app.post(() -> ok.get(JsonIO.json.fromJson(Seq.class, cls, d)));
        }, err);
    }

    public static void getJval(String url, Cons<arc.util.serialization.Jval> ok) {
        request(url, r -> {
            String d = r.getResultAsString();
            if (d != null && !d.isEmpty())
                Core.app.post(() -> ok.get(arc.util.serialization.Jval.read(d)));
            else
                Core.app.post(() -> ok.get(null));
        }, e -> Core.app.post(() -> ok.get(null)));
    }

    private static void request(String url, Cons<arc.util.Http.HttpResponse> ok, Cons<Throwable> err) {
        AuthHttp.get(url)
                .error(e -> {
                    // Arc often throws an exception for non-2xx status codes
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    String str = e.toString();
                    boolean is401 = msg.contains("401") || str.contains("401") || msg.contains("UNAUTHORIZED")
                            || str.contains("UNAUTHORIZED");

                    if (is401) {
                        Log.info(
                                "Access Unauthorized (401) caught in error handler, invalidating token and retrying as guest...");
                        BrowserAuthService.invalidateToken();

                        // Retry as Guest
                        GuestHttp.get(url)
                                .error(e2 -> {
                                    Log.err(url + " (retry)", e2);
                                    if (err != null)
                                        Core.app.post(() -> err.get(
                                                new RuntimeException("Guest Retry Failed: " + e2.getMessage(), e2)));
                                })
                                .submit(r2 -> {
                                    if (r2.getStatus() == arc.util.Http.HttpStatus.OK) {
                                        ok.get(r2);
                                    } else {
                                        Log.info("Guest retry returned status: " + r2.getStatus());
                                        if (err != null)
                                            Core.app.post(() -> err.get(new RuntimeException(
                                                    "Guest Retry Failed with Status: " + r2.getStatus())));
                                    }
                                });
                    } else {
                        Log.err(url, e);
                        if (err != null)
                            Core.app.post(() -> err.get(e));
                    }
                })
                .submit(r -> {
                    if (r.getStatus() == arc.util.Http.HttpStatus.UNAUTHORIZED) {
                        Log.info("Access Unauthorized (401), invalidating token and retrying as guest...");
                        BrowserAuthService.invalidateToken();

                        // Retry as Guest
                        GuestHttp.get(url)
                                .error(e -> {
                                    Log.err(url + " (retry)", e);
                                    if (err != null)
                                        Core.app.post(() -> err
                                                .get(new RuntimeException("Guest Retry Failed: " + e.getMessage(), e)));
                                })
                                .submit(r2 -> {
                                    if (r2.getStatus() == arc.util.Http.HttpStatus.OK) {
                                        ok.get(r2);
                                    } else {
                                        Log.info("Guest retry returned status: " + r2.getStatus());
                                        if (err != null)
                                            Core.app.post(() -> err.get(new RuntimeException(
                                                    "Guest Retry Failed with Status: " + r2.getStatus())));
                                    }
                                });
                    } else {
                        ok.get(r);
                    }
                });
    }
}
