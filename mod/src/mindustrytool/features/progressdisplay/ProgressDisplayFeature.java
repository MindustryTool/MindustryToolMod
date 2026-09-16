package mindustrytool.features.progressdisplay;

import java.util.BitSet;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.ui.Fonts;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.GenericCrafter.GenericCrafterBuild;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.Reconstructor.ReconstructorBuild;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitAssembler.AssemblerUnitPlan;
import mindustry.world.blocks.units.UnitAssembler.UnitAssemblerBuild;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild;
import mindustry.world.blocks.units.UnitFactory.UnitPlan;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

/**
 * Overlays a countdown of the remaining production time above active unit
 * factories, reconstructors, unit assemblers, and crafters. Per-block opt-outs
 * are stored in {@link Core#settings} and mirrored into a {@link BitSet} for
 * allocation-free lookups in the draw loop. Scalar configuration is peeked once
 * per frame into scratch fields.
 */
public class ProgressDisplayFeature extends Feature {

    public static final String BLOCK_SETTING_PREFIX = "mindustrytool.features.progress-display.block.";

    public final ConfigGroup config;
    public final ConfigValue<Float> zoomThresholdConfig;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> scaleConfig;

    private static final Boolf<Building> IS_PRODUCER = b -> b instanceof UnitFactoryBuild
            || b instanceof ReconstructorBuild
            || b instanceof UnitAssemblerBuild
            || b instanceof GenericCrafterBuild;

    private final Cons<Building> buildingDrawer = this::drawBuilding;

    private @Nullable BitSet blockEnabled;
    private @Nullable ProgressDisplaySettingsDialog settingsDialog;

    // Per-frame scratch written once in draw(), read by the per-building loop.
    private float frameOpacity;
    private float frameScale;

    public ProgressDisplayFeature() {
        super(FeatureMetadata.builder()
                .id("progress-display")
                .icon(FileIcon.of("hourglass.png"))
                .order(10)
                .quickAccess(true)
                .enabledByDefault(true)
                .build());

        config = configGroup();
        zoomThresholdConfig = config.floatValue("zoom-threshold", 0.5f);
        opacityConfig = config.floatValue("opacity", 1f);
        scaleConfig = config.floatValue("scale", 1f);

        Events.run(Trigger.draw, this::draw);
    }

    public void resetToDefaults() {
        zoomThresholdConfig.reset();
        opacityConfig.reset();
        scaleConfig.reset();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new ProgressDisplaySettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    // --- Per-block configuration ---

    public static String blockSettingKey(Block block) {
        return BLOCK_SETTING_PREFIX + block.name;
    }

    public boolean isBlockEnabled(Block block) {
        return blockEnabled == null
                ? Core.settings.getBool(blockSettingKey(block), true)
                : blockEnabled.get(block.id);
    }

    public void setBlockEnabled(Block block, boolean value) {
        Core.settings.put(blockSettingKey(block), value);
        rebuildBitSet();
    }

    private void rebuildBitSet() {
        if (Vars.content == null) {
            return;
        }

        Seq<Block> blocks = Vars.content.blocks();
        BitSet bits = new BitSet(blocks.size);

        for (int i = 0; i < blocks.size; i++) {
            Block block = blocks.get(i);
            if (isSupported(block)) {
                bits.set(block.id, Core.settings.getBool(blockSettingKey(block), true));
            }
        }

        blockEnabled = bits;
    }

    private static boolean isSupported(Block block) {
        return block instanceof UnitFactory
                || block instanceof Reconstructor
                || block instanceof UnitAssembler
                || block instanceof GenericCrafter;
    }

    // --- Draw ---

    private void draw() {
        if (!isEnabled() || !Vars.state.isGame() || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
            return;
        }

        Float zoomThreshold = zoomThresholdConfig.signal().peek();
        Float opacity = opacityConfig.signal().peek();
        Float scale = scaleConfig.signal().peek();

        float threshold = zoomThreshold != null ? zoomThreshold : 0.5f;
        frameOpacity = opacity != null ? opacity : 1f;
        frameScale = scale != null ? scale : 1f;

        if (frameOpacity <= 0.01f) {
            return;
        }

        if (threshold > 0.01f && Vars.renderer.getScale() < threshold) {
            return;
        }

        if (blockEnabled == null) {
            rebuildBitSet();
        }

        float z = Draw.z();
        Draw.z(Layer.overlayUI);

        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float radius = Math.max(Core.camera.width, Core.camera.height) * 0.75f;

        Vars.indexer.eachBlock(null, cx, cy, radius, IS_PRODUCER, buildingDrawer);

        Draw.z(z);
        Draw.reset();
    }

    private void drawBuilding(Building build) {
        if (build == null || !build.isValid() || blockEnabled == null || !blockEnabled.get(build.block.id)) {
            return;
        }

        float fraction;
        float totalTime;

        if (build instanceof UnitFactoryBuild) {
            UnitFactoryBuild b = (UnitFactoryBuild) build;
            UnitFactory block = (UnitFactory) b.block;
            if (b.currentPlan < 0 || b.currentPlan >= block.plans.size) {
                return;
            }
            UnitPlan plan = block.plans.get(b.currentPlan);
            totalTime = plan.time;
            fraction = totalTime > 0f ? b.progress / totalTime : 0f;
        } else if (build instanceof ReconstructorBuild) {
            ReconstructorBuild b = (ReconstructorBuild) build;
            Reconstructor block = (Reconstructor) b.block;
            totalTime = block.constructTime;
            fraction = totalTime > 0f ? b.progress / totalTime : 0f;
        } else if (build instanceof UnitAssemblerBuild) {
            UnitAssemblerBuild b = (UnitAssemblerBuild) build;
            AssemblerUnitPlan plan = b.plan();
            if (plan == null) {
                return;
            }
            totalTime = plan.time;
            fraction = b.progress;
        } else if (build instanceof GenericCrafterBuild) {
            GenericCrafterBuild b = (GenericCrafterBuild) build;
            GenericCrafter block = (GenericCrafter) b.block;
            totalTime = block.craftTime;
            fraction = b.progress;
        } else {
            return;
        }

        if (totalTime <= 0f || fraction < 0.01f) {
            return;
        }

        float timeScale = build.timeScale();
        if (timeScale <= 0.0001f) {
            timeScale = 1f;
        }

        float remainingSeconds = Math.max((1f - fraction) * totalTime / 60f / timeScale, 0f);
        drawText(build.x, build.y, remainingSeconds);
    }

    private void drawText(float x, float y, float remainingSeconds) {
        String text = String.format("%.1fs", remainingSeconds);
        Fonts.outline.draw(text, x, y, Tmp.c1.set(Color.white).a(frameOpacity),
                0.25f * frameScale, false, Align.center);
        Draw.reset();
    }
}
