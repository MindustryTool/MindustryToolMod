package mindustrytool.features.joystick;

import static solim.UI.*;
import static solim.modifier.Values.draggable;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.overlay.Hud;

public class JoystickHudView extends Component {

    private final JoystickFeature feature;
    private @Nullable Hud hud;

    public JoystickHudView(JoystickFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        hud = hud(() -> {
            column().children(() -> {
                when(feature.showHandleConfig.signal())
                        .thenDo(() -> button()
                                .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), tooltip(Core.bundle.get("feature.joystick.settings.show-handle")), draggable(feature.posXConfig.signal(), feature.posYConfig.signal()))
                                .children(() -> icon(Icon.move).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)))));

                arc(new JoystickWidget(feature));
            });
        });

        hud.props(position(feature.posXConfig.signal(), feature.posYConfig.signal()));
        hud.opacity(feature.opacityConfig.signal());

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        effect(() -> {
            feature.sizeConfig.signal().get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}
