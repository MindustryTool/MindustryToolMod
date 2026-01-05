package mindustrytool.features.gameplay.autoconveyor;

import arc.Core;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.math.geom.Vec2;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import arc.struct.Seq;
import arc.scene.ui.Label;
import arc.scene.ui.Image;

public class AutoConveyorOverlay extends Table {

    private Tile startTile;
    private Block activeBlock;

    private final Runnable onCancel;
    private final Runnable onConfirm;

    private boolean isPreviewing = false;

    private Table contentTable = new Table();
    private Image blockIcon = new Image();
    private Label statusLabel = new Label("");

    public AutoConveyorOverlay(Runnable onConfirm, Runnable onCancel) {
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        background(Styles.black6);
        visible = false;

        buildUI();

        update(() -> {
            if (!visible || startTile == null)
                return;

            // Sync Position to Start Tile
            Vec2 v = Core.camera.project(startTile.centerX() * Vars.tilesize,
                    (startTile.centerY() + 1) * Vars.tilesize);
            setPosition(v.x, v.y, Align.bottom);

            // Close if menu opened
            if (Vars.state.isMenu())
                visible = false;
        });
    }

    private void buildUI() {
        // Header
        add(statusLabel).style(Styles.outlineLabel).padBottom(4).row();

        // Block Icon
        add(blockIcon).size(32).padBottom(8).row();

        add(contentTable).growX().row();
    }

    public void showStart(Tile tile, Block heldBlock) {
        this.startTile = tile;
        this.activeBlock = heldBlock;
        this.isPreviewing = false;

        this.visible = true;
        rebuildIdle();
        pack();

        // Add to scene if not present
        if (getScene() == null) {
            Core.scene.root.addChild(this);
        }
    }

    public void showPreview(Block heldBlock) {
        this.activeBlock = heldBlock;
        this.isPreviewing = true;
        rebuildPreview();
        pack();
    }

    private void rebuildIdle() {
        contentTable.clear();
        statusLabel.setText("Select End Point");
        blockIcon.setDrawable(new arc.scene.style.TextureRegionDrawable(activeBlock.uiIcon));

        // Toggles
        contentTable.table(t -> {
            t.defaults().size(40).pad(2);

            // Junction
            t.button(new arc.scene.style.TextureRegionDrawable(mindustry.content.Blocks.junction.uiIcon), () -> {
                Core.settings.put(AutoConveyorSettings.SETTING_USE_JUNCTION, !AutoConveyorSettings.isUseJunction());
            }).get().setStyle(Styles.clearTogglei);

            setupButton(t, mindustry.content.Blocks.junction,
                    AutoConveyorSettings.isUseJunction(),
                    () -> Core.settings.put(AutoConveyorSettings.SETTING_USE_JUNCTION,
                            !AutoConveyorSettings.isUseJunction()),
                    "Auto Junction");

            setupButton(t, mindustry.content.Blocks.itemBridge,
                    AutoConveyorSettings.isUseBridge(),
                    () -> Core.settings.put(AutoConveyorSettings.SETTING_USE_BRIDGE,
                            !AutoConveyorSettings.isUseBridge()),
                    "Auto Bridge");

            setupButton(t, mindustry.content.Blocks.phaseConveyor,
                    AutoConveyorSettings.isUsePhase(),
                    () -> Core.settings.put(AutoConveyorSettings.SETTING_USE_PHASE, !AutoConveyorSettings.isUsePhase()),
                    "Auto Phase");

            t.row();

            // Destructive
            ImageButton bDest = t.button(Icon.hammer, () -> {
                Core.settings.put(AutoConveyorSettings.SETTING_DESTRUCTIVE, !AutoConveyorSettings.isDestructive());
            }).tooltip("Destructive Pathing").get();
            bDest.setStyle(Styles.clearTogglei);
            bDest.setChecked(AutoConveyorSettings.isDestructive());

            // Algorithm
            ImageButton bAlgo = t.button(Icon.settings, () -> {
                AutoConveyorSettings.Algorithm[] vals = AutoConveyorSettings.Algorithm.values();
                int idx = AutoConveyorSettings.getAlgorithm().ordinal();
                int next = (idx + 1) % vals.length;
                Core.settings.put(AutoConveyorSettings.SETTING_ALGORITHM, vals[next].name());
                Vars.ui.showInfoToast("Algo: " + vals[next].label, 1f);
            }).tooltip("Cycle Algorithm").get();
            bAlgo.setStyle(Styles.clearTogglei);

        }).row();

        // Cancel
        contentTable.button("Cancel", Icon.cancel, onCancel).size(120, 40).padTop(4);
    }

    private void setupButton(Table t, Block iconBlock, boolean check, Runnable run, String tooltip) {
        ImageButton b = t.button(new arc.scene.style.TextureRegionDrawable(iconBlock.uiIcon), run).tooltip(tooltip)
                .get();
        b.setStyle(Styles.clearTogglei);
        b.setChecked(check);
    }

    private void rebuildPreview() {
        contentTable.clear();
        statusLabel.setText("Preview: " + activeBlock.localizedName);
        blockIcon.setDrawable(new arc.scene.style.TextureRegionDrawable(activeBlock.uiIcon));

        Table buttons = new Table();
        buttons.defaults().size(60, 40).pad(4);

        ImageButton bCancel = buttons.button(Icon.cancel, onCancel).tooltip("Cancel").get();
        bCancel.setStyle(Styles.clearNonei);

        ImageButton bOk = buttons.button(Icon.ok, onConfirm).tooltip("Build Settings").get();
        bOk.setStyle(Styles.defaulti);

        contentTable.add(buttons).row();
    }

    public void hide() {
        this.visible = false;
        this.startTile = null;
    }
}
