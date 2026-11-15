package mindustrytool.features.freecamera;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.event.Touchable;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import solim.core.Component;

public class FreeCameraHudView extends Component {

    private static final float TOP_MARGIN = 60f;

    private final FreeCameraFeature feature;

    public FreeCameraHudView(FreeCameraFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        String labelText = Core.bundle != null
                ? Core.bundle.get("status.free-camera.enabled", "Free camera enabled")
                : "Free camera enabled";

        return column()
                .props(fillParent(), top(), center(), touchable(Touchable.childrenOnly))
                .children(() -> {
                    button(this::handleClick)
                            .props(
                                    style(WebStyles.ghost()),
                                    marginTop(TOP_MARGIN),
                                    padding(unit(1), unit(2), unit(1), unit(2)),
                                    center()
                            )
                            .children(() -> {
                                text(labelText)
                                        .props(style(Styles.outlineLabel), color(Pal.accent));
                            });
                })
                .element();
    }

    private void handleClick() {
        feature.setEnabled(false);
    }
}
