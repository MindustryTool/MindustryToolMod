package mindustrytool.data.api;

import java.net.URI;
import org.apache.http.client.utils.URIBuilder;
import arc.Core;
import arc.func.Cons;
import arc.struct.*;
import arc.util.*;
import arc.util.Http.HttpRequest;
import mindustry.io.JsonIO;
import mindustrytool.domain.service.AuthService;

public class PagingRequest<T> {
    private volatile boolean isLoading = false;
    private boolean hasMore = true, isError = false;
    private String error = "";
    private int size = 20, page = 0;
    private final String url;
    private final Class<T> clazz;
    private ObjectMap<String, String> options = new ObjectMap<>();

    public PagingRequest(Class<T> clazz, String url) { this.url = url; this.clazz = clazz; }

    public synchronized void getPage(Cons<Seq<T>> listener) {
        if (isLoading) return;
        isError = false; isLoading = true;
        try {
            URIBuilder b = new URIBuilder(url)
                .setParameter("page", String.valueOf(page))
                .setParameter("size", String.valueOf(Math.min(size, 100)));
            
            // Add sort parameter before other options
            if (options.containsKey("sort") && options.get("sort") != null && !options.get("sort").isEmpty()) {
                b.setParameter("sort", options.get("sort"));
            }
            
            // Add remaining options (excluding sort which was already added)
            for (var e : options.entries()) {
                if (!e.key.equals("sort") && e.value != null && !e.value.isEmpty()) {
                    b.setParameter(e.key, e.value);
                }
            }
            
            URI uri = b.build();
            listener.get(null);
            Log.info(uri);
            
            HttpRequest req = Http.get(uri.toString()).timeout(5000);
            String token = AuthService.getAccessToken();
            if (token != null) req.header("Authorization", "Bearer " + token);
            req.error(e -> { Log.err(uri.toString(), e); error = e.getMessage(); isLoading = false; isError = true; listener.get(null); }).submit(r -> handleResult(r, listener));
        } catch (Exception e) { Log.err(url, e); error = e.getMessage(); isLoading = false; isError = true; listener.get(null); }
    }

    @SuppressWarnings("unchecked")
    private synchronized void handleResult(Http.HttpResponse r, Cons<Seq<T>> listener) {
        isLoading = false; isError = false;
        if (r == null || r.getStatus() != Http.HttpStatus.OK) { isError = true; error = r != null ? r.getResultAsString() : "No response"; listener.get(new Seq<>()); return; }
        String data = r.getResultAsString();
        if (data == null || data.isEmpty()) { listener.get(new Seq<>()); return; }
        Core.app.post(() -> { var items = JsonIO.json.fromJson(Seq.class, clazz, data); hasMore = items != null && items.size != 0; listener.get(items != null ? items : new Seq<>()); });
    }

    public synchronized void setPage(int p) { if(p>0){page = p-1;}else{page = 0;} } public synchronized void setOptions(ObjectMap<String, String> o) { options = o; }
    public synchronized int getItemPerPage() { return size; } public synchronized void setItemPerPage(int s) { size = s; }
    public synchronized boolean hasMore() { return hasMore; } public synchronized boolean isLoading() { return isLoading; } public synchronized boolean isError() { return isError; }
    public synchronized String getError() { return error; } public synchronized int getPage() { return page; }
    public synchronized void nextPage(Cons<Seq<T>> l) { if (!isLoading && hasMore) page++; getPage(l); }
    public synchronized void previousPage(Cons<Seq<T>> l) { if (!isLoading && page > 0) page--; getPage(l); }
}
