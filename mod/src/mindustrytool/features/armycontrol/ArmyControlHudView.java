package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.settings.ModSettings;
import solim.core.BaseComponent;
import solim.overlay.Hud;
import solim.reactive.Readable;

/**
 * Standalone persistent draggable HUD view for ArmyControl when display mode is set to HUD.
 */
public class ArmyControlHudView extends BaseComponent {

    private final ArmyControlFeature feature;
    private @Nullable Hud hud;

    public ArmyControlHudView(ArmyControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> dragIconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));

        hud = hud(() -> {
            // Drag handle
            when(feature.hideDragHandleConfig.signal())
                    .elseDo(() -> button()
                            .style(WebStyles.ghost())
                            .size(buttonSize)
                            .children(() -> icon(Icon.move).size(dragIconSize))
                            .draggable(feature.xSignal, feature.ySignal));

            // Content container
            column().width(unit(65)).gap(unit(1.5f)).children(() -> {
                // Header with title
                row().growX().center().children(() -> {
                    icon(Icon.units).size(unit(4.5f)).color(Pal.accent);
                    text(Core.bundle.get("feature.army-control.title", "Army Control"))
                            .color(Pal.accent)
                            .growX()
                            .left();
                });

                // Scope actions
                row().growX().gap(unit(1)).center().children(() -> {
                    button(Core.bundle.get("feature.army-control.action.select-map", "Select Map"), feature::selectAllMap)
                            .style(WebStyles.primaryText())
                            .growX();

                    button(Core.bundle.get("feature.army-control.action.select-screen", "Select Screen"), feature::selectScreen)
                            .style(WebStyles.secondaryText())
                            .growX();

                    button(Core.bundle.get("feature.army-control.action.deselect-all", "Deselect All"), feature::deselectAll)
                            .style(WebStyles.outlineText())
                            .growX();
                });

                // Active selection chips
                new ActiveSelectionBar(feature);

                // Add group button
                row().growX().children(() -> {
                    button(Core.bundle.get("feature.army-control.action.add-group", "Add Group"), () -> {
                        Seq<ControlGroup> groups = feature.controlGroupsSignal().peek();
                        int nextNum = (groups != null ? groups.size : 0) + 1;
                        String groupName = Core.bundle.format("feature.army-control.group.name-format", nextNum);
                        IntSeq ids = feature.getSelectedUnitIds();
                        if (ids.isEmpty() && Groups.unit != null && Vars.player != null) {
                            for (Unit u : Groups.unit) {
                                if (u.team == Vars.player.team() && u != Vars.player.unit() && u.isValid()) {
                                    ids.add(u.id);
                                }
                            }
                        }
                        feature.createGroup(groupName, ids);
                    }).style(WebStyles.secondaryText()).growX();
                });

                // Control group list
                new ControlGroupList(feature);
            });
        }).gap(unit(1));

        hud.position(feature.xSignal, feature.ySignal)
                .padding(unit(2))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.BORDER);

        listen(ResizeEvent.class, e -> {
            if (hud != null) {
                hud.keepInScreen();
            }
        });

        return hud.element();
    }
}
