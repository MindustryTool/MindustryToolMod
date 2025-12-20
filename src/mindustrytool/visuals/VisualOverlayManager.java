package mindustrytool.visuals;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.geom.Rect;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.defense.RegenProjector;
import mindustry.world.blocks.defense.ShockMine;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.distribution.MassDriver;
import mindustry.world.blocks.logic.LogicBlock;

import mindustry.world.blocks.payloads.PayloadMassDriver;
import mindustry.world.blocks.units.RepairTower;

import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.LiquidTurret;
import mindustrytool.ui.DualContentSelectionTable;

/**
 * Displays strategic overlay information such as turret ranges, projector
 * zones,
 * and spawn points. Optimized for performance with spatial queries and zoom
 * threshold.
 */
public class VisualOverlayManager {

    // Configuration toggles
    private boolean drawPlayerCursors = false;
    private boolean drawBlockRanges = false; // Unified toggle for all blocks
    private boolean drawSpawnpoints = true;
    private boolean drawUnitRanges = false;

    // Visual settings
    private float rangeTransparency = 0.3f;
    private float zoomThreshold = 0.1f; // Stop drawing when zoomed out beyond this (lower = more zoom out allowed)
    private boolean hideInactiveRanges = false;
    private boolean fillRanges = false;

    // Cache for camera bounds
    private final Rect cameraRect = new Rect();

    // Own filter data (separate from Entity Hider)
    private final ObjectSet<UnlockableContent> filterContent = new ObjectSet<>();

    private BaseDialog dialog;

    public VisualOverlayManager() {
        Events.run(EventType.Trigger.draw, this::renderOverlays);
        loadSettings();
        buildDialog();
    }

    private void loadSettings() {
        drawPlayerCursors = Core.settings.getBool("mindustrytool.overlay.cursors", false);
        drawBlockRanges = Core.settings.getBool("mindustrytool.overlay.blockRanges", true);
        drawUnitRanges = Core.settings.getBool("mindustrytool.overlay.unitRanges", false);
        rangeTransparency = Core.settings.getFloat("mindustrytool.overlay.transparency", 0.3f);
        zoomThreshold = Core.settings.getFloat("mindustrytool.overlay.zoomThreshold", 0.1f);
        hideInactiveRanges = Core.settings.getBool("mindustrytool.overlay.hideInactive", false);
        fillRanges = Core.settings.getBool("mindustrytool.overlay.fillRanges", false);
    }

    private void buildDialog() {
        dialog = new BaseDialog("Strategic Overlays");
        dialog.addCloseButton();
        dialog.buttons.button("Reset to defaults", mindustry.gen.Icon.refresh, this::resetToDefaults).size(250, 64);

        rebuildCont();
    }

