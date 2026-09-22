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
        float barCenterY = barY + barH / 2f;
        boolean upperHalf = stageH > 0f ? barCenterY >= stageH / 2f : false;
        float anchorX = quickAccessBar != null ? barCenterX : stageW / 2f;
        float anchorY = quickAccessBar != null ? (upperHalf ? barY - gap : barTop + gap) : stageH / 2f;

        menu.layerBehind(quickAccessBar);
        menu.show(data, anchorX, anchorY);

        if (Core.scene != null && quickAccessBar != null && menu.table() != null) {
            try {
                float menuW = menu.table().getWidth();
                float menuH = menu.table().getHeight();
                float desiredX = barCenterX - menuW / 2f;
                float desiredY = upperHalf ? barY - menuH - gap : barTop + gap;
                float clampedX = stageW > menuW ? Math.max(0f, Math.min(desiredX, stageW - menuW)) : 0f;
                float clampedY = stageH > menuH ? Math.max(0f, Math.min(desiredY, stageH - menuH)) : 0f;
                menu.table().setPosition(clampedX, clampedY);
            } catch (Exception ignored) {
            }
        }
    }
}
