package mindustrytool.services;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import arc.util.Nullable;
import mindustrytool.services.auth.AuthProvider;
import mindustrytool.utils.JsonUtils;

public final class Request {

    private static final AtomicLong THREAD_ID = new AtomicLong(0);
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("Request-Worker-" + THREAD_ID.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final Duration timeout;
    private final AuthProvider authProvider;
    private final Map<String, String> headers;

    private Request(String baseUrl, Duration timeout, AuthProvider authProvider, Map<String, String> headers) {
        this.baseUrl = baseUrl;
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
        this.authProvider = authProvider;
        this.headers = headers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String baseUrl;
        private Duration timeout;
        private AuthProvider authProvider;
        private final Map<String, String> headers = new LinkedHashMap<>();

        public Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder authProvider(AuthProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Request build() {
            return new Request(baseUrl, timeout, authProvider, headers);
        }
    }

    // ─── HTTP verbs ────────────────────────────────────────────────

    public RequestBuilder get(String url) {
        return new RequestBuilder(this, "GET", url);
    }

    public RequestBuilder post(String url) {
        return new RequestBuilder(this, "POST", url);
    }

    public RequestBuilder put(String url) {
        return new RequestBuilder(this, "PUT", url);
    }

    public RequestBuilder delete(String url) {
        return new RequestBuilder(this, "DELETE", url);
    }

    // ─── Response & Handlers ───────────────────────────────────────

    public static final class Response<T> {
        private final int statusCode;
        private final Map<String, List<String>> headers;
        private final T body;

        public Response(int statusCode, Map<String, List<String>> headers, T body) {
            this.statusCode = statusCode;
            this.headers = headers != null ? headers : Collections.emptyMap();
            this.body = body;
        }

        public int statusCode() {
            return statusCode;
        }

        public T body() {
            return body;
        }

        public Map<String, List<String>> headers() {
            return headers;
        }

        public String header(String name) {
            if (headers == null || name == null)
                return null;
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (name.equalsIgnoreCase(entry.getKey())) {
                    List<String> values = entry.getValue();
                    return (values != null && !values.isEmpty()) ? values.get(0) : null;
                }
            }
            return null;
        }
    }

    @FunctionalInterface
    public interface BodyHandler<T> {
        T apply(InputStream stream) throws IOException;
    }

    public static final class BodyHandlers {
        private BodyHandlers() {
        }

        public static BodyHandler<String> ofString() {
            return stream -> {
                byte[] bytes = readAllBytes(stream);
                return new String(bytes, StandardCharsets.UTF_8);
            };
        }

        public static BodyHandler<byte[]> ofByteArray() {
            return Request::readAllBytes;
        }

        public static BodyHandler<Stream<String>> ofLines() {
            return stream -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                return reader.lines().onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                });
            };
        }
    }

    // ─── Builder ───────────────────────────────────────────────────

    public static final class RequestBuilder {
        private final Request outer;
        private final String method;
        private final String url;
        private Duration timeoutOverride;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private byte[] bodyBytes;
        private boolean useAuth = true;

        private RequestBuilder(Request outer, String method, String url) {
            this.outer = outer;
            this.method = method;
            this.url = url;
            this.headers.putAll(outer.headers);
        }

        public RequestBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public RequestBuilder timeout(Duration timeout) {
            this.timeoutOverride = timeout;
            return this;
        }

        public RequestBuilder body(String body) {
            this.bodyBytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
            return this;
        }

        public RequestBuilder json(String json) {
            this.bodyBytes = json != null ? json.getBytes(StandardCharsets.UTF_8) : new byte[0];
            headers.put("Content-Type", "application/json");
            return this;
        }

        public RequestBuilder bytes(byte[] bytes) {
            this.bodyBytes = bytes != null ? bytes : new byte[0];
            return this;
        }

        public RequestBuilder withoutAuth() {
            this.useAuth = false;
            return this;
        }

        public CompletableFuture<Response<String>> sendAsync() {
            return sendAsync(BodyHandlers.ofString());
        }

        public <T> CompletableFuture<Response<T>> sendAsync(BodyHandler<T> handler) {
            String resolvedUrl = resolveUrl(outer.baseUrl, url);
            Duration effectiveTimeout = timeoutOverride != null ? timeoutOverride : outer.timeout;

            if (useAuth && outer.authProvider != null) {
                return outer.authProvider.refreshIfNeeded().thenCompose(v -> {
                    String token = outer.authProvider.getAccessToken();
                    this.headers.put("Authorization", "Bearer " + token);
                    return executeAsync(resolvedUrl, effectiveTimeout, this.headers, handler);
                });
            } else {
                return executeAsync(resolvedUrl, effectiveTimeout, this.headers, handler);
            }
        }

        private <T> CompletableFuture<Response<T>> executeAsync(
                String resolvedUrl, Duration effectiveTimeout, Map<String, String> headers, BodyHandler<T> handler) {
            CompletableFuture<Response<T>> future = new CompletableFuture<>();
            EXECUTOR.execute(() -> {
                try {
                    Response<T> resp = executeSync(resolvedUrl, effectiveTimeout, headers, handler);
                    future.complete(resp);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        }

        private <T> Response<T> executeSync(
                String resolvedUrl, Duration effectiveTimeout, Map<String, String> headers, BodyHandler<T> handler)
                throws IOException {
            URL targetUrl = new URL(resolvedUrl);
            HttpURLConnection conn = (HttpURLConnection) targetUrl.openConnection();
            conn.setRequestMethod(method);

            if (effectiveTimeout != null) {
                int timeoutMillis = (int) Math.min(effectiveTimeout.toMillis(), Integer.MAX_VALUE);
                conn.setConnectTimeout(timeoutMillis);
                conn.setReadTimeout(timeoutMillis);
            }

            conn.setInstanceFollowRedirects(true);

            for (Map.Entry<String, String> header : headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }

            byte[] payload = bodyBytes;
            if ("POST".equals(method) || "PUT".equals(method) || (payload != null && payload.length > 0)) {
                byte[] toWrite = payload != null ? payload : new byte[0];
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(toWrite.length);
                try (OutputStream out = conn.getOutputStream()) {
                    if (toWrite.length > 0) {
                        out.write(toWrite);
                    }
                    out.flush();
                }
            }

            int statusCode = conn.getResponseCode();
            Map<String, List<String>> respHeaders = conn.getHeaderFields();

            if (statusCode < 200 || statusCode >= 400) {
                InputStream errIn = conn.getErrorStream();
                if (errIn == null) {
                    try {
                        errIn = conn.getInputStream();
                    } catch (IOException ignored) {
                    }
                }
                byte[] errBytes = errIn != null ? readAllBytes(errIn) : new byte[0];
                String rawBody = errBytes.length > 0 ? new String(errBytes, StandardCharsets.UTF_8) : null;
                JsonNode errorBody = parseErrorBody(rawBody);
                throw new HttpException(statusCode, errorBody, rawBody, respHeaders);
            }

            InputStream in;
            try {
                in = conn.getInputStream();
            } catch (IOException e) {
                in = new ByteArrayInputStream(new byte[0]);
            }
            if (in == null) {
                in = new ByteArrayInputStream(new byte[0]);
            }

            T body = handler.apply(in);
            return new Response<>(statusCode, respHeaders, body);
        }

        static @Nullable JsonNode parseErrorBody(@Nullable String rawBody) {
            if (rawBody == null || rawBody.trim().isEmpty()) {
                return null;
            }
            try {
                return JsonUtils.readTree(rawBody);
            } catch (Exception e) {
                return JsonNodeFactory.instance.textNode(rawBody);
            }
        }

        static String resolveUrl(String baseUrl, String url) {
            if (url == null)
                return baseUrl != null ? baseUrl : "";
            if (url.isEmpty())
                return baseUrl != null ? baseUrl : "";
            if (url.startsWith("http://") || url.startsWith("https://")) {
                return url;
            }
            if (baseUrl == null || baseUrl.isEmpty()) {
                return url;
            }
            // Query or fragment should append directly without extra slash
            if (url.startsWith("?") || url.startsWith("#")) {
                return baseUrl + url;
            }
            boolean baseEnds = baseUrl.endsWith("/");
            boolean urlStarts = url.startsWith("/");
            if (baseEnds && urlStarts) {
                return baseUrl + url.substring(1);
            }
            if (!baseEnds && !urlStarts) {
                return baseUrl + "/" + url;
            }
            return baseUrl + url;
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────

    public static byte[] readAllBytes(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ─── Multipart helper (kept for upload) ────────────────────────

    public static byte[] buildMultipartBody(String boundary, byte[] fileBytes, String fileName, String hash) {
        String CRLF = "\r\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"hash\"" + CRLF).getBytes(StandardCharsets.UTF_8));
            out.write(CRLF.getBytes(StandardCharsets.UTF_8));
            out.write((hash + CRLF).getBytes(StandardCharsets.UTF_8));

            out.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + CRLF)
                    .getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: application/octet-stream" + CRLF).getBytes(StandardCharsets.UTF_8));
            out.write(CRLF.getBytes(StandardCharsets.UTF_8));
            out.write(fileBytes);
            out.write(CRLF.getBytes(StandardCharsets.UTF_8));

            out.write(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build multipart body", e);
        }
        return out.toByteArray();
    }
}
