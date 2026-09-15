package mindustrytool.features.browser.common;

import mindustrytool.Config;

/**
 * Shared helpers for building API image URLs and reading nullable stat
 * counters from list responses.
 */
public final class BrowserImages {

    private BrowserImages() {
    }

    public static String schematicPreviewUrl(String itemId) {
        return Config.API_URL + "/schematics/" + itemId + "/image.png?variant=preview";
    }

    public static String schematicImageUrl(String itemId) {
        return Config.API_URL + "/schematics/" + itemId + "/image.png";
    }

    public static String mapPreviewUrl(String itemId) {
        return Config.API_URL + "/maps/" + itemId + "/image.png?variant=preview";
    }

    public static String mapImageUrl(String itemId) {
        return Config.API_URL + "/maps/" + itemId + "/image.png";
    }

    public static long count(Long value) {
        return value != null ? value : 0;
    }
}
