package mindustrytool.features.display.range;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RangeDisplay implements Feature {
    private boolean enabled = false;
    private BaseDialog dialog;

    private final ObjectMap<Block, Float> blockRangeCache = new ObjectMap<>();

    private final Cons<Unit> unitDrawer = this::drawUnit;
    private final Cons<Building> buildingDrawer = this::drawBuilding;
    private final Boolf<Building> buildingPredicate = b -> true;

    private static final Set<String> blockRangeFields = new HashSet<String>();

    private float targetX = 0, targetY = 0;

    static {
        blockRangeFields.add("range");
    }

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.range-display.name")
                .description("@feature.range-display.description")
                .icon(Utils.icons("range-display.png"))
                .order(5)
                .enabledByDefault(true)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        RangeDisplayConfig.load();
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
            dialog = new BaseDialog("@range-display.settings.title");
            dialog.name = "rangeDisplaySettingDialog";
            dialog.addCloseButton();

            Table cont = dialog.cont;
            cont.defaults().left().pad(5);

            Slider opacitySlider = new Slider(0f, 1f, 0.05f, false);
            opacitySlider.setValue(RangeDisplayConfig.opacity);

            Label opacityValue = new Label(
                    String.format("%.0f%%", RangeDisplayConfig.opacity * 100),
                    Styles.outlineLabel);
            opacityValue.setColor(Color.lightGray);

            Table opacityContent = new Table();
            opacityContent.touchable = arc.scene.event.Touchable.disabled;
            opacityContent.margin(3f, 33f, 3f, 33f);
            opacityContent.add("@opacity", Styles.outlineLabel).left().growX();
            opacityContent.add(opacityValue).padLeft(10f).right();

            opacitySlider.changed(() -> {
                RangeDisplayConfig.opacity = opacitySlider.getValue();
                opacityValue.setText(String.format("%.0f%%", RangeDisplayConfig.opacity * 100));
                RangeDisplayConfig.save();
            });

            float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);
            cont.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

            addCheck(cont, "@range-display.draw-ally-block-range", RangeDisplayConfig.drawBlockRangeAlly, v -> {
                RangeDisplayConfig.drawBlockRangeAlly = v;
                RangeDisplayConfig.save();
            });

            if (Vars.mobile) {
                cont.row();
            }

            addCheck(cont, "@range-display.draw-enemy-block-range", RangeDisplayConfig.drawBlockRangeEnemy, v -> {
                RangeDisplayConfig.drawBlockRangeEnemy = v;
                RangeDisplayConfig.save();
            });

            cont.row();

            addCheck(cont, "@range-display.draw-ally-turret-range", RangeDisplayConfig.drawTurretRangeAlly, v -> {
                RangeDisplayConfig.drawTurretRangeAlly = v;
                RangeDisplayConfig.save();
            });

            if (Vars.mobile) {
                cont.row();
            }

            addCheck(cont, "@range-display.draw-enemy-turret-range", RangeDisplayConfig.drawTurretRangeEnemy, v -> {
                RangeDisplayConfig.drawTurretRangeEnemy = v;
                RangeDisplayConfig.save();
            });

            cont.row();

            addCheck(cont, "@range-display.draw-ally-unit-range", RangeDisplayConfig.drawUnitRangeAlly, v -> {
                RangeDisplayConfig.drawUnitRangeAlly = v;
                RangeDisplayConfig.save();
            });

            if (Vars.mobile) {
                cont.row();
            }

            addCheck(cont, "@range-display.draw-enemy-unit-range", RangeDisplayConfig.drawUnitRangeEnemy, v -> {
                RangeDisplayConfig.drawUnitRangeEnemy = v;
                RangeDisplayConfig.save();
            });

            cont.row();

            addCheck(cont, "@range-display.draw-player-range", RangeDisplayConfig.drawPlayerRange, v -> {
                RangeDisplayConfig.drawPlayerRange = v;
                RangeDisplayConfig.save();
            });

            cont.row();

            addCheck(cont, "@range-display.draw-spawner-range", RangeDisplayConfig.drawSpawnerRange, v -> {
                RangeDisplayConfig.drawSpawnerRange = v;
                RangeDisplayConfig.save();
            });

            if (Vars.mobile) {
                cont.row();
            }
        }
        return Optional.of(dialog);
    }

    private void draw() {
        if (!enabled || !Vars.state.isGame()) {
            return;
        }

        if (Vars.player.unit() != null) {
            targetX = Vars.player.unit().x;
            targetY = Vars.player.unit().y;
        }

        Draw.z(Layer.overlayUI);

        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float cw = Core.camera.width;
        float ch = Core.camera.height;
        float maxDimension = Math.max(cw, ch);
        float radius = maxDimension * 0.75f;

        if (RangeDisplayConfig.drawSpawnerRange) {
            if (Vars.spawner.getSpawns() != null) {
                float dropRadius = Vars.state.rules.dropZoneRadius;
                for (Tile tile : Vars.spawner.getSpawns()) {
                    if (tile == null)
                        continue;
                    float x = tile.worldx();
                    float y = tile.worldy();

                    if (Mathf.dst(cx, cy, x, y) - dropRadius < radius) {
                        Color color = Vars.state.rules.waveTeam.color;

                        drawCircle(x, y, dropRadius, color);

                    }
                }
            }
        }

        if (RangeDisplayConfig.drawUnitRangeAlly || RangeDisplayConfig.drawUnitRangeEnemy || RangeDisplayConfig.drawPlayerRange) {
            float margin = 2000f;
            Groups.unit.intersect(cx - cw / 2f - margin, cy - ch / 2f - margin, cw + margin * 2, ch + margin * 2,
                    unitDrawer);
        }

        Vars.indexer.eachBlock(null, cx, cy, radius, buildingPredicate, buildingDrawer);

        Draw.reset();
    }

    private void drawUnit(Unit unit) {
        if (!unit.isValid())
            return;

        boolean isPlayer = unit == Vars.player.unit();
        boolean isAlly = unit.team == Vars.player.team();

        if (isPlayer) {
            if (!RangeDisplayConfig.drawPlayerRange)
                return;
        } else {
            if (isAlly && !RangeDisplayConfig.drawUnitRangeAlly)
                return;
            if (!isAlly && !RangeDisplayConfig.drawUnitRangeEnemy)
                return;
        }

        if (unit.range() > 0) {
            Color color = unit.team.color;

            drawCircle(unit.x, unit.y, unit.range(), color);

        }
    }

    private void drawBuilding(Building build) {
        if (!build.isValid()) {
            return;
        }

        boolean isAlly = build.team == Vars.player.team();
        boolean isTurret = build.block instanceof Turret;

        if (isTurret) {
            if (isAlly && !RangeDisplayConfig.drawTurretRangeAlly)
                return;
            if (!isAlly && !RangeDisplayConfig.drawTurretRangeEnemy)
                return;
        } else {
            if (isAlly && !RangeDisplayConfig.drawBlockRangeAlly)
                return;
            if (!isAlly && !RangeDisplayConfig.drawBlockRangeEnemy)
                return;
        }

        float range = 0;
        if (isTurret) {
            range = ((Turret) build.block).range;
        } else if (build.block instanceof OverdriveProjector projector) {
            range = projector.range;
        } else {
            range = getBlockRange(build.block);
        }

        if (range > 0) {
            Color color = build.team.color;
            drawCircle(build.x, build.y, range, color);
        }
    }

    private void drawCircle(float x, float y, float range, Color color) {

        var rotation = Mathf.angle(targetX - x, targetY - y);

        Tmp.c2.set(color).a(RangeDisplayConfig.opacity);
        Lines.stroke(1f, Tmp.c2);
        Lines.dashCircle(x, y, range);
        Draw.color(Tmp.c2);
        Lines.line(x, y, x + Mathf.cosDeg(rotation) * range, y + Mathf.sinDeg(rotation) * range);
        Draw.reset();
    }

    private float getBlockRange(Block block) {
        if (blockRangeCache.containsKey(block)) {
            return blockRangeCache.get(block);
        }

        float range = 0;
        for (String fieldName : blockRangeFields) {
            try {
                Field rangeField = block.getClass().getDeclaredField(fieldName);
                rangeField.setAccessible(true);
                float blockRange = rangeField.getFloat(block);

                if (blockRange > 0) {
                    range = blockRange;
                    break;
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {

            }
        }

        blockRangeCache.put(block, range);
        return range;
    }

    private void addCheck(Table table, String text, boolean def, arc.func.Boolc listener) {
        table.check(text, def, listener).left();
    }
}
