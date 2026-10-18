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
                            .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), draggable(feature.xSignal, feature.ySignal))
                            .children(() -> icon(Icon.move).props(size(dragIconSize), prefWidth(dragIconSize), prefHeight(dragIconSize))));

            column().props(width(scale.map(s -> unit(65) * (s != null ? s : 1f))), prefWidth(scale.map(s -> unit(65) * (s != null ? s : 1f))), gap(unit(2))).children(() -> {
                new ArmyControlCompactContent(feature, scale);
            });
        }).props(gap(unit(1.5f)), position(feature.xSignal, feature.ySignal), padding(unit(2.5f)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1.5f, WebStyles.Colors.BORDER));

        listen(ResizeEvent.class, e -> {
            if (hud != null) {
                hud.keepInScreen();
            }
        });

        return hud.element();
    }
}
