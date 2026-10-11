package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import mindustry.type.UnitType;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.reactive.Readable;

/**
 * Renders the active unit selection bar. Displays chips for each selected unit type
 * with unit icon and count. Tapping a chip immediately deselects that unit type.
 */
public class ActiveSelectionBar extends BaseComponent {

    private final ArmyControlFeature feature;

    public ActiveSelectionBar(ArmyControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<ObjectIntMap<UnitType>> activeMap = feature.activeSelectionSignal();

        return column()
                .growX()
                .gap(unit(1))
                .children(() -> {
                    row().growX().children(() -> {
                        text(Core.bundle.get("feature.army-control.section.selection", "Active Selection"))
                                .color(WebStyles.Colors.GHOST_FG)
                                .growX()
                                .left();
                    });

                    dynamic(activeMap, map -> {
                        if (map == null || map.size == 0) {
                            row().growX().padding(unit(1)).children(() -> {
                                text(Core.bundle.get("feature.army-control.empty.selection", "No units selected"))
                                        .color(WebStyles.Colors.DISABLED_FG)
                                        .left();
                            });
                            return;
                        }

                        Seq<UnitType> types = new Seq<>();
                        for (UnitType key : map.keys()) {
                            types.add(key);
                        }
                        types.sort((a, b) -> Integer.compare(map.get(b, 0), map.get(a, 0)));

                        wrap()
                                .growX()
                                .gap(unit(1.5f))
                                .children(() -> {
                                    for (UnitType type : types) {
                                        int count = map.get(type, 0);
                                        button()
                                                .style(WebStyles.filterChip())
                                                .padding(unit(1))
                                                .tooltip(Core.bundle.get("feature.army-control.hint.deselect-chip", "Tap to exclude from selection"))
                                                .onClick(() -> feature.deselectUnitType(type))
                                                .children(() -> row().gap(unit(1)).center().children(() -> {
                                                    image(new TextureRegionDrawable(type.uiIcon)).size(unit(5));
                                                    text(type.localizedName + " x" + count)
                                                            .color(WebStyles.Colors.PRIMARY_FG);
                                                }));
                                    }
                                });
                    });
                }).element();
    }
}
