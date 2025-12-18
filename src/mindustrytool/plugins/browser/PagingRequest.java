package mindustrytool.plugins.browser;

import arc.Core;
import arc.func.Cons;
import arc.struct.*;
import arc.util.*;

import mindustry.io.JsonIO;

public class PagingRequest<T> {
    private volatile boolean isLoading = false;
    private boolean hasMore = true, isError = false;
    private String error = "";
    private int size = 20, page = 0;
    private final String url;
    private final Class<T> clazz;
    private ObjectMap<String, String> options = new ObjectMap<>();

    public PagingRequest(Class<T> clazz, String url) {
        this.url = url;
        this.clazz = clazz;
    }

    public synchronized void getPage(Cons<Seq<T>> l) {
        if (isLoading)
            return;
        isError = false;
        isLoading = true;
        try {
            String uri = buildUri();
            l.get(null);
            Log.info(uri);
            AuthHttp.get(uri).timeout(5000)
                    .error(e -> {
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        String str = e.toString();
                        boolean is401 = msg.contains("401") || str.contains("401") || msg.contains("UNAUTHORIZED")
                                || str.contains("UNAUTHORIZED");

                        if (is401) {
                            Log.info(
                                    "Access Unauthorized (401) caught in error handler (Paging), invalidating token and retrying as guest...");
                            BrowserAuthService.invalidateToken();

                            // Retry as Guest
                            GuestHttp.get(uri).timeout(5000)
                                    .error(e2 -> {
                                        Log.err(uri + " (retry)", e2);
                                        error = "Guest Retry Failed: " + e2.getMessage();
                                        isLoading = false;
                                        isError = true;
                                        l.get(null);
                                    })
                                    .submit(r2 -> {
                                        if (r2.getStatus() == Http.HttpStatus.OK) {
                                            handleResult(r2, l);
                                        } else {
                                            Log.info("Guest retry returned status: " + r2.getStatus());
                                            error = "Guest Retry Failed with Status: " + r2.getStatus();
                                            isLoading = false;
                                            isError = true;
                                            l.get(null);
                                        }
                                    });
                        } else {
                            Log.err(uri, e);
                            error = e.getMessage();
                            isLoading = false;
                            isError = true;
                            l.get(null);
                        }
                    })
                    .submit(r -> {
                        if (r.getStatus() == Http.HttpStatus.UNAUTHORIZED) {
                            Log.info("Access Unauthorized (401) (Paging), invalidating token and retrying as guest...");
                            BrowserAuthService.invalidateToken();

                            // Retry as Guest
                            GuestHttp.get(uri).timeout(5000)
                                    .error(e -> {
                                        Log.err(uri + " (retry)", e);
                                        error = "Guest Retry Failed: " + e.getMessage();
                                        isLoading = false;
                                        isError = true;
                                        l.get(null);
                                    })
                                    .submit(r2 -> {
                                        if (r2.getStatus() == Http.HttpStatus.OK) {
                                            handleResult(r2, l);
                                        } else {
                                            Log.info("Guest retry returned status: " + r2.getStatus());
                                            error = "Guest Retry Failed with Status: " + r2.getStatus();
                                            isLoading = false;
                                            isError = true;
                                            l.get(null);
                                        }
                                    });
                        } else {
                            handleResult(r, l);
                        }
                    });
        } catch (Exception e) {
            Log.err(url, e);
            error = e.getMessage();
            isLoading = false;
            isError = true;
            l.get(null);
        }
    }

    private String buildUri() {
        StringBuilder sb = new StringBuilder(url);
        sb.append("?page=").append(page);
        sb.append("&size=").append(Math.min(size, 100));
        String sort = options.get("sort");
        if (sort != null && !sort.isEmpty())
            sb.append("&sort=").append(Strings.encode(sort));
        for (ObjectMap.Entry<String, String> e : options.entries()) {
            if (!e.key.equals("sort") && e.value != null && !e.value.isEmpty()) {
                sb.append("&").append(Strings.encode(e.key)).append("=").append(Strings.encode(e.value));
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private synchronized void handleResult(Http.HttpResponse r, Cons<Seq<T>> l) {
        isLoading = false;
        isError = false;
        if (r == null || r.getStatus() != Http.HttpStatus.OK) {
            isError = true;
            error = r != null ? r.getResultAsString() : "No response";
            l.get(new Seq<>());
            return;
        }
        String d = r.getResultAsString();
        if (d == null || d.isEmpty()) {
            l.get(new Seq<>());
            return;
        }
        Core.app.post(() -> {
            Seq<T> items = JsonIO.json.fromJson(Seq.class, clazz, d);
            hasMore = items != null && items.size != 0;
            l.get(items != null ? items : new Seq<>());
        });
    }

    public synchronized void setPage(int p) {
        page = p;
    }

    public synchronized void setOptions(ObjectMap<String, String> o) {
        options = o;
    }

    public synchronized void setItemPerPage(int s) {
        size = s;
    }

    public synchronized int getItemPerPage() {
        return size;
    }

    public synchronized int getPage() {
        return page;
    }

    public synchronized boolean hasMore() {
        return hasMore;
    }

    public synchronized boolean isLoading() {
        return isLoading;
    }

    public synchronized boolean isError() {
        return isError;
    }

    public synchronized String getError() {
        return error;
    }

    public synchronized void nextPage(Cons<Seq<T>> l) {
        if (!isLoading && hasMore)
            page++;
        getPage(l);
    }

    public synchronized void previousPage(Cons<Seq<T>> l) {
        if (!isLoading && page > 0)
            page--;
        getPage(l);
    }
}
