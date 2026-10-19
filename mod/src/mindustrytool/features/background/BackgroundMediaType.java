package mindustrytool.features.background;

import arc.util.Nullable;

public enum BackgroundMediaType {
    NONE,
    IMAGE,
    VIDEO;

    private static final String VIDEO_EXTENSION = ".mp4";

    public static BackgroundMediaType of(@Nullable String path) {
        if (path == null || path.trim().isEmpty()) {
            return NONE;
        }
        return path.toLowerCase().endsWith(VIDEO_EXTENSION) ? VIDEO : IMAGE;
    }

    public boolean isVideo() {
        return this == VIDEO;
    }
}
