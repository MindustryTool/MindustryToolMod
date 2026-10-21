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
    private final Readable<Float> scale;

    public ControlGroupList(ArmyControlFeature feature, Readable<Float> scale) {
        this.feature = feature;
        this.scale = scale;
    }

    @Override
    protected Element build() {
        Readable<Seq<ControlGroup>> groupsSignal = feature.controlGroupsSignal();
        Readable<Float> deleteButtonSize = scale.map(s -> unit(9) * (s != null ? s : 1f));
        Readable<Float> unitIconSize = scale.map(s -> unit(4) * (s != null ? s : 1f));
        Readable<Float> deleteIconSize = scale.map(s -> unit(5) * (s != null ? s : 1f));

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
                                        buildGroupRow(group, deleteButtonSize, unitIconSize, deleteIconSize);
                                    }
                                });
                    });
                }).element();
    }

    private void buildGroupRow(ControlGroup group, Readable<Float> deleteButtonSize, Readable<Float> unitIconSize,
            Readable<Float> deleteIconSize) {
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
                                        image(new TextureRegionDrawable(ut.uiIcon)).props(size(unitIconSize), prefWidth(unitIconSize), prefHeight(unitIconSize));
                                    }
                                });

                                text(String.valueOf(livingTotal)).props(color(Pal.accent), right(), fontScale(scale));
                            }));

                    button()
                            .props(style(WebStyles.danger()), size(deleteButtonSize), prefWidth(deleteButtonSize), prefHeight(deleteButtonSize), tooltip(Core.bundle.get("button.delete", "Delete")))
                            .onClick(() -> feature.deleteGroup(group))
                            .children(() -> icon(Icon.trash).props(size(deleteIconSize), prefWidth(deleteIconSize), prefHeight(deleteIconSize), color(WebStyles.Colors.DANGER_FG)));
                });
    }
}
