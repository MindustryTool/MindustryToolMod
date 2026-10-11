package mindustrytool.features.quickaccess;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;
import solim.overlay.Popup;

/**
 * Shared positioning helper for feature popups anchored to the QuickAccess bar.
 */
public final class QuickAccessPopupHelper {

    public static final float DEFAULT_GAP = unit(1f);

    private QuickAccessPopupHelper() {
    }

    public static <T> void show(Popup<T> menu, T data, @Nullable Element quickAccessBar) {
        show(menu, data, quickAccessBar, DEFAULT_GAP);
    }

    /**
     * Half rule shared with popup pinning: a bar centered at or above stage
     * midpoint counts as upper half (popup hangs below it).
     */
    public static boolean isUpperHalf(float barY, float barH, float stageH) {
        float barCenterY = barY + barH / 2f;
        return stageH > 0f && barCenterY >= stageH / 2f;
    }

    public static <T> void show(Popup<T> menu, T data, @Nullable Element quickAccessBar, float gap) {
        float stageW = Core.scene != null ? Core.scene.getWidth() : 0f;
        float stageH = Core.scene != null ? Core.scene.getHeight() : 0f;
        float barX = 0f;
        float barY = 0f;
        float barW = 0f;
        float barH = 0f;

        if (quickAccessBar != null) {
            barX = quickAccessBar.x;
            barY = quickAccessBar.y;
            barW = quickAccessBar.getWidth();
            barH = quickAccessBar.getHeight();
        }
        
        float barCenterX = barX + barW / 2f;
        float barTop = barY + barH;
        boolean upperHalf = isUpperHalf(barY, barH, stageH);
        boolean leftHalf = quickAccessBar != null ? barCenterX < stageW / 2f : true;
        float anchorX = quickAccessBar != null ? (leftHalf ? barX : barX + barW) : stageW / 2f;
        float anchorY = quickAccessBar != null ? (upperHalf ? barY - gap : barTop + gap) : stageH / 2f;

        menu.layerBehind(quickAccessBar);
        menu.show(data, anchorX, anchorY);

        if (Core.scene != null && quickAccessBar != null && menu.table() != null) {
            try {
                float menuW = menu.table().getWidth();
                float menuH = menu.table().getHeight();
                float desiredX = computeDesiredX(barX, barW, menuW, stageW);
                float desiredY = upperHalf ? barY - menuH - gap : barTop + gap;
                float clampedX = clampX(desiredX, menuW, stageW);
                float clampedY = stageH > menuH ? Math.max(0f, Math.min(desiredY, stageH - menuH)) : 0f;
                menu.table().setPosition(clampedX, clampedY);
            } catch (Exception ignored) {
            }
        }
    }

    static float computeDesiredX(float barX, float barW, float menuW, float stageW) {
        float barCenterX = barX + barW / 2f;
        boolean leftHalf = barCenterX < stageW / 2f;
        return leftHalf ? barX : barX + barW - menuW;
    }

    static float clampX(float desiredX, float menuW, float stageW) {
        return stageW > menuW ? Math.max(0f, Math.min(desiredX, stageW - menuW)) : 0f;
    }
}
