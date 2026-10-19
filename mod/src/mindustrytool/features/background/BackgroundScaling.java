package mindustrytool.features.background;

import arc.util.Nullable;

public final class BackgroundScaling {

    public static final String CROP = "crop";
    public static final String STRETCH = "stretch";

    private BackgroundScaling() {
    }

    public static boolean isCrop(@Nullable String mode) {
        return !STRETCH.equals(mode);
    }

    public static float drawWidth(
            float textureWidth,
            float textureHeight,
            float screenWidth,
            float screenHeight,
            boolean crop) {
        return crop ? textureWidth * cropScale(textureWidth, textureHeight, screenWidth, screenHeight) : screenWidth;
    }

    public static float drawHeight(
            float textureWidth,
            float textureHeight,
            float screenWidth,
            float screenHeight,
            boolean crop) {
        return crop ? textureHeight * cropScale(textureWidth, textureHeight, screenWidth, screenHeight) : screenHeight;
    }

    private static float cropScale(float textureWidth, float textureHeight, float screenWidth, float screenHeight) {
        if (textureWidth <= 0f || textureHeight <= 0f) {
            return 1f;
        }
        return Math.max(screenWidth / textureWidth, screenHeight / textureHeight);
    }
}
