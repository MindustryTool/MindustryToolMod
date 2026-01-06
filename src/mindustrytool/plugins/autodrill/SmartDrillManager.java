package mindustrytool.plugins.autodrill;

import arc.Core;

import arc.func.Cons;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Slider;
import arc.scene.ui.Label;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import mindustrytool.plugins.browser.ModKeybinds;
import mindustry.world.meta.Attribute;
import mindustry.type.Item;

/**
 * Smart Drill Manager - Automated resource mining with preview mode.
 * Uses same event-based approach as the original mod for reliable input.
 */
public class SmartDrillManager {

    // Settings keys
    public static final String SETTING_MECH_MAX_TILES = "smart-drill.mech.max-tiles";
    public static final String SETTING_MECH_MIN_ORES = "smart-drill.mech.min-ores";
    public static final String SETTING_PNEU_MAX_TILES = "smart-drill.pneu.max-tiles";
    public static final String SETTING_PNEU_MIN_ORES = "smart-drill.pneu.min-ores";
    public static final String SETTING_CLIFF_MAX_TILES = "smart-drill.cliff.max-tiles";
    public static final String SETTING_PLASMA_MAX_TILES = "smart-drill.plasma.max-tiles";

    // Legacy/Global keys (kept for compatibility or general settings)

    private static final int BUTTON_SIZE = 30;

    private boolean enabled = true;
    private Tile selectedTile = null;
    private Direction selectedDirection = null;
    private Block selectedDrill = null;

    private final Table selectTable = new Table();
    private final Table directionTable = new Table();

    // Preview mode
    private Seq<BuildPlan> previewPlans = new Seq<>();
    private boolean showPreview = false;

    // Direction action callback
    private Cons<Direction> directionAction;

    public SmartDrillManager() {
        init();
    }

    private void init() {
        initSettings();
        buildSelectTable();
        buildDirectionTable();
        // registerEvents(); // Moved to PlayerConnectPlugin for true lazy loading
        // registerDraw(); // Moved to PlayerConnectPlugin for true lazy loading

        Log.info("[SmartDrill] Manager initialized - tap on resource tiles when enabled");
    }

    private void initSettings() {
        // Defaults
        if (!Core.settings.has(SETTING_MECH_MAX_TILES))
            Core.settings.put(SETTING_MECH_MAX_TILES, 200);
        if (!Core.settings.has(SETTING_MECH_MIN_ORES))
            Core.settings.put(SETTING_MECH_MIN_ORES, 1);

        if (!Core.settings.has(SETTING_PNEU_MAX_TILES))
            Core.settings.put(SETTING_PNEU_MAX_TILES, 150);
        if (!Core.settings.has(SETTING_PNEU_MIN_ORES))
            Core.settings.put(SETTING_PNEU_MIN_ORES, 2);

        if (!Core.settings.has(SETTING_CLIFF_MAX_TILES))
            Core.settings.put(SETTING_CLIFF_MAX_TILES, 100);
        if (!Core.settings.has(SETTING_PLASMA_MAX_TILES))
            Core.settings.put(SETTING_PLASMA_MAX_TILES, 100);
    }

    public void handleTap(EventType.TapEvent event) {
        if (!enabled)
            return;

        // Fix: Ignore taps if the tile is already occupied by a building
        if (event.tile.build != null)
            return;

        // Re-add tables if they were removed (e.g. client load / world load cleared the
        // stage)
        if (selectTable.getScene() == null) {
            Core.scene.root.addChildAt(0, selectTable);
        }
        if (directionTable.getScene() == null) {
            Core.scene.root.addChildAt(0, directionTable);
        }

        // Force remove background (in case of hotswap retaining old style)
        selectTable.background(null);
        directionTable.background(null);

        selectedTile = event.tile;
        selectTable.visible = true;

        updateSelectTable();

        // Position table above the tapped tile
        Vec2 v = Core.camera.project(selectedTile.centerX() * Vars.tilesize,
                (selectedTile.centerY() + 1) * Vars.tilesize);
        selectTable.setPosition(v.x, v.y, Align.bottom);
        directionTable.setPosition(v.x, v.y, Align.bottom);

        // Visual feedback
        Fx.tapBlock.at(selectedTile.getX(), selectedTile.getY());

        Log.info("[SmartDrill] Tile: @,@ | floor: @ | overlay: @ | block: @ | drop: @ | wallDrop: @ | solid: @",
                event.tile.x, event.tile.y,
                event.tile.floor(), event.tile.overlay(), event.tile.block(),
                event.tile.drop(), event.tile.wallDrop(), event.tile.solid());
    }

