package mindustrytool.features.display.range;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;

public class RangeDisplay implements Feature {
    private boolean enabled = false;
    private BaseDialog dialog;
    private final RangeDisplayConfig config = new RangeDisplayConfig();

    private final Set<String> blockRangeFields = Set.of("range");

    @Override
    public FeatureMetadata getMetadata() {
        return new FeatureMetadata("Range Display", "Display ranges for blocks, units, and turrets.", "eye", 5);
    }

    @Override
    public void init() {
        config.load();
        Events.run(Trigger.draw, this::draw);
    }

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    @Override
    public Optional<Dialog> setting() {
        if (dialog == null) {
            initDialog();
        }
        return Optional.of(dialog);
    }

    private void draw() {
        if (!enabled || !Vars.state.isGame())
            return;

        Draw.z(Layer.overlayUI);

        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float cw = Core.camera.width;
        float ch = Core.camera.height;
        float maxDimension = Math.max(cw, ch);
        float radius = maxDimension * 0.75f; // Slightly larger to cover corners

        // Draw Spawners
        if (config.drawSpawnerRange) {
            if (Vars.spawner.getSpawns() != null) {
                float dropRadius = Vars.state.rules.dropZoneRadius;
                for (Tile tile : Vars.spawner.getSpawns()) {
                    if (tile == null)
                        continue;
                    float x = tile.worldx();
                    float y = tile.worldy();
                    // Check visibility
                    if (Mathf.dst(cx, cy, x, y) - dropRadius < radius) {
                        Draw.color(Vars.state.rules.waveTeam.color);
                        Drawf.dashCircle(x, y, dropRadius, Vars.state.rules.waveTeam.color);
                        Draw.color(Vars.state.rules.waveTeam.color);
                        Lines.line(x, y, x + Mathf.cosDeg(15) * dropRadius, y + Mathf.sinDeg(15) * dropRadius);
                    }
                }
            }
        }

        // Draw Units
        if (config.drawUnitRangeAlly || config.drawUnitRangeEnemy || config.drawPlayerRange) {
            float margin = 2000f; // Increase search area to account for unit range
            Groups.unit.intersect(cx - cw / 2f - margin, cy - ch / 2f - margin, cw + margin * 2, ch + margin * 2,
                    unit -> {
                        if (!unit.isValid())
                            return;

                        boolean isPlayer = unit == Vars.player.unit();
                        boolean isAlly = unit.team == Vars.player.team();

                        if (isPlayer) {
                            if (!config.drawPlayerRange)
                                return;
                        } else {
                            if (isAlly && !config.drawUnitRangeAlly)
                                return;
                            if (!isAlly && !config.drawUnitRangeEnemy)
                                return;
                        }

                        if (unit.range() > 0) {
                            Draw.color(unit.team.color);
                            Drawf.dashCircle(unit.x, unit.y, unit.range(), unit.team.color);
                            Draw.color(unit.team.color);
                            Lines.line(unit.x, unit.y, unit.x + Mathf.cosDeg(15) * unit.range(),
                                    unit.y + Mathf.sinDeg(15) * unit.range());
                        }
                    });
        }

        // Draw Buildings
        Vars.indexer.eachBlock(null, cx, cy, radius, b -> true, build -> {
            if (!build.isValid())
                return;

            boolean isAlly = build.team == Vars.player.team();
            boolean isTurret = build.block instanceof Turret;

            if (isTurret) {
                if (isAlly && !config.drawTurretRangeAlly)
                    return;
                if (!isAlly && !config.drawTurretRangeEnemy)
                    return;
            } else {
                if (isAlly && !config.drawBlockRangeAlly)
                    return;
                if (!isAlly && !config.drawBlockRangeEnemy)
                    return;
            }

            float range = 0;
            if (isTurret) {
                range = ((Turret) build.block).range;
            } else {
                try {
                    for (String fieldName : blockRangeFields) {
                        Field rangeField = build.block.getClass().getDeclaredField(fieldName);
                        rangeField.setAccessible(true);
                        float blockRange = rangeField.getFloat(build.block);

                        if (blockRange > 0) {
                            range = blockRange;
                        }
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                }
            }

            if (range > 0) {
                Draw.color(build.team.color);
                Drawf.dashCircle(build.x, build.y, range, build.team.color);
                Draw.color(build.team.color);
                Lines.line(build.x, build.y, build.x + Mathf.cosDeg(15) * range, build.y + Mathf.sinDeg(15) * range);
            }
        });

        Draw.reset();
    }

    private void initDialog() {
        dialog = new BaseDialog("Range Display Settings");
        dialog.addCloseButton();

        Table cont = dialog.cont;
        cont.defaults().left().pad(5);

        addCheck(cont, "Draw Ally Block Range", config.drawBlockRangeAlly, v -> {
            config.drawBlockRangeAlly = v;
            config.save();
        });
        addCheck(cont, "Draw Enemy Block Range", config.drawBlockRangeEnemy, v -> {
            config.drawBlockRangeEnemy = v;
            config.save();
        });
        cont.row();

        addCheck(cont, "Draw Ally Turret Range", config.drawTurretRangeAlly, v -> {
            config.drawTurretRangeAlly = v;
            config.save();
        });
        addCheck(cont, "Draw Enemy Turret Range", config.drawTurretRangeEnemy, v -> {
            config.drawTurretRangeEnemy = v;
            config.save();
        });
        cont.row();

        addCheck(cont, "Draw Ally Unit Range", config.drawUnitRangeAlly, v -> {
            config.drawUnitRangeAlly = v;
            config.save();
        });
        addCheck(cont, "Draw Enemy Unit Range", config.drawUnitRangeEnemy, v -> {
            config.drawUnitRangeEnemy = v;
            config.save();
        });
        cont.row();

        addCheck(cont, "Draw Player Range", config.drawPlayerRange, v -> {
            config.drawPlayerRange = v;
            config.save();
        });
        cont.row();

        addCheck(cont, "Draw Spawner Range", config.drawSpawnerRange, v -> {
            config.drawSpawnerRange = v;
            config.save();
        });
    }

    private void addCheck(Table table, String text, boolean def, arc.func.Boolc listener) {
        table.check(text, def, listener).left();
    }
}
