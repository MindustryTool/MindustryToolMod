package mindustrytool.features.joystick;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.overlay.Hud;

/**
 * Floating HUD surface hosting the joystick widget and its optional reposition drag handle.
 * Position persists via the feature's posX/posY config values.
 */
public class JoystickHudView extends BaseComponent {

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
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .tooltip(Core.bundle.get("feature.joystick.settings.show-handle"))
                                .children(() -> icon(Icon.move).size(unit(6)))
                                .draggable(feature.posXConfig.signal(), feature.posYConfig.signal()));

                arc(new JoystickWidget(feature));
            });
        });

        hud.position(feature.posXConfig.signal(), feature.posYConfig.signal());
        hud.opacity(feature.opacityConfig.signal());

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        effect(() -> {
            feature.sizeConfig.get();
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
