package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.scene.Element;
import arc.util.Nullable;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.settings.ModSettings;
import solim.core.Component;
import solim.overlay.Hud;
import solim.reactive.Readable;

/**
 * Standalone persistent draggable HUD view for ArmyControl when display mode is
 * set to HUD.
 */
public class ArmyControlHudView extends Component {

    private final ArmyControlFeature feature;
    private @Nullable Hud hud;

    public ArmyControlHudView(ArmyControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> dragIconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));

        hud = hud(() -> {

            when(feature.hideDragHandleConfig.signal())
                    .elseDo(() -> button()
                            .style(WebStyles.ghost())
                            .size(buttonSize)
                            .children(() -> icon(Icon.move).size(dragIconSize))
                            .draggable(feature.xSignal, feature.ySignal));

            column().width(scale.map(s -> unit(65) * (s != null ? s : 1f))).gap(unit(2)).children(() -> {
                new ArmyControlCompactContent(feature, scale);
            });
        }).gap(unit(1.5f));

        hud.position(feature.xSignal, feature.ySignal)
                .padding(unit(2.5f))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.BORDER);

        listen(ResizeEvent.class, e -> {
            if (hud != null) {
                hud.keepInScreen();
            }
        });

        return hud.element();
    }
}
