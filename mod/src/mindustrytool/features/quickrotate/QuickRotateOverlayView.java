package mindustrytool.features.quickrotate;

import arc.Core;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;

/**
 * In-world floating overlay with step-rotate controls anchored to the
 * selected rotatable building. Uses a raw Arc table like other floating
 * world menus so it can track the building's projected screen position.
 */
public class QuickRotateOverlayView {

    private final QuickRotateFeature feature;

    public QuickRotateOverlayView(QuickRotateFeature feature) {
        this.feature = feature;
    }

    public Table build() {
        Drawable ccw = FileIcon.of("rotate-ccw.png", Icon.left);
        Drawable cw = FileIcon.of("rotate-cw.png", Icon.right);

        Table menu = new Table(Styles.black8);
        menu.touchable = Touchable.enabled;
        menu.margin(4f);

        menu.button(ccw, Styles.clearNonei, 32f, feature::rotateCounterClockwise)
                .size(44f)
                .pad(2)
                .tooltip(Core.bundle.get("feature.quick-rotate.rotate-ccw.tooltip"));

        menu.button(cw, Styles.clearNonei, 32f, feature::rotateClockwise)
                .size(44f)
                .pad(2)
                .tooltip(Core.bundle.get("feature.quick-rotate.rotate-cw.tooltip"));

        return menu;
    }
}
