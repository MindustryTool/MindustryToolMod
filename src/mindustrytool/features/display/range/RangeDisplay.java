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
    private final RangeDisplayConfig config = new RangeDisplayConfig();

    // Cache for reflection results to avoid overhead per frame
    private final ObjectMap<Block, Float> blockRangeCache = new ObjectMap<>();

    // In Java, using a method reference like this::drawUnit directly in a loop (or
    // a
    // method called every frame) creates a new object instance each time because it
    // captures this .
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
                .name("Range Display")
                .description("Display ranges for blocks, units, and turrets.")
                .icon(Utils.icons("range-display.png"))
                .order(5)
                .enabledByDefault(true)
                .quickAccess(true)
                .build();
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
                        Color color = Vars.state.rules.waveTeam.color;

                        drawCircle(x, y, dropRadius, color);

                    }
                }
            }
        }

        // Draw Units
        if (config.drawUnitRangeAlly || config.drawUnitRangeEnemy || config.drawPlayerRange) {
            float margin = 2000f; // Increase search area to account for unit range
            Groups.unit.intersect(cx - cw / 2f - margin, cy - ch / 2f - margin, cw + margin * 2, ch + margin * 2,
                    unitDrawer);
        }

        // Draw Buildings
        Vars.indexer.eachBlock(null, cx, cy, radius, buildingPredicate, buildingDrawer);

        Draw.reset();
    }

    private void drawUnit(Unit unit) {
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
            Color color = unit.team.color;

            drawCircle(unit.x, unit.y, unit.range(), color);

        }
    }

    private void drawBuilding(Building build) {
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

        Tmp.c2.set(color).a(config.opacity);
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
                // Ignore
            }
        }

        blockRangeCache.put(block, range);
        return range;
    }

    private void initDialog() {
        dialog = new BaseDialog("Range Display Settings");
        dialog.addCloseButton();

        Table cont = dialog.cont;
        cont.defaults().left().pad(5);

        Slider opacitySlider = new Slider(0f, 1f, 0.05f, false);
        opacitySlider.setValue(config.opacity);

        Label opacityValue = new Label(
                String.format("%.0f%%", config.opacity * 100),
                Styles.outlineLabel);
        opacityValue.setColor(Color.lightGray);

        Table opacityContent = new Table();
        opacityContent.touchable = arc.scene.event.Touchable.disabled;
        opacityContent.margin(3f, 33f, 3f, 33f);
        opacityContent.add("Opacity", Styles.outlineLabel).left().growX();
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            config.opacity = opacitySlider.getValue();
            opacityValue.setText(String.format("%.0f%%", config.opacity * 100));
            config.save();
        });

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);
        cont.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

        addCheck(cont, "Draw Ally Block Range", config.drawBlockRangeAlly, v -> {
            config.drawBlockRangeAlly = v;
            config.save();
        });

        if (Vars.mobile) {
            cont.row();
        }

        addCheck(cont, "Draw Enemy Block Range", config.drawBlockRangeEnemy, v -> {
            config.drawBlockRangeEnemy = v;
            config.save();
        });

        cont.row();

        addCheck(cont, "Draw Ally Turret Range", config.drawTurretRangeAlly, v -> {
            config.drawTurretRangeAlly = v;
            config.save();
        });

        if (Vars.mobile) {
            cont.row();
        }

        addCheck(cont, "Draw Enemy Turret Range", config.drawTurretRangeEnemy, v -> {
            config.drawTurretRangeEnemy = v;
            config.save();
        });

        cont.row();

        addCheck(cont, "Draw Ally Unit Range", config.drawUnitRangeAlly, v -> {
            config.drawUnitRangeAlly = v;
            config.save();
        });

        if (Vars.mobile) {
            cont.row();
        }

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

        if (Vars.mobile) {
            cont.row();
        }
    }

    private void addCheck(Table table, String text, boolean def, arc.func.Boolc listener) {
        table.check(text, def, listener).left();
    }
}
