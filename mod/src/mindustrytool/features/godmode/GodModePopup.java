package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Time;
import mindustrytool.components.WebStyles;
import mindustrytool.features.quickaccess.QuickAccessPopupHelper;
import solim.core.Component;
import solim.overlay.Popup;
import solim.reactive.Readable;

/**
 * QuickAccess popup shell for GodMode. Reuses the shared icon-only horizontal
 * row layout from GodModeHudView inside a black rounded container.
 */
public final class GodModePopup {

    private static @Nullable Popup<GodModeFeature> menu;

    private GodModePopup() {
    }

    public static boolean isShowing() {
        return menu != null && menu.isShowing();
    }

    public static void toggle(GodModeFeature feature, @Nullable Element quickAccessBar) {
        if (menu != null && (menu.isShowing() || Time.timeSinceMillis(menu.getLastHideTime()) < 250L)) {
            hide();
            return;
        }
        show(feature, quickAccessBar);
    }

    public static void show(GodModeFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.children(GodModePopup::buildContent);
        }
        QuickAccessPopupHelper.show(menu, feature, quickAccessBar);
    }

    public static void hide() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component buildContent(@Nullable GodModeFeature feature) {
        if (feature == null) {
            return row();
        }
        Readable<Boolean> canEdit = feature.enabled();

        return row()
                .padding(unit(1))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.BORDER)
                .gap(unit(1))
                .center()
                .children(() -> GodModeHudView.buildControls(feature, canEdit));
    }
}
