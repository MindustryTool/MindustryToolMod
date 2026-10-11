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
import solim.core.BaseComponent;
import solim.reactive.Readable;

/**
 * Component rendering saved Control Groups list with recall, unit summary, and delete buttons.
 */
public class ControlGroupList extends BaseComponent {

    private final ArmyControlFeature feature;

    public ControlGroupList(ArmyControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Seq<ControlGroup>> groupsSignal = feature.controlGroupsSignal();

        return column()
                .growX()
                .gap(unit(1))
                .children(() -> {
                    row().growX().children(() -> {
                        text(Core.bundle.get("feature.army-control.section.groups", "Control Groups"))
                                .color(WebStyles.Colors.GHOST_FG)
                                .growX()
                                .left();
                    });

                    dynamic(groupsSignal, groups -> {
                        if (groups == null || groups.size == 0) {
                            row().growX().padding(unit(1)).children(() -> {
                                text(Core.bundle.get("feature.army-control.empty.groups", "No groups saved"))
                                        .color(WebStyles.Colors.DISABLED_FG)
                                        .left();
                            });
                            return;
                        }

                        column()
                                .growX()
                                .gap(unit(1))
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
        int totalUnits = group.getUnitIds().size;

        row()
                .growX()
                .gap(unit(1))
                .center()
                .children(() -> {
                    // Recall button
                    button()
                            .style(WebStyles.secondary())
                            .growX()
                            .padding(unit(1.5f))
                            .onClick(() -> feature.recallGroup(group))
                            .children(() -> row().growX().gap(unit(1.5f)).center().children(() -> {
                                text(group.getName() + " (" + totalUnits + ")")
                                        .color(Pal.accent)
                                        .left();

                                // Icons of unit types in group
                                row().gap(unit(1)).growX().right().children(() -> {
                                    for (UnitType ut : summary.keys()) {
                                        image(new TextureRegionDrawable(ut.uiIcon)).size(unit(4));
                                    }
                                });
                            }));

                    // Delete group button
                    button()
                            .style(WebStyles.danger())
                            .size(unit(9), unit(9))
                            .onClick(() -> feature.deleteGroup(group))
                            .tooltip(Core.bundle.get("button.delete", "Delete"))
                            .children(() -> icon(Icon.trash).size(unit(5)).color(WebStyles.Colors.DANGER_FG));
                });
    }
}
