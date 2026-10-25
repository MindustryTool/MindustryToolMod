package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Readable;


public class ArmyControlCompactContent extends Component {

    private final ArmyControlFeature feature;
    private final Readable<Float> scale;

    public ArmyControlCompactContent(ArmyControlFeature feature, Readable<Float> scale) {
        this.feature = feature;
        this.scale = scale;
    }

    @Override
    protected Element build() {
        Readable<Boolean> hasSelection = feature.activeSelectionSignal()
                .map(map -> map != null && map.size > 0);
        Readable<Boolean> hasGroups = feature.controlGroupsSignal()
                .map(groups -> groups != null && groups.size > 0);

        return column()
                .props(growX(), gap(unit(2)))
                .children(() -> {
                    new ArmyControlToolbar(feature, scale);

                    when(hasSelection)
                            .props(growX())
                            .thenDo(() -> divider().props(color(WebStyles.Colors.SECTION_BORDER)));

                    new ActiveSelectionBar(feature, scale);

                    when(hasGroups)
                            .props(growX())
                            .thenDo(() -> divider().props(color(WebStyles.Colors.SECTION_BORDER)));

                    new ControlGroupList(feature, scale);

                    when(feature.contentEmptySignal())
                            .props(growX())
                            .thenDo(() -> row().props(growX(), padding(unit(1))).children(() -> {
                                text(Core.bundle.get("feature.army-control.empty.all", "Nothing selected yet"))
                                        .props(color(WebStyles.Colors.DISABLED_FG), left(), fontScale(scale));
                            }));
                }).element();
    }
}

