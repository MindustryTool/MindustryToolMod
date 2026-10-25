package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Time;
import mindustrytool.components.WebStyles;
import mindustrytool.features.quickaccess.QuickAccessPopupHelper;
import mindustrytool.features.settings.ModSettings;
import solim.core.Component;
import solim.overlay.Popup;
import solim.reactive.Readable;


public final class ArmyControlPopup {

    private static @Nullable Popup<ArmyControlFeature> menu;

    private ArmyControlPopup() {
    }

    public static boolean isShowing() {
        return menu != null && menu.isShowing();
    }

    public static void toggle(ArmyControlFeature feature, @Nullable Element quickAccessBar) {
        if (menu != null && (menu.isShowing() || Time.timeSinceMillis(menu.getLastHideTime()) < 250L)) {
            hide();
            return;
        }
        show(feature, quickAccessBar);
    }

    public static void show(ArmyControlFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.items(ArmyControlPopup::buildContent);
        }
        feature.refreshActiveSelection();
        QuickAccessPopupHelper.show(menu, feature, quickAccessBar);
        menu.props(pinnedEdge(pinnedEdgeFor(quickAccessBar)));
    }

    public static void hide() {
        if (menu != null) {
            menu.hide();
        }
    }


    static Popup.Edge pinnedEdgeFor(@Nullable Element quickAccessBar) {
        float stageH = Core.scene != null ? Core.scene.getHeight() : 0f;
        float barY = 0f;
        float barH = 0f;
        if (quickAccessBar != null) {
            barY = quickAccessBar.y;
            barH = quickAccessBar.getHeight();
        }
        return QuickAccessPopupHelper.isUpperHalf(barY, barH, stageH) ? Popup.Edge.TOP : Popup.Edge.BOTTOM;
    }

    private static Component buildContent(@Nullable ArmyControlFeature feature) {
        if (feature == null) {
            return row();
        }

        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> columnWidth = scale.map(s -> unit(65) * (s != null ? s : 1f));

        return column()
                .props(width(columnWidth), prefWidth(columnWidth), padding(unit(2.5f)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1f, WebStyles.Colors.SECTION_BORDER), gap(unit(2)))
                .children(() -> {
                    new ArmyControlCompactContent(feature, scale);
                });
    }
}