package mindustrytool.features.quickrotate;

import arc.Core;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;

public class QuickRotateOverlayView {

    private final QuickRotateFeature feature;

    public QuickRotateOverlayView(QuickRotateFeature feature) {
        this.feature = feature;
    }

    public Table build() {
        Drawable ccw = FileIcon.of("rotate-ccw.png", Icon.left);
        Drawable cw = FileIcon.of("rotate-cw.png", Icon.right);

        Table menu = new Table();
        menu.touchable = Touchable.childrenOnly;

        menu.button(b -> b.image(ccw).scaling(Scaling.fit).size(32f),
                WebStyles.secondary().style(), feature::rotateCounterClockwise)
                .size(44f)
                .pad(2)
                .tooltip(Core.bundle.get("feature.quick-rotate.rotate-ccw.tooltip"));

        menu.button(b -> b.image(cw).scaling(Scaling.fit).size(32f),
                WebStyles.secondary().style(), feature::rotateClockwise)
                .size(44f)
                .pad(2)
                .tooltip(Core.bundle.get("feature.quick-rotate.rotate-cw.tooltip"));

        return menu;
    }
}
