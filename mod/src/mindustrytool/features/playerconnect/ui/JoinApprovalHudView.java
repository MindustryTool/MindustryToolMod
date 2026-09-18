package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.playerconnect.models.JoinRequest;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.Hud;
import solim.core.Units;

public class JoinApprovalHudView extends BaseComponent {

    private final PlayerConnectFeature feature;
    private @Nullable Hud hud;

    public JoinApprovalHudView(PlayerConnectFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        hud = hud(() -> {
            dynamic(feature.currentRequestSignal(), this::buildRequestBanner);
        });

        hud.background(Styles.black8);
        hud.visible(feature.currentRequestSignal().map(r -> r != null));

        // Center at top of screen
        hud.position(
                Units.screenWidth() / 2f,
                Units.screenHeight() - 50f);

        return hud.element();
    }

    private Component buildRequestBanner(@Nullable JoinRequest request) {
        if (request == null) {
            return row();
        }

        String msg = Core.bundle.format(
                "feature.player-connect.join-request-format",
                request.player != null ? request.player.name : "Unknown");

        return row()
                .gap(unit(3))
                .margin(unit(2), unit(4), unit(2), unit(4))
                .center()
                .children(() -> {
                    text(msg).color(Color.white);

                    button(Core.bundle.get("feature.player-connect.accept", "Accept"), () -> feature.accept(request))
                            .style(Styles.defaultb)
                            .color(Pal.accent)
                            .height(unit(8));

                    button(Core.bundle.get("feature.player-connect.reject", "Reject"), () -> feature.reject(request))
                            .style(Styles.defaultb)
                            .color(Color.scarlet)
                            .height(unit(8));
                });
    }
}
