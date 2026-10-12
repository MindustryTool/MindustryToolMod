package mindustrytool.features.blockstats;

import java.util.BitSet;
import java.util.Locale;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Prov;
import arc.input.KeyCode;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.struct.ObjectMap;
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
import mindustry.world.blocks.payloads.BlockProducer;
import mindustry.world.blocks.payloads.BlockProducer.BlockProducerBuild;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadDeconstructor;
import mindustry.world.blocks.payloads.PayloadDeconstructor.PayloadDeconstructorBuild;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.power.PowerGenerator.GeneratorBuild;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.BeamDrill.BeamDrillBuild;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.Drill.DrillBuild;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.production.GenericCrafter.GenericCrafterBuild;
import mindustry.world.blocks.production.Pump;
import mindustry.world.blocks.production.Pump.PumpBuild;
import mindustry.world.blocks.production.Separator;
import mindustry.world.blocks.production.Separator.SeparatorBuild;
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
import mindustrytool.features.settings.ModSettings;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

public class BlockStatsDisplayFeature extends Feature {

    private static final String[] PERCENT_STRINGS = new String[301];
    private static final String[] TIME_STRINGS = new String[6001];

    static {
        for (int i = 0; i <= 300; i++) {
            PERCENT_STRINGS[i] = i + "%";
        }
    }

    public final ConfigGroup config;
    public final ConfigGroup progressBlockConfig;
    public final ConfigGroup efficiencyBlockConfig;
    public final ConfigValue<Float> zoomThresholdConfig;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> scaleConfig;

    private static final Boolf<Building> isStatsCandidate = build -> build instanceof UnitFactoryBuild
            || build instanceof ReconstructorBuild
            || build instanceof UnitAssemblerBuild
            || build instanceof GenericCrafterBuild
            || build instanceof SeparatorBuild
            || build instanceof BlockProducerBuild
            || build instanceof PayloadDeconstructorBuild
            || build instanceof DrillBuild
            || build instanceof BeamDrillBuild
            || build instanceof PumpBuild
            || build instanceof GeneratorBuild;

    private final Cons<Building> buildingDrawer = this::drawBuilding;
    private final ObjectMap<Block, ConfigValue<Boolean>> progressBlockValues = new ObjectMap<>();
    private final ObjectMap<Block, ConfigValue<Boolean>> efficiencyBlockValues = new ObjectMap<>();

    private @Nullable BitSet progressBlockEnabled;
    private @Nullable BitSet efficiencyBlockEnabled;
    private @Nullable BlockStatsDisplaySettingsDialog settingsDialog;
    private float frameOpacity;
    private float frameScale;

    public BlockStatsDisplayFeature() {
        super(FeatureMetadata.builder()
                .id("blockstats-display")
                .icon(FileIcon.of("hourglass.png"))
                .order(10)
                .quickAccess(false)
                .enabledByDefault(true)
                .displayGroup(true)
                .build());

        config = configGroup();
        progressBlockConfig = config.group("progress.block");
        efficiencyBlockConfig = config.group("efficiency.block");
        zoomThresholdConfig = config.floatValue("zoom-threshold", 0.5f);
        opacityConfig = config.floatValue("opacity", 1f);
        scaleConfig = config.floatValue("scale", 1f);

        bindToggle("blockstatstoggle", KeyCode.unset);
        bindDialog("blockstatssettings", KeyCode.unset, getSettingDialog(), false);

        Events.run(Trigger.draw, this::draw);
    }

