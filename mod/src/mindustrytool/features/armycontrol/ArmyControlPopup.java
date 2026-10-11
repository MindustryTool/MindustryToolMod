package mindustrytool.features.armycontrol;

import static solim.UI.*;

import arc.Core;
import arc.math.geom.Rect;
import arc.scene.Element;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.quickaccess.QuickAccessPopupHelper;
import solim.core.Component;
import solim.overlay.Popup;

/**
 * QuickAccess popup for ArmyControl. Provides instant army selection buttons,
 * control group creation & recall, and active unit type deselection chips.
 * Stays open until user clicks outside or clicks close.
 */
public final class ArmyControlPopup {

    private static @Nullable Popup<ArmyControlFeature> menu;

    private ArmyControlPopup() {
    }

    public static boolean isShowing() {
        return menu != null && menu.isShowing();
    }

    public static void toggle(ArmyControlFeature feature, @Nullable Element quickAccessBar) {
        if (menu != null && (menu.isShowing() || Time.timeSinceMillis(menu.getLastHideTime()) < 250L)) {
            hide();
            return;
        }
        show(feature, quickAccessBar);
    }

    public static void show(ArmyControlFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.children(ArmyControlPopup::buildContent);
        }
        feature.refreshActiveSelection();
        QuickAccessPopupHelper.show(menu, feature, quickAccessBar);
    }

    public static void hide() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component buildContent(@Nullable ArmyControlFeature feature) {
        if (feature == null) {
            return row();
        }

        return column()
                .width(unit(70))
                .padding(unit(2.5f))
                .gap(unit(2))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1f, WebStyles.Colors.SECTION_BORDER)
                .children(() -> {
                    // Header row: Title + Close button
                    row().growX().center().children(() -> {
                        icon(Icon.units).size(unit(5)).color(Pal.accent);
                        text(Core.bundle.get("feature.army-control.title", "Army Control"))
                                .color(Pal.accent)
                                .growX()
                                .left();

                        button()
                                .style(WebStyles.ghost())
                                .size(unit(7), unit(7))
                                .onClick(ArmyControlPopup::hide)
                                .children(() -> icon(Icon.cancel).size(unit(4.5f)).color(WebStyles.Colors.GHOST_FG));
                    });

                    // Scope Action Buttons: Select Map, Select Screen, Deselect All
                    row().growX().gap(unit(1.5f)).center().children(() -> {
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

                    // Active Selection Bar (Unit chips with tap-to-exclude)
                    new ActiveSelectionBar(feature);

                    // Add Group Action Button
                    row().growX().gap(unit(1.5f)).children(() -> {
                        button(Core.bundle.get("feature.army-control.action.add-group", "Add Group"), () -> showAddGroupDialog(feature))
                                .style(WebStyles.secondaryText())
                                .growX();
                    });

                    // Saved Control Groups
                    new ControlGroupList(feature);
                });
    }

    private static void showAddGroupDialog(ArmyControlFeature feature) {
        Seq<ControlGroup> groups = feature.controlGroupsSignal().peek();
        int nextNum = (groups != null ? groups.size : 0) + 1;
        String groupName = Core.bundle.format("feature.army-control.group.name-format", nextNum);

        // Capture current selected units by default, or all units on screen if none selected
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
