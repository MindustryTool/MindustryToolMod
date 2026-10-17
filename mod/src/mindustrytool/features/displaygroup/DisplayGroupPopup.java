package mindustrytool.features.displaygroup;

import static solim.UI.*;
import static solim.modifier.Values.border;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.rounded;

import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Time;
import mindustrytool.components.WebStyles;
import mindustrytool.features.quickaccess.QuickAccessPopupHelper;
import solim.core.Component;
import solim.overlay.Popup;

public final class DisplayGroupPopup {

    private static @Nullable Popup<DisplayGroupFeature> menu;

    private DisplayGroupPopup() {
    }

    public static boolean isShowing() {
        return menu != null && menu.isShowing();
    }

    public static void toggle(DisplayGroupFeature feature, @Nullable Element quickAccessBar) {
        if (menu != null && (menu.isShowing() || Time.timeSinceMillis(menu.getLastHideTime()) < 250L)) {
            hide();
            return;
        }
        show(feature, quickAccessBar);
    }

    public static void show(DisplayGroupFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.items(DisplayGroupPopup::buildContent);
        }
        QuickAccessPopupHelper.show(menu, feature, quickAccessBar);
    }

    public static void hide() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component buildContent(@Nullable DisplayGroupFeature feature) {
        if (feature == null) {
            return row();
        }

        return row()
                .props(rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1.5f, WebStyles.Colors.BORDER), padding(unit(1)), gap(unit(1)), center())
                .children(() -> new DisplayGroupPopupView(feature));
    }
}