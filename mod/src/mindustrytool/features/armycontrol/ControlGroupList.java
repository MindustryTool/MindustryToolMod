package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Readable;

public class ControlGroupList extends Component {

    private final ArmyControlFeature feature;

    public ControlGroupList(ArmyControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Seq<ControlGroup>> groupsSignal = feature.controlGroupsSignal();

        return column()
                .props(growX(), gap(unit(1.5f)))
                .children(() -> {
                    dynamic(groupsSignal, groups -> {
                        if (groups == null || groups.size == 0) {
                            return;
                        }

                        column()
                                .props(growX(), gap(unit(1.5f)))
                                .children(() -> {
                                    for (ControlGroup group : groups) {
                                        buildGroupRow(group);
                                    }
                                });
                    });
                }).element();
    }

    private void buildGroupRow(ControlGroup group) {
        ObjectIntMap<UnitType> summary = group.summarizeUnits();
        int living = 0;
        for (UnitType ut : summary.keys()) {
            living += summary.get(ut, 0);
        }
        final int livingTotal = living;

        row()
                .props(growX(), gap(unit(2)), center())
                .children(() -> {

                    button()
                            .props(style(WebStyles.secondary()), growX(), padding(unit(1.5f)))
                            .onClick(() -> feature.recallGroup(group))
                            .children(() -> row().props(growX(), gap(unit(1.5f)), center()).children(() -> {

                                row().props(growX(), gap(unit(1)), left()).children(() -> {
                                    for (UnitType ut : summary.keys()) {
                                        image(new TextureRegionDrawable(ut.uiIcon)).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                    }
                                });

                                text(String.valueOf(livingTotal)).props(color(Pal.accent), right());
                            }));

                    button()
                            .props(style(WebStyles.danger()), size(unit(9), unit(9)), prefWidth(unit(9)), prefHeight(unit(9)), tooltip(Core.bundle.get("button.delete", "Delete")))
                            .onClick(() -> feature.deleteGroup(group))
                            .children(() -> icon(Icon.trash).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)), color(WebStyles.Colors.DANGER_FG)));
                });
    }
}