    public void resetToDefaults() {
        zoomThresholdConfig.reset();
        opacityConfig.reset();
        scaleConfig.reset();
        for (Block block : progressBlocks()) {
            progressBlockValue(block).reset();
        }
        for (Block block : efficiencyBlocks()) {
            efficiencyBlockValue(block).reset();
        }
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new BlockStatsDisplaySettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    public static boolean isProgressEligible(Block block) {
        return block instanceof UnitFactory
                || block instanceof Reconstructor
                || block instanceof UnitAssembler
                || block instanceof GenericCrafter
                || block instanceof Separator
                || block instanceof BlockProducer
                || block instanceof PayloadDeconstructor;
    }

    public static boolean isEfficiencyEligible(Block block) {
        return isProgressEligible(block)
                || block instanceof Drill
                || block instanceof BeamDrill
                || block instanceof Pump
                || block instanceof PowerGenerator;
    }

    public static Seq<Block> progressBlocks() {
        return eligibleBlocks(true);
    }

    public static Seq<Block> efficiencyBlocks() {
        return eligibleBlocks(false);
    }

    private static Seq<Block> eligibleBlocks(boolean progress) {
        Seq<Block> blocks = Vars.content != null ? Vars.content.blocks() : null;
        if (blocks == null) {
            return new Seq<>();
        }

        Seq<Block> result = new Seq<>();
        for (int i = 0; i < blocks.size; i++) {
            Block block = blocks.get(i);
            if (progress ? isProgressEligible(block) : isEfficiencyEligible(block)) {
                result.add(block);
            }
        }
        return result;
    }

    public ConfigValue<Boolean> progressBlockValue(Block block) {
        ConfigValue<Boolean> value = progressBlockValues.get(block);
        if (value == null) {
            value = progressBlockConfig.boolValue(block.name, true);
            value.signal().subscribe(enabled -> applyProgressBit(block, Boolean.TRUE.equals(enabled)));
            progressBlockValues.put(block, value);
        }
        return value;
    }

    public ConfigValue<Boolean> efficiencyBlockValue(Block block) {
        ConfigValue<Boolean> value = efficiencyBlockValues.get(block);
        if (value == null) {
            value = efficiencyBlockConfig.boolValue(block.name, true);
            value.signal().subscribe(enabled -> applyEfficiencyBit(block, Boolean.TRUE.equals(enabled)));
            efficiencyBlockValues.put(block, value);
        }
        return value;
    }

    public boolean isProgressBlockEnabled(Block block) {
        return Boolean.TRUE.equals(progressBlockValue(block).get());
    }

    public boolean isEfficiencyBlockEnabled(Block block) {
        return Boolean.TRUE.equals(efficiencyBlockValue(block).get());
    }

    public void setProgressBlockEnabled(Block block, boolean enabled) {
        progressBlockValue(block).set(enabled);
    }

    public void setEfficiencyBlockEnabled(Block block, boolean enabled) {
        efficiencyBlockValue(block).set(enabled);
    }

    public void setAllProgressBlocksEnabled(boolean enabled) {
        for (Block block : progressBlocks()) {
            setProgressBlockEnabled(block, enabled);
        }
    }

    public void setAllEfficiencyBlocksEnabled(boolean enabled) {
        for (Block block : efficiencyBlocks()) {
            setEfficiencyBlockEnabled(block, enabled);
        }
    }

    public void rebuildProgressBitSet() {
        progressBlockEnabled = buildBitSet(true);
    }

    public void rebuildEfficiencyBitSet() {
        efficiencyBlockEnabled = buildBitSet(false);
    }

    private void applyProgressBit(Block block, boolean enabled) {
        if (progressBlockEnabled != null) {
            progressBlockEnabled.set(block.id, enabled);
        }
    }

    private void applyEfficiencyBit(Block block, boolean enabled) {
        if (efficiencyBlockEnabled != null) {
            efficiencyBlockEnabled.set(block.id, enabled);
        }
    }

    private @Nullable BitSet buildBitSet(boolean progress) {
        Seq<Block> blocks = Vars.content != null ? Vars.content.blocks() : null;
        if (blocks == null) {
            return null;
        }

        BitSet bits = new BitSet(blocks.size);
        for (int i = 0; i < blocks.size; i++) {
            Block block = blocks.get(i);
            boolean eligible = progress ? isProgressEligible(block) : isEfficiencyEligible(block);
            if (eligible) {
                bits.set(block.id, progress ? isProgressBlockEnabled(block) : isEfficiencyBlockEnabled(block));
            }
        }
        return bits;
    }

    private void draw() {
        if (!isEnabled() || !Vars.state.isGame() || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
            return;
        }

        Float zoomThreshold = zoomThresholdConfig.signal().peek();
        Float opacity = opacityConfig.signal().peek();
        Float scale = scaleConfig.signal().peek();

        float threshold = zoomThreshold != null ? zoomThreshold : 0.5f;
        frameOpacity = opacity != null ? opacity : 1f;
        frameScale = ModSettings.effectiveScale(scale != null ? scale : 1f);

        if (frameOpacity <= 0.01f) {
            return;
        }

        if (threshold > 0.01f && Vars.renderer.getScale() < threshold) {
            return;
        }

        if (progressBlockEnabled == null || efficiencyBlockEnabled == null) {
            rebuildProgressBitSet();
            rebuildEfficiencyBitSet();
        }

        float z = Draw.z();
        Draw.z(Layer.overlayUI);

        float cx = Core.camera.position.x;
        float cy = Core.camera.position.y;
        float radius = Math.max(Core.camera.width, Core.camera.height) * 0.75f;

        Vars.indexer.eachBlock(null, cx, cy, radius, isStatsCandidate, buildingDrawer);

        Draw.z(z);
        Draw.reset();
    }

    private void drawBuilding(Building build) {
        if (build == null || !build.isValid()) {
            return;
        }

        BitSet progressSet = progressBlockEnabled;
        BitSet efficiencySet = efficiencyBlockEnabled;
        if (progressSet == null || efficiencySet == null) {
            return;
        }

        int id = build.block.id;
        float remaining = progressSet.get(id) ? remainingSeconds(build) : -1f;
        float efficiency = efficiencySet.get(id) ? efficiencyPercent(build) : -1f;

        if (remaining >= 0f && efficiency >= 0f) {
            float offset = 3.5f * frameScale;
            drawRemaining(build.x, build.y + offset, remaining);
            drawEfficiency(build.x, build.y - offset, efficiency);
        } else if (remaining >= 0f) {
            drawRemaining(build.x, build.y, remaining);
        } else if (efficiency >= 0f) {
            drawEfficiency(build.x, build.y, efficiency);
        }
    }

    private static float remainingSeconds(Building build) {
        float cycleTicks;
        float fraction;

        if (build instanceof UnitFactoryBuild) {
            UnitFactoryBuild b = (UnitFactoryBuild) build;
            UnitFactory block = (UnitFactory) b.block;
            if (b.currentPlan < 0 || b.currentPlan >= block.plans.size) {
                return -1f;
            }
            UnitPlan plan = block.plans.get(b.currentPlan);
            cycleTicks = plan.time;
            fraction = cycleTicks > 0f ? b.progress / cycleTicks : 0f;
        } else if (build instanceof ReconstructorBuild) {
            ReconstructorBuild b = (ReconstructorBuild) build;
            cycleTicks = ((Reconstructor) b.block).constructTime;
            fraction = cycleTicks > 0f ? b.progress / cycleTicks : 0f;
        } else if (build instanceof UnitAssemblerBuild) {
            UnitAssemblerBuild b = (UnitAssemblerBuild) build;
            AssemblerUnitPlan plan = b.plan();
            if (plan == null) {
                return -1f;
            }
            cycleTicks = plan.time;
            fraction = b.progress;
        } else if (build instanceof GenericCrafterBuild) {
            GenericCrafterBuild b = (GenericCrafterBuild) build;
            cycleTicks = ((GenericCrafter) b.block).craftTime;
            fraction = b.progress;
        } else if (build instanceof SeparatorBuild) {
            SeparatorBuild b = (SeparatorBuild) build;
            cycleTicks = ((Separator) b.block).craftTime;
            fraction = b.progress;
        } else if (build instanceof BlockProducerBuild) {
            BlockProducerBuild b = (BlockProducerBuild) build;
            Block recipe = b.recipe();
            BlockProducer block = (BlockProducer) b.block;
            if (recipe == null || recipe.buildTime <= 0f || block.buildSpeed <= 0f) {
                return -1f;
            }
            cycleTicks = recipe.buildTime / block.buildSpeed;
            fraction = b.progress / recipe.buildTime;
        } else if (build instanceof PayloadDeconstructorBuild) {
            PayloadDeconstructorBuild b = (PayloadDeconstructorBuild) build;
            Payload payload = b.deconstructing;
            PayloadDeconstructor block = (PayloadDeconstructor) b.block;
            if (payload == null || block.deconstructSpeed <= 0f) {
                return -1f;
            }
            cycleTicks = payload.buildTime() / block.deconstructSpeed;
            fraction = b.progress;
        } else {
            return -1f;
        }

        if (cycleTicks <= 0f || fraction < 0.01f) {
            return -1f;
        }

        float timeScale = build.timeScale();
        if (timeScale <= 0.0001f) {
            timeScale = 1f;
        }

        return Math.max((1f - fraction) * cycleTicks / 60f / timeScale, 0f);
    }

    private static float efficiencyPercent(Building build) {
        float value = build.efficiency;
        if (build instanceof GeneratorBuild) {
            GeneratorBuild generator = (GeneratorBuild) build;
            if (generator.productionEfficiency > 0f) {
                value = generator.productionEfficiency;
            }
        }

        return value > 0f ? value : -1f;
    }

    private void drawRemaining(float x, float y, float remainingSeconds) {
        Fonts.outline.draw(timeText(remainingSeconds), x, y, Tmp.c1.set(Color.white).a(frameOpacity),
                0.25f * frameScale, false, Align.center);
    }

    private void drawEfficiency(float x, float y, float efficiency) {
        int percent = Mathf.clamp(Math.round(efficiency * 100f), 0, 300);
        Color color = percent >= 95 ? Color.green : (percent > 50 ? Color.orange : Color.scarlet);

        Fonts.outline.draw(PERCENT_STRINGS[percent], x, y, Tmp.c2.set(color).a(frameOpacity),
                0.25f * frameScale, false, Align.center);
    }

    private static String timeText(float seconds) {
        int step = Mathf.clamp(Math.round(seconds * 10f), 0, TIME_STRINGS.length - 1);
        if (step == TIME_STRINGS.length - 1) {
            return String.format(Locale.US, "%.1fs", seconds);
        }

        String cached = TIME_STRINGS[step];
        if (cached == null) {
            cached = step / 10 + "." + step % 10 + "s";
            TIME_STRINGS[step] = cached;
        }
        return cached;
    }
}
