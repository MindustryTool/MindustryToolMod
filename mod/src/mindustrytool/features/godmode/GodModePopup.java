package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;
import solim.core.Component;
import solim.overlay.Popup;
import solim.signal.Readable;

/**
 * QuickAccess popup shell for GodMode. Reuses the shared icon-only horizontal
 * row layout from GodModeHudView under a popup-only title header.
 */
public final class GodModePopup {

    private static @Nullable Popup<GodModeFeature> menu;

    private GodModePopup() {
    }

    public static void show(GodModeFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.children(GodModePopup::buildContent).rounded(2);
        }
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
        float anchorY = quickAccessBar != null ? (upperHalf ? barY : barTop) : stageH / 2f;

        menu.show(feature, anchorX, anchorY);

        if (Core.scene != null && quickAccessBar != null && menu.table() != null) {
            try {
                float menuW = menu.table().getWidth();
                float menuH = menu.table().getHeight();
                float desiredX = barCenterX - menuW / 2f;
                float desiredY = upperHalf ? barY - menuH : barTop;
                float clampedX = stageW > menuW ? Math.max(0f, Math.min(desiredX, stageW - menuW)) : 0f;
                float clampedY = stageH > menuH ? Math.max(0f, Math.min(desiredY, stageH - menuH)) : 0f;
                menu.table().setPosition(clampedX, clampedY);
            } catch (Exception ignored) {
            }
        }
    }

    public static void hide() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component buildContent(@Nullable GodModeFeature feature) {
        if (feature == null) {
            return column().children(() -> text(Core.bundle.get("feature.god-mode.popup.title")));
        }
        Readable<Boolean> canEdit = feature.enabled();

        return column()
                .padding(unit(2))
                .gap(unit(1))
                .children(() -> {
                    text(Core.bundle.get("feature.god-mode.popup.title")).center();
                    GodModeHudView.buildControls(feature, canEdit);
                });
    }
}
