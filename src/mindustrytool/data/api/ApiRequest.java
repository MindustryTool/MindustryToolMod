package mindustrytool.data.api;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.io.JsonIO;

public final class ApiRequest {
    private ApiRequest() {}

    public static <T> void get(String url, Class<T> cls, Cons<T> ok) {
        AuthHttp.get(url).submit(r -> {
            String d = r.getResultAsString();
            if (d != null && !d.isEmpty()) Core.app.post(() -> ok.get(JsonIO.json.fromJson(cls, d)));
        });
    }

    public static <T> void getWithError(String url, Class<T> cls, Cons<T> ok, Cons<Throwable> err) {
        AuthHttp.get(url).error(err).submit(r -> {
            String d = r.getResultAsString();
            Core.app.post(() -> ok.get(JsonIO.json.fromJson(cls, d)));
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> void getList(String url, Class<T> cls, Cons<Seq<T>> ok) {
        AuthHttp.get(url).submit(r -> {
            String d = r.getResultAsString();
            Core.app.post(() -> ok.get(JsonIO.json.fromJson(Seq.class, cls, d)));
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> void getList(String url, Class<T> cls, Cons<Seq<T>> ok, Cons<Throwable> err) {
        AuthHttp.get(url).error(e -> { Log.err(url, e); if (err != null) err.get(e); })
            .submit(r -> { String d = r.getResultAsString(); Core.app.post(() -> ok.get(JsonIO.json.fromJson(Seq.class, cls, d))); });
    }
}