    private void rebuildCont() {
        Table cont = dialog.cont;
        cont.clear();
        cont.defaults().pad(6).left();

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

        // --- Transparency Slider ---
        Slider transSlider = new Slider(0.1f, 1f, 0.05f, false);
        transSlider.setValue(rangeTransparency);

        Label transLabel = new Label(Math.round(rangeTransparency * 100) + "%", Styles.outlineLabel);

        Table transContent = new Table();
        transContent.add("Transparency", Styles.outlineLabel).left().growX();
        transContent.add(transLabel).padLeft(10f).right();
        transContent.margin(3f, 33f, 3f, 33f);
        transContent.touchable = arc.scene.event.Touchable.disabled;

        transSlider.changed(() -> {
            rangeTransparency = transSlider.getValue();
            transLabel.setText(Math.round(rangeTransparency * 100) + "%");
            Core.settings.put("mindustrytool.overlay.transparency", rangeTransparency);
        });

        cont.stack(transSlider, transContent).width(width).left().padTop(4f).row();

        // --- Zoom Threshold Slider ---
        Slider zoomSlider = new Slider(0.1f, 2f, 0.1f, false);
        zoomSlider.setValue(zoomThreshold);

        Label zoomLabel = new Label(String.format("%.1fx", zoomThreshold), Styles.outlineLabel);

        Table zoomContent = new Table();
        zoomContent.add("Zoom Threshold", Styles.outlineLabel).left().growX();
        zoomContent.add(zoomLabel).padLeft(10f).right();
        zoomContent.margin(3f, 33f, 3f, 33f);
        zoomContent.touchable = arc.scene.event.Touchable.disabled;

        zoomSlider.changed(() -> {
            zoomThreshold = zoomSlider.getValue();
            zoomLabel.setText(String.format("%.1fx", zoomThreshold));
            Core.settings.put("mindustrytool.overlay.zoomThreshold", zoomThreshold);
        });

        cont.stack(zoomSlider, zoomContent).width(width).left().padTop(4f).row();

        // --- Checkboxes ---
        cont.check("Player Cursors", drawPlayerCursors, b -> {
            drawPlayerCursors = b;
            Core.settings.put("mindustrytool.overlay.cursors", b);
        }).left().padTop(4).row();

        cont.check("Block Ranges", drawBlockRanges, b -> {
            drawBlockRanges = b;
            Core.settings.put("mindustrytool.overlay.blockRanges", b);
        }).left().padTop(4).row();

        cont.check("Unit Ranges", drawUnitRanges, b -> {
            drawUnitRanges = b;
            Core.settings.put("mindustrytool.overlay.unitRanges", b);
        }).left().padTop(4).row();

        cont.check("Hide Inactive Ranges", hideInactiveRanges, b -> {
            hideInactiveRanges = b;
            Core.settings.put("mindustrytool.overlay.hideInactive", b);
        }).left().padTop(4).row();

        cont.check("Hybrid Fill Ranges", fillRanges, b -> {
            fillRanges = b;
            Core.settings.put("mindustrytool.overlay.fillRanges", b);
        }).left().padTop(4).row();

        // --- Advanced Filters Button ---
        // Align button width with sliders for consistency
        cont.button("Advanced Filters", this::showFilterDialog)
                .size(width, 45).left().row();
    }

    private BaseDialog filterDialog; // Lazy loading dialog

    private void resetToDefaults() {
        drawPlayerCursors = false;
        drawPlayerCursors = false;
        drawBlockRanges = true;
        drawSpawnpoints = true;
        drawUnitRanges = false;
        rangeTransparency = 0.3f;
        zoomThreshold = 0.5f;

        Core.settings.put("mindustrytool.overlay.cursors", false);
        Core.settings.put("mindustrytool.overlay.blockRanges", true);
        Core.settings.put("mindustrytool.overlay.spawnpoints", true);
        Core.settings.put("mindustrytool.overlay.unitRanges", false);
        Core.settings.put("mindustrytool.overlay.transparency", 0.3f);
        Core.settings.put("mindustrytool.overlay.zoomThreshold", 0.1f);
        Core.settings.put("mindustrytool.overlay.hideInactive", false);
        Core.settings.put("mindustrytool.overlay.fillRanges", false);

        Core.settings.put("mindustrytool.overlay.fillRanges", false);

        filterDialog = null; // Force rebuild dialog
        filterContent.clear();

        rebuildCont();
    }

    private void showFilterDialog() {
        if (filterDialog == null) {
            initFilterDialog();
        }
        filterDialog.show();
    }

