package mindustrytool.features.armycontrol;

import static solim.UI.*;
import static solim.modifier.Values.color;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.math.geom.Rect;
import arc.scene.Element;
import arc.struct.IntSeq;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Readable;


public class ArmyControlToolbar extends Component {

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
                .props(growX(), gap(unit(2)), left())
                .children(() -> {
                    button()
                            .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.army-control.action.select-map", "Select Map")))
                            .onClick(feature::selectAllMap)
                            .children(() -> icon(FileIcon.of("map.png", Icon.map)).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(WebStyles.Colors.PRIMARY_FG)));

                    button()
                            .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.army-control.action.select-screen", "Select Screen")))
                            .onClick(feature::selectScreen)
                            .children(() -> icon(FileIcon.of("scan.png", Icon.zoom)).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(WebStyles.Colors.PRIMARY_FG)));

                    button()
                            .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.army-control.action.deselect-all", "Deselect All")))
                            .onClick(feature::deselectAll)
                            .children(() -> icon(FileIcon.of("x.png", Icon.exit)).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(WebStyles.Colors.PRIMARY_FG)));

                    button()
                            .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.army-control.action.add-group", "Add Group")))
                            .onClick(() -> createGroupFromContext(feature))
                            .children(() -> icon(FileIcon.of("plus.png", Icon.add)).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(WebStyles.Colors.PRIMARY_FG)));
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
