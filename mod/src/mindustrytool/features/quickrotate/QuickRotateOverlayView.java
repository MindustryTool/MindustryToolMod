package mindustrytool.features.quickrotate;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.overlay.Hud;

/**
 * In-world floating overlay with step-rotate controls anchored to the
 * selected rotatable building.
 */
public class QuickRotateOverlayView extends BaseComponent {

    private final QuickRotateFeature feature;
    private @Nullable Hud hud;

    public QuickRotateOverlayView(QuickRotateFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        hud = hud(() -> {
            row()
                    .padding(unit(1))
                    .gap(unit(1))
                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                    .border(1.5f, WebStyles.Colors.BORDER)
                    .center()
                    .children(() -> {
                        button()
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .tooltip(Core.bundle.get("feature.quick-rotate.rotate-ccw.tooltip"))
                                .onClick(feature::rotateCounterClockwise)
                                .children(() -> text(CCW));

                        button()
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .tooltip(Core.bundle.get("feature.quick-rotate.rotate-cw.tooltip"))
                                .onClick(feature::rotateClockwise)
                                .children(() -> text(CW));
                    });
        });

        hud.position(feature.xSignal(), feature.ySignal());

        return hud.element();
    }

    public @Nullable Hud getHud() {
        return hud;
    }

    private static final String CCW = "\u21BA";
    private static final String CW = "\u21BB";
}