    private void initFilterDialog() {
        filterDialog = new BaseDialog("Advanced Range Filters");
        filterDialog.addCloseButton();
        Table fCont = filterDialog.cont;

        Table tabs = new Table();

        // 1. TURRETS Tab
        Table turretTable = new Table();
        turretTable.add(new DualContentSelectionTable(
                Vars.content.blocks().select(b -> b instanceof BaseTurret),
                filterContent, "Hidden Turrets", "Visible Turrets", () -> {
                })).grow();

        // 2. PROJECTORS Tab
        Table projectorTable = new Table();
        projectorTable.add(new DualContentSelectionTable(
                Vars.content.blocks().select(b -> b instanceof OverdriveProjector || b instanceof MendProjector
                        || b instanceof RegenProjector || b instanceof ForceProjector),
                filterContent, "Hidden Projectors", "Visible Projectors", () -> {
                })).grow();

        // 3. UTILITIES Tab
        Table utilTable = new Table();
        // Added PayloadMassDriver and RepairTower
        utilTable.add(new DualContentSelectionTable(
                Vars.content.blocks().select(b -> b instanceof MassDriver || b instanceof LogicBlock
                        || b instanceof ShockMine
                        || b instanceof PayloadMassDriver || b instanceof RepairTower
                        // Safe name checks for modded/specific blocks
                        || b.name.equals("repair-point") || b.name.equals("repair-turret")
                        || b.name.equals("shockwave-tower")),
                filterContent, "Hidden Utilities", "Visible Utilities", () -> {
                })).grow();

        // 4. UNITS Tab
        Table unitTable = new Table();
        unitTable.add(new DualContentSelectionTable(
                Vars.content.units(), filterContent, "Hidden Units", "Visible Units", () -> {
                })).grow();

        // Default tab
        tabs.add(turretTable).grow();

        arc.scene.ui.ButtonGroup<arc.scene.ui.TextButton> group = new arc.scene.ui.ButtonGroup<>();
        fCont.table(t -> {
            t.defaults().growX().height(50);
            t.button("Turrets", Styles.togglet, () -> {
                tabs.clear();
                tabs.add(turretTable).grow();
            }).group(group).checked(true);

            t.button("Projectors", Styles.togglet, () -> {
                tabs.clear();
                tabs.add(projectorTable).grow();
            }).group(group);

            t.button("Utilities", Styles.togglet, () -> {
                tabs.clear();
                tabs.add(utilTable).grow();
            }).group(group);

            t.button("Units", Styles.togglet, () -> {
                tabs.clear();
                tabs.add(unitTable).grow();
            }).group(group);
        }).growX().pad(5).row();

        fCont.add(tabs).grow().row();
    }

    public void showDialog() {
        if (dialog != null)
            dialog.show();
    }

    // --- Rendering Logic ---

    private void renderBlockRanges() {
        renderTurretRanges();
        renderProjectorZones();
        renderUtilRanges();
    }

    private void renderOverlays() {
        if (!Vars.state.isGame())
            return;

        // Zoom Threshold Safeguard: Don't render if zoomed out too far
        float zoom = Vars.renderer.getScale();
        if (zoom < zoomThreshold)
            return;

        // Build camera rect for spatial queries
        float camX = Core.camera.position.x;
        float camY = Core.camera.position.y;
        float camW = Core.camera.width;
        float camH = Core.camera.height;
        cameraRect.set(camX - camW / 2, camY - camH / 2, camW, camH);

        try {
            Draw.z(Layer.overlayUI);

            if (drawPlayerCursors)
                renderPlayerCursors();
            if (drawSpawnpoints)
                renderSpawnpoints();
            if (drawBlockRanges) {
                renderBlockRanges();
            }
            if (drawUnitRanges)
                renderUnitRanges();

            Draw.reset();
        } catch (Exception e) {
            // Safe Render: Never crash the game
        }
    }

    private void renderPlayerCursors() {
        Groups.player.each(p -> {
            if (p == Vars.player || p.dead())
                return;
            float x = p.mouseX;
            float y = p.mouseY;

            // Draw aim line if within camera
            if (p.unit() != null) {
                float aimX = p.unit().aimX;
                float aimY = p.unit().aimY;

                // Simple dash line to aim target
                if (cameraRect.contains(p.unit().x, p.unit().y) || cameraRect.contains(aimX, aimY)) {
                    Draw.color(p.team().color, rangeTransparency);
                    Lines.dashLine(p.unit().x, p.unit().y, aimX, aimY,
                            (int) (arc.math.Mathf.dst(p.unit().x, p.unit().y, aimX, aimY) / 8f));
                }
            }

            if (!cameraRect.contains(x, y))
                return;

            Draw.color(p.team().color, rangeTransparency);
            Lines.stroke(1.5f);
            Lines.line(x - 8, y, x + 8, y);
            Lines.line(x, y - 8, x, y + 8);
        });
    }

