package mindustrytool.features.autoplay;

import static solim.UI.*;

import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Time;
import mindustrytool.components.WebStyles;
import mindustrytool.features.quickaccess.QuickAccessPopupHelper;
import solim.core.Component;
import solim.overlay.Popup;

/**
 * QuickAccess popup for Autoplay, opened from QuickAccess in popup display
 * mode. Reuses the shared horizontal task strip layout from AutoplayHudView
 * inside a black rounded container. Opening or closing the popup never changes
 * the feature enabled state.
 */
public final class AutoplayPopup {

    private static @Nullable Popup<AutoplayFeature> menu;

    private AutoplayPopup() {
    }

    public static boolean isShowing() {
        return menu != null && menu.isShowing();
    }

    public static void toggle(AutoplayFeature feature, @Nullable Element quickAccessBar) {
        if (menu != null && (menu.isShowing() || Time.timeSinceMillis(menu.getLastHideTime()) < 250L)) {
            hide();
            return;
        }
        show(feature, quickAccessBar);
    }

    public static void show(AutoplayFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.children(AutoplayPopup::buildContent);
        }
        QuickAccessPopupHelper.show(menu, feature, quickAccessBar);
    }

    public static void hide() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component buildContent(@Nullable AutoplayFeature feature) {
        if (feature == null) {
            return row();
        }

        return row()
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.BORDER)
                .padding(unit(1))
                .gap(unit(1))
                .center()
                .children(() -> AutoplayHudView.buildControls(feature, null));
    }
}
