package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Time;
import mindustrytool.features.quickaccess.QuickAccessPopupHelper;
import solim.core.Component;
import solim.overlay.Popup;

/**
 * QuickAccess popup for the schematic grid, anchored to the QuickAccess bar.
 * Reuses the shared reactive grid from the HUD view and auto-dismisses on selection.
 */
public final class QuickSchematicGridPopup {

    private static @Nullable Popup<QuickSchematicGridFeature> menu;

    private QuickSchematicGridPopup() {
    }

    public static boolean isShowing() {
        return menu != null && menu.isShowing();
    }

    public static void toggle(QuickSchematicGridFeature feature, @Nullable Element quickAccessBar) {
        if (menu != null && (menu.isShowing() || Time.timeSinceMillis(menu.getLastHideTime()) < 250L)) {
            hide();
            return;
        }
        show(feature, quickAccessBar);
    }

    public static void show(QuickSchematicGridFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.children(QuickSchematicGridPopup::buildContent);
        }
        QuickAccessPopupHelper.show(menu, feature, quickAccessBar);
    }

    public static void hide() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component buildContent(@Nullable QuickSchematicGridFeature feature) {
        if (feature == null) {
            return row();
        }
        return QuickSchematicGridHudView.buildFullLayout(feature, QuickSchematicGridPopup::hide, false);
    }
}
