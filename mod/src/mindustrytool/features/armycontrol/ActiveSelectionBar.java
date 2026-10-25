package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import mindustry.type.UnitType;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Readable;

public class ActiveSelectionBar extends Component {

    private final ArmyControlFeature feature;
    private final Readable<Float> scale;

    public ActiveSelectionBar(ArmyControlFeature feature, Readable<Float> scale) {
        this.feature = feature;
        this.scale = scale;
    }

    @Override
    protected Element build() {
        Readable<ObjectIntMap<UnitType>> activeMap = feature.activeSelectionSignal();
        Readable<Float> chipHeight = scale.map(s -> unit(9) * (s != null ? s : 1f));
        Readable<Float> chipIconSize = scale.map(s -> unit(5) * (s != null ? s : 1f));

        return column()
                .props(growX(), gap(unit(1)))
                .children(() -> {
                    dynamic(activeMap, map -> {
                        if (map == null || map.size == 0) {
                            return;
                        }

                        Seq<UnitType> types = new Seq<>();
                        for (UnitType key : map.keys()) {
                            types.add(key);
                        }
                        types.sort((a, b) -> Integer.compare(map.get(b, 0), map.get(a, 0)));

                        wrap()
                                .props(growX(), gap(unit(1.5f)), left())
                                .children(() -> {
                                    for (UnitType type : types) {
                                        int count = map.get(type, 0);
                                        String hint = Core.bundle.get("feature.army-control.hint.deselect-chip",
                                                "Tap to exclude from selection");
                                        button()
                                                .props(style(WebStyles.actionChip()), padding(unit(1)), height(chipHeight), prefHeight(chipHeight), tooltip(type.localizedName + ": " + hint))
                                                .onClick(() -> feature.deselectUnitType(type))
                                                .children(() -> row().props(gap(unit(1)), center()).children(() -> {
                                                    image(new TextureRegionDrawable(type.uiIcon)).props(size(chipIconSize), prefWidth(chipIconSize), prefHeight(chipIconSize));
                                                    text(String.valueOf(count)).props(color(WebStyles.Colors.PRIMARY_FG), fontScale(scale));
                                                }));
                                    }
                                });
                    });
                }).element();
    }
}