    private void renderProjectorZones() {
        // Use tile iteration for all teams' projectors in camera view
        float padding = 400f;
        Rect queryRect = new Rect(cameraRect.x - padding, cameraRect.y - padding, cameraRect.width + padding * 2,
                cameraRect.height + padding * 2);

        int minX = Math.max(0, (int) (queryRect.x / 8));
        int minY = Math.max(0, (int) (queryRect.y / 8));
        int maxX = Math.min(Vars.world.width() - 1, (int) ((queryRect.x + queryRect.width) / 8));
        int maxY = Math.min(Vars.world.height() - 1, (int) ((queryRect.y + queryRect.height) / 8));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                var tile = Vars.world.tile(x, y);
                if (tile == null || tile.build == null)
                    continue;

                // Optimization: Only render on the main tile of the block to prevent duplicate
                // drawing (stutter/opacity issues)
                if (tile.build.tile != tile)
                    continue;

                if (tile.build instanceof OverdriveProjector.OverdriveBuild ob) {
                    OverdriveProjector block = (OverdriveProjector) ob.block;
                    if (filterContent.contains(block))
                        continue; // Filter check

                    float range = block.range + ob.phaseHeat * block.phaseRangeBoost;

                    if (ob.efficiency <= 0) {
                        if (hideInactiveRanges)
                            continue; // Skip if setting is enabled
                        Draw.color(Color.gray, rangeTransparency);
                    } else {
                        Draw.color(Color.valueOf("feb380"), rangeTransparency); // Overdrive color
                    }

                    if (fillRanges) {
                        Draw.alpha(rangeTransparency * 0.2f);
                        Fill.circle(ob.x, ob.y, range);
                        Draw.alpha(rangeTransparency); // Reset for stroke
                    }

                    Lines.stroke(1.5f);
                    Lines.circle(ob.x, ob.y, range);
                } else if (tile.build instanceof MendProjector.MendBuild mb) {
                    MendProjector block = (MendProjector) mb.block;
                    if (filterContent.contains(block))
                        continue; // Filter check

                    float range = block.range;

                    if (mb.efficiency <= 0) {
                        if (hideInactiveRanges)
                            continue; // Skip if setting is enabled
                        Draw.color(Color.gray, rangeTransparency);
                    } else {
                        Draw.color(Pal.heal, rangeTransparency); // Heal green
                    }

                    if (fillRanges) {
                        Draw.alpha(rangeTransparency * 0.2f);
                        Fill.circle(mb.x, mb.y, range); // Circle for Mend
                        Draw.alpha(rangeTransparency); // Reset for stroke
                    }

                    Lines.stroke(1.5f);
                    Lines.circle(mb.x, mb.y, range);

                } else if (tile.build instanceof RegenProjector.RegenProjectorBuild rb) {
                    RegenProjector block = (RegenProjector) rb.block;
                    if (filterContent.contains(block))
                        continue; // Filter check

                    // Regen Projector range is typically in tiles in some mods/contexts.
                    // User complained about size being wrong (likely too small). Scale by tilesize.
                    float range = block.range * Vars.tilesize;

                    if (rb.efficiency <= 0) {
                        if (hideInactiveRanges)
                            continue; // Skip if setting is enabled
                        Draw.color(Color.gray, rangeTransparency);
                    } else {
                        Draw.color(Color.lime, rangeTransparency);
                    }

                    if (fillRanges) {
                        Draw.alpha(rangeTransparency * 0.2f);
                        Fill.rect(rb.x, rb.y, range, range);
                        Draw.alpha(rangeTransparency); // Reset for stroke
                    }

                    Lines.stroke(1.5f);
                    // Draw box for Regen Projector
                    Lines.rect(rb.x - range / 2f, rb.y - range / 2f, range, range);
                } else if (tile.build instanceof ForceProjector.ForceBuild fb) {
                    ForceProjector block = (ForceProjector) fb.block;
                    if (filterContent.contains(block))
                        continue; // Filter check

                    float range = block.radius;
                    if (fb.efficiency <= 0 || fb.broken) {
                        if (hideInactiveRanges)
                            continue;
                        Draw.color(Color.gray, rangeTransparency);
                    } else {
                        Draw.color(Color.royal, rangeTransparency);
                    }

                    if (fillRanges) {
                        Draw.alpha(rangeTransparency * 0.2f);
                        Fill.poly(fb.x, fb.y, 6, range);
                        Draw.alpha(rangeTransparency);
                    }

                    Lines.stroke(1.5f);
                    Lines.poly(fb.x, fb.y, 6, range);

                    // Fallback for Build Tower by name (since class generic unavailable)
                    // REMOVED: Build Tower manual check (Moved to renderUtilRanges to avoid
                    // duplication)
                }
            }
        }
    }

    private void renderSpawnpoints() {
        if (!Vars.state.hasSpawns())
            return;
        Draw.color(Color.scarlet, rangeTransparency);
        Lines.stroke(1.5f);
        for (var tile : Vars.spawner.getSpawns()) {
            if (!cameraRect.contains(tile.worldx(), tile.worldy()))
                continue;
            Lines.dashCircle(tile.worldx(), tile.worldy(), Vars.state.rules.dropZoneRadius);
        }
    }

    private void renderTurretRanges() {
        // Expand query to check for off-screen turrets
        float padding = 2500f;
        Rect queryRect = new Rect(cameraRect.x - padding, cameraRect.y - padding, cameraRect.width + padding * 2,
                cameraRect.height + padding * 2);

        // Render turrets for ALL teams (including enemies)
        for (mindustry.game.Team team : mindustry.game.Team.all) {
            if (team.data().buildings.size == 0)
                continue; // Skip empty teams

            Vars.indexer.eachBlock(team, queryRect, b -> b instanceof BaseTurret.BaseTurretBuild, b -> {
                BaseTurret.BaseTurretBuild tb = (BaseTurret.BaseTurretBuild) b;
                // Skip turrets that are in our own filter (hidden)
                if (filterContent.contains(tb.block))
                    return;

                // Double check interaction with camera to avoid drawing completely off-screen
                // circles (optimization)
                if (!cameraRect.overlaps(tb.x - tb.range(), tb.y - tb.range(), tb.range() * 2, tb.range() * 2))
                    return;

                boolean hasAmmo = true;
                if (tb instanceof ItemTurret.ItemTurretBuild itb) {
                    // Check if block actually REQUIRES ammo (non-empty ammoTypes)
                    // Some modded turrets inherit ItemTurret but use no ammo (support/magic)
                    hasAmmo = itb.totalAmmo > 0 || ((ItemTurret) itb.block).ammoTypes.isEmpty();
                } else if (tb instanceof LiquidTurret.LiquidTurretBuild ltb) {
                    // Similar check for liquid (though less common to have empty requirements)
                    hasAmmo = ltb.liquids.currentAmount() > 0.01f;
                }

                // Force hasAmmo = true for any repair/healer block (they often have weird ammo
                // logic or use power only)
                if (tb.block.name.contains("repair") || tb.block.name.contains("healer")) {
                    hasAmmo = true;
                }

                // Strictly check if block CONSUMES power before flagging low efficiency
                // Some blocks have power nodes but don't consume (efficiency might vary)
                // We also check shouldConsume() to ensure we don't flag "Idle" blocks as "No
                // Power"
                // (e.g. Foreshadow only consumes while shooting/charging)
                boolean powerMissing = false;
                if (tb.power != null && tb.block.consumesPower) {
                    if (tb.efficiency > 0) {
                        powerMissing = false;
                    } else {
                        if (tb.shouldConsume()) {
                            powerMissing = true;
                        } else {
                            powerMissing = tb.power.graph.getSatisfaction() <= 0.0001f;
                        }
                    }
                }

                // Only mark inactive if:
                // 1. Needs power AND has no power
                // 2. Is disabled
                // 3. Needs ammo AND has no ammo
                boolean isInactive = powerMissing || !tb.enabled || !hasAmmo;

                if (isInactive) {
                    if (hideInactiveRanges)
                        return; // Skip if setting is enabled
                    Draw.color(Color.gray, rangeTransparency);
                } else {
                    // Customize color for Repair Turret
                    if (tb.block.name.equals("repair-turret")) {
                        Draw.color(Pal.heal, rangeTransparency);
                    } else {
                        Draw.color(tb.team.color, rangeTransparency);
                    }
                }

                if (fillRanges) {
                    Draw.alpha(rangeTransparency * 0.2f); // Faint fill
                    Fill.circle(tb.x, tb.y, tb.range());
                    Draw.alpha(rangeTransparency); // Reset for stroke
                }

                Lines.stroke(1.5f);

                float drawRange = tb.range();
                // Special check for Repair Turret to use repairRadius if available
                if (tb.block.name.contains("repair")) {
                    drawRange = getModdedRange(tb.block, tb.range());
                }

                Lines.circle(tb.x, tb.y, drawRange);
            });
        }
    }

    private void renderUnitRanges() {
        // Query larger area for units
        float padding = 2000f;
        Rect queryRect = new Rect(cameraRect.x - padding, cameraRect.y - padding, cameraRect.width + padding * 2,
                cameraRect.height + padding * 2);

        Groups.unit.intersect(queryRect.x, queryRect.y, queryRect.width, queryRect.height, u -> {
            if (u.dead())
                return;
            // Skip units that are in our own filter (hidden)
            if (filterContent.contains(u.type))
                return;

            // Optimization: check if range overlap camera
            if (!cameraRect.overlaps(u.x - u.range(), u.y - u.range(), u.range() * 2, u.range() * 2))
                return;

            Draw.color(u.team.color, rangeTransparency);

            if (fillRanges) {
                Draw.alpha(rangeTransparency * 0.2f);
                Fill.circle(u.x, u.y, u.range());
                Draw.alpha(rangeTransparency);
            }

            Lines.stroke(1.5f);
            Lines.circle(u.x, u.y, u.range());
        });
    }

    private void renderUtilRanges() {
        // Expand query slightly for these blocks
        float padding = 1000f;
        Rect queryRect = new Rect(cameraRect.x - padding, cameraRect.y - padding, cameraRect.width + padding * 2,
                cameraRect.height + padding * 2);

        for (mindustry.game.Team team : mindustry.game.Team.all) {
            if (team.data().buildings.size == 0)
                continue;

            Vars.indexer.eachBlock(team, queryRect, b -> b.block instanceof MassDriver ||
                    b.block instanceof LogicBlock ||
                    b.block instanceof ShockMine ||
                    b.block instanceof PayloadMassDriver ||
                    b.block instanceof RepairTower ||
                    (b.block.name.contains("repair") && !(b.block instanceof BaseTurret)) || // Strict BaseTurret
                                                                                             // exclusion
                    (b.block.name.contains("shockwave") && !(b.block instanceof BaseTurret)) ||
                    b.block.name.equals("build-tower"), b -> { // Explicitly add Build Tower

                        // SAFETY GUARD: Absolutely prevent Turrets from being drawn here
                        if (b instanceof BaseTurret.BaseTurretBuild)
                            return;

                        // Safe Guard:
                        if (b instanceof BaseTurret.BaseTurretBuild ||
                                b instanceof MendProjector.MendBuild ||
                                b instanceof OverdriveProjector.OverdriveBuild ||
                                b instanceof RegenProjector.RegenProjectorBuild ||
                                b instanceof ForceProjector.ForceBuild)
                            return;

                        if (filterContent.contains(b.block))
                            return;

                        // Optimization: check if overlap camera
                        float range = 0f;
                        if (b instanceof MassDriver.MassDriverBuild mdb) {
                            range = ((MassDriver) mdb.block).range;
                        } else if (b instanceof LogicBlock.LogicBuild lb) {
                            range = ((LogicBlock) lb.block).range;
                        } else if (b instanceof ShockMine.ShockMineBuild) {
                            range = 25f; // Reduced from 10 tiles to ~3 tiles (impact radius)
                        } else if (b.block instanceof PayloadMassDriver) {
                            range = ((PayloadMassDriver) b.block).range;
                        } else if (b instanceof RepairTower.RepairTowerBuild rtb) {
                            range = ((RepairTower) rtb.block).range;
                        } else if (b.block.name.contains("repair")) {
                            // Try to get range via reflection for dynamic/modded blocks, fallback to 110f
                            range = getModdedRange(b.block, 110f);
                        } else if (b.block.name.contains("shockwave")) {
                            range = getModdedRange(b.block, 220f);
                        } else if (b.block.name.equals("build-tower")) {
                            range = getModdedRange(b.block, 180f);
                        }
                        if (range <= 0)
                            return;

                        if (!cameraRect.overlaps(b.x - range, b.y - range, range * 2, range * 2))
                            return;

                        boolean powerMissing = false;
                        if (b.power != null && b.block.consumesPower) {
                            if (b.efficiency > 0) {
                                powerMissing = false;
                            } else {
                                if (b.shouldConsume()) {
                                    powerMissing = true;
                                } else {
                                    powerMissing = b.power.graph.getSatisfaction() <= 0.0001f;
                                }
                            }
                        }
                        boolean isInactive = powerMissing || !b.enabled;

                        if (isInactive) {
                            if (hideInactiveRanges)
                                return;
                            Draw.color(Color.gray, rangeTransparency);
                        } else {
                            if (b instanceof MassDriver.MassDriverBuild)
                                Draw.color(Color.violet, rangeTransparency);
                            else if (b instanceof LogicBlock.LogicBuild)
                                Draw.color(Color.sky, rangeTransparency);
                            else if (b instanceof ShockMine.ShockMineBuild)
                                Draw.color(Color.valueOf("a9d8ff"), rangeTransparency);
                            else if (b.block instanceof PayloadMassDriver)
                                Draw.color(Color.purple, rangeTransparency);
                            else if (b instanceof RepairTower.RepairTowerBuild)
                                Draw.color(Color.valueOf("84f491"), rangeTransparency);
                            else if (b.block.name.contains("repair"))
                                Draw.color(Pal.heal, rangeTransparency);
                            else if (b.block.name.contains("shockwave"))
                                Draw.color(Color.white, rangeTransparency);
                            else if (b.block.name.equals("build-tower"))
                                Draw.color(Color.valueOf("ffd37f"), rangeTransparency);
                        }

                        if (fillRanges) {
                            Draw.alpha(rangeTransparency * 0.2f); // Faint fill
                            Fill.circle(b.x, b.y, range);
                            Draw.alpha(rangeTransparency); // Reset for stroke
                        }

                        Lines.stroke(1.5f);
                        Lines.circle(b.x, b.y, range);
                    });
        }
    }

    // Helper to safely get 'range' field from modded/unknown blocks using
    // reflection
    // Helper to safely get 'range' field from modded/unknown blocks using
    // reflection
    private float getModdedRange(mindustry.world.Block block, float def) {
        // Priority: repairRadius -> shockRadius -> radius -> range -> def
        try {
            java.lang.reflect.Field field = block.getClass().getField("repairRadius");
            return field.getFloat(block);
        } catch (Exception e) {
        }

        try {
            java.lang.reflect.Field field = block.getClass().getField("shockRadius");
            return field.getFloat(block);
        } catch (Exception e) {
        }

        try {
            java.lang.reflect.Field field = block.getClass().getField("radius");
            return field.getFloat(block);
        } catch (Exception e) {
        }

        try {
            java.lang.reflect.Field field = block.getClass().getField("range");
            return field.getFloat(block);
        } catch (Exception e) {
        }

        return def;
    }
}
