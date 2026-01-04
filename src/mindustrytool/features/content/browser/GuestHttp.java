package mindustrytool.features.content.browser;

import arc.util.Http;
import arc.util.Http.HttpRequest;

/**
 * Handles HTTP requests specifically for guest access (no authentication).
 * Ensures no Authorization headers are sent.
 */
public final class GuestHttp {
    private GuestHttp() {
    }

    public static HttpRequest get(String url) {
        // Create a raw request.
        // Arc's Http.get() does not add default headers, so this is clean.
        // We wrap it here to be explicit and allow for future guest-specific config.
        return Http.get(url)
                .header("User-Agent", "MindustryTool-Guest/1.0")
                .header("Accept", "application/json");
    }
}
