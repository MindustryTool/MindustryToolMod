package mindustrytool.features.blockstats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.world.Block;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.production.Pump;
import mindustry.world.blocks.production.Separator;
import mindustry.world.blocks.units.UnitFactory;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockStatsDisplayFeatureTest extends MindustryTestEnv {

    @BeforeEach
    void setUp() {
        Vars.content = new ContentLoader();
    }

    @Test
    void progressAndEfficiencySelectionsAreIndependent() {
        BlockStatsDisplayFeature feature = new BlockStatsDisplayFeature();
        Block factory = register(new UnitFactory("test-factory"));

        assertTrue(feature.isProgressBlockEnabled(factory));
        assertTrue(feature.isEfficiencyBlockEnabled(factory));

        feature.setProgressBlockEnabled(factory, false);

        assertFalse(feature.isProgressBlockEnabled(factory));
        assertTrue(feature.isEfficiencyBlockEnabled(factory));
        assertEquals("mindustrytool.features.blockstats-display.progress.block.test-factory",
                feature.progressBlockValue(factory).getKey());
        assertEquals("mindustrytool.features.blockstats-display.efficiency.block.test-factory",
                feature.efficiencyBlockValue(factory).getKey());
        assertNotEquals(feature.progressBlockValue(factory).getKey(),
                feature.efficiencyBlockValue(factory).getKey());
    }

    @Test
    void bulkProgressSelectionLeavesEfficiencyUntouched() {
        BlockStatsDisplayFeature feature = new BlockStatsDisplayFeature();
        Block factory = register(new UnitFactory("test-factory"));

        feature.rebuildProgressBitSet();
        feature.rebuildEfficiencyBitSet();

        feature.setAllProgressBlocksEnabled(false);
        assertFalse(feature.isProgressBlockEnabled(factory));
        assertTrue(feature.isEfficiencyBlockEnabled(factory));

        feature.resetToDefaults();
        assertTrue(feature.isProgressBlockEnabled(factory));
    }

    @Test
    void extractionAndPowerBlocksAreEfficiencyOnly() {
        Block drill = register(new Drill("test-drill"));
        Block pump = register(new Pump("test-pump"));
        Block generator = register(new PowerGenerator("test-generator"));
        Block separator = register(new Separator("test-separator"));

        assertFalse(BlockStatsDisplayFeature.isProgressEligible(drill));
        assertTrue(BlockStatsDisplayFeature.isEfficiencyEligible(drill));
        assertFalse(BlockStatsDisplayFeature.isProgressEligible(pump));
        assertTrue(BlockStatsDisplayFeature.isEfficiencyEligible(pump));
        assertFalse(BlockStatsDisplayFeature.isProgressEligible(generator));
        assertTrue(BlockStatsDisplayFeature.isEfficiencyEligible(generator));
        assertTrue(BlockStatsDisplayFeature.isProgressEligible(separator));

        assertFalse(BlockStatsDisplayFeature.progressBlocks().contains(drill));
        assertFalse(BlockStatsDisplayFeature.progressBlocks().contains(generator));
        assertTrue(BlockStatsDisplayFeature.progressBlocks().contains(separator));
        assertTrue(BlockStatsDisplayFeature.efficiencyBlocks().contains(drill));
    }

    private static Block register(Block block) {
        block.id = (short) Vars.content.blocks().size;
        Vars.content.handleContent(block);
        return block;
    }
}
