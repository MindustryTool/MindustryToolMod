package mindustrytool.features.smartdrill.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Scaling;
import java.util.List;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.world.Block;
import mindustrytool.components.WebStyles;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import mindustrytool.features.smartdrill.plan.PlannedBuild;
import solim.core.Component;
import solim.reactive.Readable;

public class SmartDrillPanel extends Component {

    private static final int DRILL_COLUMNS = 5;

    private final SmartDrillFeature feature;

    public SmartDrillPanel(SmartDrillFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column()
                .props(
                        solim.UI.name("smart-drill-panel"),
                        growX(),
                        maxWidth(unit(125)),
                        padding(unit(2.5f)),
                        gap(unit(2)),
                        rounded(unit(2), WebStyles.Colors.SECTION_BG),
                        border(1f, WebStyles.Colors.SECTION_BORDER))
                .children(() -> {
                    titleRow();
                    targetRow();
                    facingRow();
                    areaRow();
                    drillRow();
                    rigRow();
                    actionRow();
                })
                .element();
    }

    private Component titleRow() {
        return row().props(growX(), gap(unit(1.5f)), left()).children(() -> {
            icon(Icon.wrench).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)));
            text(Core.bundle.get("feature.smart-drill.panel.title")).props(growX(), left());
        });
    }

    private Component targetRow() {
        return row().props(growX(), gap(unit(2)), left()).children(() -> {
            icon(feature.targetIcon()).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)));
            text(feature.targetName()).props(growX(), left());
            badge(feature.targetBadge()).props(rounded(unit(1), WebStyles.Colors.PRIMARY));
        });
    }

    private Component facingRow() {
        return row().props(growX(), gap(unit(1.5f)), left()).children(() -> {
            text(Core.bundle.get("feature.smart-drill.panel.facing"))
                    .props(color(WebStyles.Colors.GHOST_FG), left());
            spacer();
            directionButton(Icon.left, "feature.smart-drill.panel.facing.left",
                    () -> feature.setFacing(feature.getFacing() + 2));
            directionButton(Icon.up, "feature.smart-drill.panel.facing.up",
                    () -> feature.setFacing(feature.getFacing() + 1));
            directionButton(Icon.down, "feature.smart-drill.panel.facing.down",
                    () -> feature.setFacing(feature.getFacing() + 3));
            directionButton(Icon.right, "feature.smart-drill.panel.facing.right",
                    () -> feature.setFacing(feature.getFacing()));
        });
    }

    private Component directionButton(Drawable drawable, String key, Runnable action) {
        return button()
                .props(style(WebStyles.ghost()), size(unit(9)), prefWidth(unit(9)), prefHeight(unit(9)),
                        tooltip(Core.bundle.get(key)))
                .onClick(action)
                .children(() -> icon(drawable).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));
    }

    private Component areaRow() {
        return row().props(growX(), gap(unit(2)), left()).children(() -> {
            text(Core.bundle.get("feature.smart-drill.panel.area"))
                    .props(color(WebStyles.Colors.GHOST_FG), left());
            text(feature.rectLabel()).props(growX(), left());
            button(Core.bundle.get("feature.smart-drill.panel.area.reset"), feature::resetRect)
                    .props(style(WebStyles.ghost()), height(unit(9)), prefHeight(unit(9)));
        });
    }

    private Component drillRow() {
        Readable<List<Block>> drills = feature.drills();
        return column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
            text(Core.bundle.get("feature.smart-drill.panel.drills"))
                    .props(color(WebStyles.Colors.GHOST_FG), left());
            reactiveGrid(drills)
                    .columns(DRILL_COLUMNS)
                    .props(gap(unit(1.5f)), growX())
                    .key(block -> block.name)
                    .emptyView(() -> text(Core.bundle.get("feature.smart-drill.no-drills"))
                            .props(color(WebStyles.Colors.GHOST_FG), left(), textWrap()))
                    .items(this::drillButton);
        });
    }

    private Component drillButton(Block drill) {
        String name = drill.localizedName;
        boolean selected = name != null && name.equals(feature.getSelectedDrillName());
        return button()
                .props(
                        style(selected ? WebStyles.secondary() : WebStyles.ghost()),
                        size(unit(11)),
                        prefWidth(unit(11)),
                        prefHeight(unit(11)),
                        checked(selected),
                        tooltip(name))
                .onClick(() -> feature.selectDrill(drill.name))
                .children(() -> image(new TextureRegionDrawable(
                        drill.uiIcon != null ? drill.uiIcon : drill.fullIcon))
                        .props(scaling(Scaling.fit), size(unit(6)), prefWidth(unit(6)),
                                prefHeight(unit(6))));
    }

    private Component rigRow() {
        Readable<List<Schematic>> rigs = feature.rigs();
        return row()
                .props(growX(), gap(unit(1.5f)), left(), visible(feature.hasRigs()))
                .children(() -> {
                    text(Core.bundle.get("feature.smart-drill.panel.rigs"))
                            .props(color(WebStyles.Colors.GHOST_FG), left());
                    reactiveGrid(rigs)
                            .props(gap(unit(1.5f)), growX())
                            .key(schematic -> schematic.name())
                            .items(this::rigButton);
                    button()
                            .props(style(WebStyles.ghost()), size(unit(9)), prefWidth(unit(9)),
                                    prefHeight(unit(9)),
                                    tooltip(Core.bundle.get("feature.smart-drill.panel.rig.bind")))
                            .onClick(feature::openBindDialog)
                            .children(() -> icon(Icon.add).props(size(unit(5)), prefWidth(unit(5)),
                                    prefHeight(unit(5))));
                });
    }

    private Component rigButton(Schematic schematic) {
        String name = schematic.name();
        return button()
                .props(style(WebStyles.ghost()), height(unit(9)), prefWidth(unit(11)),
                        tooltip(Core.bundle.format("feature.smart-drill.panel.rig.use", name)))
                .onClick(() -> feature.selectRig(name))
                .children(() -> text(name).props(fontScale(0.8f), center()));
    }

    private Component actionRow() {
        return row().props(growX(), gap(unit(2)), left()).children(() -> {
            column().props(growX(), gap(unit(1)), left()).children(() -> {
                text(feature.readout()).props(color(WebStyles.Colors.GHOST_FG), left(), textWrap());
                text(feature.warning()).props(color(Color.scarlet), left(), textWrap());
            });
            button(Core.bundle.get("feature.smart-drill.panel.cancel"), feature::cancel)
                    .props(style(WebStyles.outlineText()), height(unit(11)), prefHeight(unit(11)));
            button(Core.bundle.get("feature.smart-drill.panel.place"), feature::place)
                    .props(style(WebStyles.primary()), height(unit(11)), prefHeight(unit(11)),
                            visible(feature.hasPlan()));
        });
    }

    public @Nullable Table table() {
        Element element = element();
        return element instanceof Table ? (Table) element : null;
    }

    public int plannedDrills() {
        int count = 0;
        Seq<PlannedBuild> builds = feature.getPlan().builds;
        for (int i = 0; i < builds.size; i++) {
            if (builds.get(i).role == PlannedBuild.Role.DRILL
                    || builds.get(i).role == PlannedBuild.Role.BEAM_DRILL) {
                count++;
            }
        }
        return count;
    }
}