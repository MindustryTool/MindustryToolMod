package mindustrytool.features.timecontrol;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Time;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.overlay.Popup;
import solim.signal.Readable;
import solim.signal.Signal;
import solim.signal.Signals;

/**
 * QuickAccess popup for TimeControl, opened from QuickAccess in popup display
 * mode. Reuses the shared horizontal control row layout from TimeControlHudView
 * inside a black rounded container.
 */
public final class TimeControlPopup {

    private static @Nullable Popup<TimeControlFeature> menu;

    private TimeControlPopup() {
    }

    public static boolean isShowing() {
        return menu != null && menu.isShowing();
    }

    public static void toggle(TimeControlFeature feature, @Nullable Element quickAccessBar) {
        if (menu != null && (menu.isShowing() || Time.timeSinceMillis(menu.getLastHideTime()) < 250L)) {
            hide();
            return;
        }
        show(feature, quickAccessBar);
    }

    public static void show(TimeControlFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.children(TimeControlPopup::buildContent).rounded(unit(2), Color.black);
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

    private static Component buildContent(@Nullable TimeControlFeature feature) {
        if (feature == null) {
            return row();
        }

        Readable<Boolean> canEdit = Signal.computed(() -> Boolean.TRUE.equals(feature.enabled().get())
                && Boolean.FALSE.equals(Signals.netClient().get()));

        return row()
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.BORDER)
                .padding(unit(1))
                .gap(unit(1))
                .cellPadding(unit(5))
                .margin(unit(5))
                .center()
                .children(() -> TimeControlHudView.buildControls(feature, canEdit));
    }
}