    public void update() {
        if (!Vars.state.isGame())
            return;
        if (Core.scene.hasField())
            return;

        if (Core.input.keyTap(ModKeybinds.smartDrillToggle)) {
            toggle();
        }
    }

    public void draw() {
        if (!enabled || !showPreview || previewPlans.isEmpty())
            return;

        Draw.reset();
        Draw.z(80);

        for (BuildPlan plan : previewPlans) {
            float x = plan.x * Vars.tilesize + plan.block.offset;
            float y = plan.y * Vars.tilesize + plan.block.offset;

            Draw.color(Pal.accent, 0.5f);
            Lines.stroke(2f);
            float size = plan.block.size * Vars.tilesize;
            Lines.rect(x - size / 2f, y - size / 2f, size, size);

            Draw.color(1f, 1f, 1f, 0.6f);
            Draw.rect(plan.block.fullIcon, x, y, size * 0.8f, size * 0.8f);
        }
        Draw.reset();
    }

    public void toggle() {
        enabled = !enabled;
        selectTable.visible = false;
        directionTable.visible = false;
        previewPlans.clear();
        showPreview = false;

        Vars.ui.showInfoToast(
                enabled ? "[accent]Smart Drill[]: Enabled - Tap on resource tiles" : "[gray]Smart Drill[]: Disabled",
                1.5f);
    }

