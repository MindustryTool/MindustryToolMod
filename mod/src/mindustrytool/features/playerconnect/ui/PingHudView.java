package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import solim.core.BaseComponent;
import solim.overlay.Hud;
import solim.signal.Computed;

public class PingHudView extends BaseComponent {

    private final PlayerConnectFeature feature;
    private @Nullable Hud hud;

    public PingHudView(PlayerConnectFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Computed<String> pingText = feature.pingSignal().map(p -> "Relay: " + (p != null ? p : 0) + "ms");
        Computed<Color> pingColor = feature.pingSignal().map(p -> {
            int val = p != null ? p : 0;
            if (val <= 150) {
                return Color.green;
            }
            if (val <= 300) {
                return Pal.accent;
            }
            return Color.scarlet;
        });

        hud = hud(() -> {
            row().margin(unit(1), unit(2), unit(1), unit(2)).children(() -> {
                text(pingText).color(pingColor);
            });
        });

        hud.background(Styles.black6);
        hud.visible(feature.isHostingSignal());
        hud.position(250f, 20f);

        return hud.element();
    }
}
