package mindustrytool.features.armycontrol;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;

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

    public ActiveSelectionBar(ArmyControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<ObjectIntMap<UnitType>> activeMap = feature.activeSelectionSignal();

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
                                                .style(WebStyles.actionChip())
                                                .props(padding(unit(1)), height(unit(9)), prefHeight(unit(9)), tooltip(type.localizedName + ": " + hint))
                                                .onClick(() -> feature.deselectUnitType(type))
                                                .children(() -> row().props(gap(unit(1)), center()).children(() -> {
                                                    image(new TextureRegionDrawable(type.uiIcon)).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)));
                                                    text(String.valueOf(count))
                                                            .color(WebStyles.Colors.PRIMARY_FG);
                                                }));
                                    }
                                });
                    });
                }).element();
    }
}