    private void buildSelectTable() {
        selectTable.update(() -> {
            if (Vars.state.isMenu()) {
                selectTable.visible = false;
                return;
            }
            if (selectedTile != null && selectTable.visible) {
                Vec2 v = Core.camera.project(selectedTile.centerX() * Vars.tilesize,
                        (selectedTile.centerY() + 1) * Vars.tilesize);
                selectTable.setPosition(v.x, v.y, Align.bottom);
            }
        });

        // Hide when clicking outside
        Core.input.addProcessor(new InputProcessor() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button) {
                if (!selectTable.hasMouse())
                    selectTable.visible = false;
                return false;
            }
        });

        selectTable.pack();
        selectTable.act(0);
        selectTable.background(null); // Force transparent
        selectTable.visible = false; // Fix: Hide by default
        Core.scene.root.addChildAt(0, selectTable);
    }

    private void updateSelectTable() {
        selectTable.clear();
        if (selectedTile == null)
            return;

        // Serpulo drills
        if (Blocks.mechanicalDrill.environmentBuildable() && ((Drill) Blocks.mechanicalDrill).canMine(selectedTile)) {
            addDrillButton(Blocks.mechanicalDrill, () -> {
                selectTable.visible = false;
                directionTable.visible = true;
                directionAction = dir -> executeAndQueue(
                        DrillFiller.fillBridgeDrill(selectedTile, (Drill) Blocks.mechanicalDrill, dir));
            });
        }

        if (Blocks.pneumaticDrill.environmentBuildable() && ((Drill) Blocks.pneumaticDrill).canMine(selectedTile)) {
            addDrillButton(Blocks.pneumaticDrill, () -> {
                selectTable.visible = false;
                directionTable.visible = true;
                directionAction = dir -> executeAndQueue(
                        DrillFiller.fillBridgeDrill(selectedTile, (Drill) Blocks.pneumaticDrill, dir));
            });
        }

        // Erekir drills - check for wall resources
        Item wallDrop = selectedTile.wallDrop();
        boolean hasSandAttribute = selectedTile.block().attributes.get(Attribute.sand) > 0;

        if (wallDrop != null || hasSandAttribute) {
            // Cliff Crusher - for sand from walls
            if (Blocks.cliffCrusher.environmentBuildable()) {
                boolean valid = false;
                // Cliff Crusher is WallCrafter, strictly requires Attribute.sand
                if (hasSandAttribute)
                    valid = true;

                if (valid) {
                    addDrillButton(Blocks.cliffCrusher, () -> {
                        selectTable.visible = false;
                        directionTable.visible = true;
                        directionAction = dir -> executeAndQueue(
                                DrillFiller.fillWallDrill(selectedTile, Blocks.cliffCrusher, dir));
                    });
                }
            }

            // Plasma Bore - for other wall resources
            if (Blocks.plasmaBore.environmentBuildable() && wallDrop != null
                    && wallDrop.hardness <= ((BeamDrill) Blocks.plasmaBore).tier) {
                addDrillButton(Blocks.plasmaBore, () -> {
                    selectTable.visible = false;
                    directionTable.visible = true;
                    directionAction = dir -> executeAndQueue(
                            DrillFiller.fillWallDrill(selectedTile, (BeamDrill) Blocks.plasmaBore, dir));
                });
            }
        }

        selectTable.pack();
    }

    private void addDrillButton(Block drill, Runnable action) {
        ImageButton btn = selectTable.button(new TextureRegionDrawable(drill.uiIcon), Styles.defaulti, action).get();
        btn.resizeImage(BUTTON_SIZE);
    }

    private void executeAndQueue(Seq<BuildPlan> plans) {
        if (plans.isEmpty()) {
            Vars.ui.showInfoToast("[orange]No valid placements found", 1.5f);
            return;
        }

        for (BuildPlan plan : plans) {
            Vars.player.unit().addBuild(plan);
        }

        Vars.ui.showInfoToast("[accent]Queued " + plans.size + " buildings", 1.5f);
        // Keep enabled = true so user can continue tapping without pressing keybind
        // again
    }

    private void buildDirectionTable() {
        directionTable.update(() -> {
            if (Vars.state.isMenu()) {
                directionTable.visible = false;
                return;
            }
            if (selectedTile != null && directionTable.visible) {
                Vec2 v = Core.camera.project(selectedTile.centerX() * Vars.tilesize,
                        (selectedTile.centerY() + 1) * Vars.tilesize);
                directionTable.setPosition(v.x, v.y, Align.bottom);
            }
        });

        // Up button
        directionTable.table().get().button(Icon.up, Styles.defaulti, () -> {
            if (directionAction != null)
                directionAction.get(Direction.UP);
            directionTable.visible = false;
        }).get().resizeImage(BUTTON_SIZE);

        directionTable.row();

        // Left, Cancel, Right
        Table row2 = directionTable.table().get();
        row2.button(Icon.left, Styles.defaulti, () -> {
            if (directionAction != null)
                directionAction.get(Direction.LEFT);
            directionTable.visible = false;
        }).get().resizeImage(BUTTON_SIZE);
        row2.button(Icon.cancel, Styles.defaulti, () -> {
            directionTable.visible = false;
        }).get().resizeImage(BUTTON_SIZE);
        row2.button(Icon.right, Styles.defaulti, () -> {
            if (directionAction != null)
                directionAction.get(Direction.RIGHT);
            directionTable.visible = false;
        }).get().resizeImage(BUTTON_SIZE);

        directionTable.row();

        // Down button
        directionTable.table().get().button(Icon.down, Styles.defaulti, () -> {
            if (directionAction != null)
                directionAction.get(Direction.DOWN);
            directionTable.visible = false;
        }).get().resizeImage(BUTTON_SIZE);

        // Hide when clicking outside
        Core.input.addProcessor(new InputProcessor() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button) {
                if (!directionTable.hasMouse())
                    directionTable.visible = false;
                return false;
            }
        });

        directionTable.pack();
        directionTable.act(0);
        directionTable.visible = false; // Fix: Hide by default
        Core.scene.root.addChildAt(0, directionTable);
    }

    private void resetSettings() {
        Core.settings.put(SETTING_MECH_MAX_TILES, 200);
        Core.settings.put(SETTING_MECH_MIN_ORES, 1);
        Core.settings.put(SETTING_PNEU_MAX_TILES, 150);
        Core.settings.put(SETTING_PNEU_MIN_ORES, 2);
        Core.settings.put(SETTING_CLIFF_MAX_TILES, 100);
        Core.settings.put(SETTING_PLASMA_MAX_TILES, 100);
    }

    public void showSettings() {
        BaseDialog dialog = new BaseDialog("Smart Drill Settings");
        dialog.addCloseButton();

        dialog.buttons.button("Reset to defaults", Icon.refresh, () -> {
            resetSettings();
            dialog.hide();
            showSettings();
        }).size(250f, 64f);

        Table cont = dialog.cont;
        cont.clear();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        cont.pane(t -> {
            t.top().left();
            t.defaults().pad(4).left();

            // Mechanical Drill
            addDrillSetting(t, "Mechanical Drill", Blocks.mechanicalDrill, SETTING_MECH_MAX_TILES,
                    SETTING_MECH_MIN_ORES);
            t.image().color(Pal.gray).height(2).pad(10, 0, 10, 0).growX().row();

            // Pneumatic Drill
            addDrillSetting(t, "Pneumatic Drill", Blocks.pneumaticDrill, SETTING_PNEU_MAX_TILES, SETTING_PNEU_MIN_ORES);
            t.image().color(Pal.gray).height(2).pad(10, 0, 10, 0).growX().row();

            // Cliff Crusher
            addDrillSetting(t, "Cliff Crusher", Blocks.cliffCrusher, SETTING_CLIFF_MAX_TILES, null);
            t.image().color(Pal.gray).height(2).pad(10, 0, 10, 0).growX().row();

            // Plasma Bore
            addDrillSetting(t, "Plasma Bore", Blocks.plasmaBore, SETTING_PLASMA_MAX_TILES, null);
            t.image().color(Pal.gray).height(2).pad(10, 0, 10, 0).growX().row();

        }).width(width).maxHeight(Core.graphics.getHeight() * 0.8f).row();

        dialog.show();
    }

    private void addDrillSetting(Table table, String title, Block block, String maxTilesKey, String minOresKey) {
        table.table(t -> {
            t.left();
            t.image(block.uiIcon).size(24).padRight(10);
            t.add(title).color(Pal.accent);
        }).padBottom(4).row();

        // Max Tiles
        Table maxTilesStack = new Table();
        Slider maxTilesSlider = new Slider(10, 1000, 10, false);
        maxTilesSlider.setValue(Core.settings.getInt(maxTilesKey));

        Label maxTilesLabel = new Label(String.valueOf((int) maxTilesSlider.getValue()), Styles.outlineLabel);

        maxTilesSlider.changed(() -> {
            Core.settings.put(maxTilesKey, (int) maxTilesSlider.getValue());
            maxTilesLabel.setText(String.valueOf((int) maxTilesSlider.getValue()));
        });

        Table maxTilesContent = new Table();
        maxTilesContent.touchable = Touchable.disabled;
        maxTilesContent.margin(3f, 33f, 3f, 33f);
        maxTilesContent.add("Max Tiles", Styles.outlineLabel).left().growX();
        maxTilesContent.add(maxTilesLabel).padLeft(10f).right();

        maxTilesStack.stack(maxTilesSlider, maxTilesContent).height(40f).growX();
        table.add(maxTilesStack).growX().row();

        // Min Ores (if applicable)
        if (minOresKey != null) {
            Table minOresStack = new Table();
            Slider minOresSlider = new Slider(1, 20, 1, false);
            minOresSlider.setValue(Core.settings.getInt(minOresKey));

            Label minOresLabel = new Label(String.valueOf((int) minOresSlider.getValue()), Styles.outlineLabel);

            minOresSlider.changed(() -> {
                Core.settings.put(minOresKey, (int) minOresSlider.getValue());
                minOresLabel.setText(String.valueOf((int) minOresSlider.getValue()));
            });

            Table minOresContent = new Table();
            minOresContent.touchable = Touchable.disabled;
            minOresContent.margin(3f, 33f, 3f, 33f);
            minOresContent.add("Min Ores", Styles.outlineLabel).left().growX();
            minOresContent.add(minOresLabel).padLeft(10f).right();

            minOresStack.stack(minOresSlider, minOresContent).height(40f).growX();
            table.add(minOresStack).growX().padTop(4).row();
        }
    }

    // Draw logic moved to draw() method for lazy loading

    public void dispose() {
        selectTable.remove();
        directionTable.remove();
        Log.info("[SmartDrill] Manager disposed");
    }
}
