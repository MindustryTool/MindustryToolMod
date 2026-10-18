package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;
import static solim.modifier.Values.background;
import static solim.modifier.Values.color;
import static solim.modifier.Values.height;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.style;
import static solim.modifier.Values.visible;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.playerconnect.models.JoinRequest;
import solim.core.Component;
import solim.overlay.Hud;
import solim.core.Units;

public class JoinApprovalHudView extends Component {

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

        hud.props(
                background(Styles.black8),
                visible(feature.currentRequestSignal().map(r -> r != null)),
                position(Units.screenWidth() / 2f, Units.screenHeight() - 50f));

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
                .props(margin(unit(2), unit(4), unit(2), unit(4)), gap(unit(3)), center())
                .children(() -> {
                    text(msg).props(color(Color.white));

                    button(Core.bundle.get("feature.player-connect.accept", "Accept"), () -> feature.accept(request))
                            .props(style(Styles.defaultb), color(Pal.accent), height(unit(8)), prefHeight(unit(8)));

                    button(Core.bundle.get("feature.player-connect.reject", "Reject"), () -> feature.reject(request))
                            .props(style(Styles.defaultb), color(Color.scarlet), height(unit(8)), prefHeight(unit(8)));
                });
    }
}
