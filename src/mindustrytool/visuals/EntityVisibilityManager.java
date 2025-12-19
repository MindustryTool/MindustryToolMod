package mindustrytool.visuals;

import arc.Events;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import arc.graphics.g2d.Draw;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.ui.DualContentSelectionTable;
import mindustry.gen.Building;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EntityVisibilityManager {

    // Configuration: Set of content types to hide
    private final ObjectSet<UnlockableContent> hiddenContent = new ObjectSet<>();

    // Optimization Flags
    private boolean hasHiddenUnits = false;
    private boolean hasHiddenBlocks = false;

    // Cache for restoring hidden items
    private final Seq<Unit> hiddenUnits = new Seq<>();
    private final Seq<Building> hiddenBuildings = new Seq<>();

    // Reflection fields for accessing internal draw indices
    private static Field unitDrawIndexField;
    private static Field blockTileviewField;

    private boolean enabled = true;
    private BaseDialog dialog;

    public EntityVisibilityManager() {
        initReflection();

        // Execute visibility logic during the draw trigger to intercept rendering
        Events.run(EventType.Trigger.draw, this::updateVisibility);

        setupUI();
    }

    private void initReflection() {
        try {
            // Access internal draw index field to manipulate render list
            unitDrawIndexField = Unit.class.getDeclaredField("index__draw");
            unitDrawIndexField.setAccessible(true);

            // Access BlockRenderer's tileview list to filter visible blocks
            // Mindustry class: mindustry.graphics.BlockRenderer
            Class<?> blockRendererClass = mindustry.graphics.BlockRenderer.class;
            blockTileviewField = blockRendererClass.getDeclaredField("tileview");
            blockTileviewField.setAccessible(true);

        } catch (Exception e) {
            arc.util.Log.err("EntityVisibilityManager reflection failure", e);
            enabled = false;
        }
    }

    private void recalculateFlags() {
        hasHiddenUnits = false;
        hasHiddenBlocks = false;
        for (UnlockableContent c : hiddenContent) {
            if (c instanceof mindustry.type.UnitType)
                hasHiddenUnits = true;
            if (c instanceof mindustry.world.Block)
                hasHiddenBlocks = true;
        }
    }

    private void setupUI() {
        dialog = new BaseDialog("Entity Visibility");
        dialog.addCloseButton();

        Table cont = dialog.cont;

        // Content Tabs
        Table tabs = new Table();

        // Unit Selection Table (Native Style)
        Table unitTable = new Table();
        unitTable.add(new DualContentSelectionTable(Vars.content.units(), hiddenContent, "Banned Units",
                "Unbanned Units", this::recalculateFlags)).grow();

        // Block Selection Table (Native Style)
        Table blockTable = new Table();
        blockTable.add(new DualContentSelectionTable(Vars.content.blocks(), hiddenContent, "Banned Blocks",
                "Unbanned Blocks", this::recalculateFlags)).grow();

        // Initial View: Show Units
        tabs.add(unitTable).grow();

        // Tab Selectors
        arc.scene.ui.ButtonGroup<arc.scene.ui.TextButton> group = new arc.scene.ui.ButtonGroup<>();

        cont.table(t -> {
            t.button("Units", mindustry.ui.Styles.togglet, () -> {
                tabs.clear();
                tabs.add(unitTable).grow();
            }).group(group).checked(true).growX().height(40);

            t.button("Blocks", mindustry.ui.Styles.togglet, () -> {
                tabs.clear();
                tabs.add(blockTable).grow();
            }).group(group).growX().height(40);
        }).growX().pad(5).row();

        cont.add(tabs).grow().row();
    }

    public void showDialog() {
        if (dialog != null)
            dialog.show();
    }

    private void updateVisibility() {
        if (!enabled || !Vars.state.isGame())
            return;

        // Optimization: If nothing is hidden and we don't need to restore anything,
        // skip
        if (!hasHiddenUnits && !hasHiddenBlocks && hiddenUnits.isEmpty()) {
            return;
        }

        // 1. UNIT VISIBILITY LOGIC
        // Restore Phase: Re-enable visibility for units that are no longer hidden
        for (int i = hiddenUnits.size - 1; i >= 0; i--) {
            Unit u = hiddenUnits.get(i);
            if (!u.isValid()) {
                hiddenUnits.remove(i);
                continue;
            }
            if (!hiddenContent.contains(u.type)) {
                int newIndex = Groups.draw.addIndex(u);
                setIndex(u, newIndex);
                hiddenUnits.remove(i);
            }
        }

        // Hide Phase: Remove units from draw list
        if (hasHiddenUnits) {
            Groups.draw.each(entity -> {
                if (entity instanceof Unit u) {
                    if (hiddenContent.contains(u.type)) {
                        int idx = getIndex(u);
                        if (idx != -1) {
                            Groups.draw.removeIndex(u, idx);
                            setIndex(u, -1);
                            hiddenUnits.add(u);
                        }
                    }
                }
            });
        }

        // 2. BLOCK VISIBILITY LOGIC
        // Filter the BlockRenderer's tileview list
        if (hasHiddenBlocks) {
            try {
                if (blockTileviewField != null) {
                    // Get the list of tiles to be rendered this frame
                    Seq<mindustry.world.Tile> tileview = (Seq<mindustry.world.Tile>) blockTileviewField
                            .get(Vars.renderer.blocks);

                    if (tileview != null && !tileview.isEmpty()) {
                        // Remove tiles that match hidden blocks
                        // This is safe because tileview is rebuilt every frame by BlockRenderer
                        tileview.removeAll(t -> t.build != null && hiddenContent.contains(t.build.block));
                    }
                }
            } catch (Exception e) {
                // Log once properly or ignore specific framely error to avoid spam
            }
        }
    }

    // Reflection Helper Methods
    private int getIndex(Unit u) {
        try {
            return unitDrawIndexField.getInt(u);
        } catch (Exception e) {
            return -1;
        }
    }

    private void setIndex(Unit u, int val) {
        try {
            unitDrawIndexField.setInt(u, val);
        } catch (Exception e) {
        }
    }
}
