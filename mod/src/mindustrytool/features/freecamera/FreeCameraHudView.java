package mindustrytool.features.freecamera;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.util.Nullable;
import mindustry.game.EventType.ResizeEvent;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import solim.core.BaseComponent;
import solim.core.Units;
import solim.overlay.Hud;

/**
 * Persistent status pill shown while Free Camera is enabled.
 * Anchored top-center below the join-approval banner zone, styled with
 * {@link Pal#accent} on a dark translucent background. The pill is
 * non-interactive so map panning and clicks pass through to the game.
 */
public class FreeCameraHudView extends BaseComponent {

    private final FreeCameraFeature feature;
    private @Nullable Hud hud;

    public FreeCameraHudView(FreeCameraFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        hud = hud(() -> {
            row()
                    .margin(unit(2), unit(4), unit(2), unit(4))
                    .center()
                    .children(() -> {
                        text(Core.bundle.get("status.free-camera.enabled")).color(Pal.accent).center();
                    });
        });

        hud.background(Styles.black8)
                .rounded(unit(2))
                .containerTouchable(Touchable.childrenOnly);

        // Below the JoinApproval banner (screenHeight - 50) so the two never overlap.
        hud.position(
                Units.screenWidth() / 2f,
                Units.screenHeight() - 110f);

        listen(ResizeEvent.class, e -> {
            keepInScreen();
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

    public @Nullable Hud getHud() {
        return hud;
    }
}
