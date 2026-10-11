package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.math.geom.Rect;
import arc.scene.Element;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;

/**
 * Icon-only action toolbar for ArmyControl. Single row with fixed order:
 * select-map, select-screen, deselect-all, add-group, close.
 * Existing bundle strings are reused as tooltips; no new user-visible text.
 */
public class ArmyControlToolbar extends BaseComponent {

    private final ArmyControlFeature feature;
    private final @Nullable Runnable onClose;

    public ArmyControlToolbar(ArmyControlFeature feature, @Nullable Runnable onClose) {
        this.feature = feature;
        this.onClose = onClose;
    }

    @Override
    protected Element build() {
        return row()
                .growX()
                .gap(unit(1))
                .center()
                .children(() -> {
                    button()
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle.get("feature.army-control.action.select-map", "Select Map"))
                            .onClick(feature::selectAllMap)
                            .children(() -> icon(Icon.map).size(unit(6)).color(WebStyles.Colors.GHOST_FG));

                    button()
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle.get("feature.army-control.action.select-screen", "Select Screen"))
                            .onClick(feature::selectScreen)
                            .children(() -> icon(Icon.zoom).size(unit(6)).color(WebStyles.Colors.GHOST_FG));

                    button()
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle.get("feature.army-control.action.deselect-all", "Deselect All"))
                            .onClick(feature::deselectAll)
                            .children(() -> icon(Icon.exit).size(unit(6)).color(WebStyles.Colors.GHOST_FG));

                    button()
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle.get("feature.army-control.action.add-group", "Add Group"))
                            .onClick(() -> createGroupFromContext(feature))
                            .children(() -> icon(Icon.add).size(unit(6)).color(WebStyles.Colors.GHOST_FG));

                    if (onClose != null) {
                        button()
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .onClick(onClose)
                                .children(() -> icon(Icon.cancel).size(unit(6)).color(WebStyles.Colors.GHOST_FG));
                    }
                }).element();
    }

    /**
     * Captures the current selection by default, else screen-visible units,
     * else all map units, into a new control group.
     */
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
