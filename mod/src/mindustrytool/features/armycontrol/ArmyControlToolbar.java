package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.math.geom.Rect;
import arc.scene.Element;
import arc.struct.IntSeq;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.reactive.Readable;


public class ArmyControlToolbar extends BaseComponent {

    private final ArmyControlFeature feature;
    private final Readable<Float> scale;

    public ArmyControlToolbar(ArmyControlFeature feature, Readable<Float> scale) {
        this.feature = feature;
        this.scale = scale;
    }

    @Override
    protected Element build() {
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));

        return row()
                .growX()
                .gap(unit(2))
                .left()
                .children(() -> {
                    button()
                            .style(WebStyles.ghost())
                            .size(buttonSize)
                            .tooltip(Core.bundle.get("feature.army-control.action.select-map", "Select Map"))
                            .onClick(feature::selectAllMap)
                            .children(() -> icon(Icon.map).size(iconSize).color(WebStyles.Colors.PRIMARY_FG));

                    button()
                            .style(WebStyles.ghost())
                            .size(buttonSize)
                            .tooltip(Core.bundle.get("feature.army-control.action.select-screen", "Select Screen"))
                            .onClick(feature::selectScreen)
                            .children(() -> icon(Icon.zoom).size(iconSize).color(WebStyles.Colors.PRIMARY_FG));

                    button()
                            .style(WebStyles.ghost())
                            .size(buttonSize)
                            .tooltip(Core.bundle.get("feature.army-control.action.deselect-all", "Deselect All"))
                            .onClick(feature::deselectAll)
                            .children(() -> icon(Icon.exit).size(iconSize).color(WebStyles.Colors.PRIMARY_FG));

                    button()
                            .style(WebStyles.ghost())
                            .size(buttonSize)
                            .tooltip(Core.bundle.get("feature.army-control.action.add-group", "Add Group"))
                            .onClick(() -> createGroupFromContext(feature))
                            .children(() -> icon(Icon.add).size(iconSize).color(WebStyles.Colors.PRIMARY_FG));
                }).element();
    }


    private static void createGroupFromContext(ArmyControlFeature feature) {
        Seq<ControlGroup> groups = feature.controlGroupsSignal().peek();
        int nextNum = (groups != null ? groups.size : 0) + 1;
        String groupName = Core.bundle.format("feature.army-control.group.name-format", nextNum);

        IntSeq ids = feature.getSelectedUnitIds();
        if (ids.isEmpty() && Groups.unit != null && Vars.player != null && Core.camera != null) {
            Rect bounds = new Rect();
            Core.camera.bounds(bounds);
            for (Unit u : Groups.unit) {
                if (u.team == Vars.player.team() && u != Vars.player.unit() && u.isValid()
                        && bounds.contains(u.x, u.y)) {
                    ids.add(u.id);
                }
            }
        }
        if (ids.isEmpty() && Groups.unit != null && Vars.player != null) {
            for (Unit u : Groups.unit) {
                if (u.team == Vars.player.team() && u != Vars.player.unit() && u.isValid()) {
                    ids.add(u.id);
                }
            }
        }

        feature.createGroup(groupName, ids);
    }
}
