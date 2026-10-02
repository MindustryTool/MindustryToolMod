package mindustrytool.features.smartupgrade;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.I18NBundle;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.gen.Icon;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.distribution.BufferedItemBridge;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Duct;
import mindustry.world.blocks.distribution.DuctBridge;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.distribution.StackConveyor;
import mindustry.world.blocks.liquid.Conduit;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SmartUpgradeFeatureTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        if (Core.bundle == null) {
            Core.bundle = I18NBundle.createEmptyBundle();
        }
        if (Icon.up == null) {
            Icon.up = new TextureRegionDrawable(new TextureRegion());
        }
        if (Icon.warning == null) {
            Icon.warning = new TextureRegionDrawable(new TextureRegion());
        }
        if (Icon.refresh == null) {
            Icon.refresh = new TextureRegionDrawable(new TextureRegion());
        }
        if (Icon.left == null) {
            Icon.left = new TextureRegionDrawable(new TextureRegion());
        }
        Vars.content = new ContentLoader();
    }


    @Test
    void testConfigMutationsAndReset() {
        SmartUpgradeFeature feature = new SmartUpgradeFeature();

        feature.maxUpdatesConfig.set(1000);
        feature.triggerModeConfig.set(SmartUpgradeFeature.MODE_PERSISTENT);
        feature.onlySameTypeConfig.set(true);
        feature.traverseBridgesConfig.set(false);

        assertEquals(1000, feature.maxUpdatesConfig.get());
        assertEquals(SmartUpgradeFeature.MODE_PERSISTENT, feature.triggerModeConfig.get());
        assertFalse(feature.isOneShot());
        assertTrue(feature.onlySameTypeConfig.get());
        assertFalse(feature.traverseBridgesConfig.get());

        feature.resetToDefaults();

        assertEquals(500, feature.maxUpdatesConfig.get());
        assertEquals(SmartUpgradeFeature.MODE_ONE_SHOT, feature.triggerModeConfig.get());
        assertTrue(feature.isOneShot());
        assertFalse(feature.onlySameTypeConfig.get());
        assertTrue(feature.traverseBridgesConfig.get());
    }

    @Test
    void testQuickAccessArmsAndAutoEnables() {
        SmartUpgradeFeature feature = new SmartUpgradeFeature();

        assertFalse(feature.isEnabled());
        assertFalse(feature.isArmed());

        feature.onQuickAccessClick();

        assertTrue(feature.isEnabled());
        assertTrue(feature.isArmed());
        assertEquals(Boolean.TRUE, feature.quickAccessHighlight().peek());

        feature.onQuickAccessClick();

        assertTrue(feature.isEnabled());
        assertFalse(feature.isArmed());
        assertEquals(Boolean.FALSE, feature.quickAccessHighlight().peek());

        feature.disarm();
        assertFalse(feature.isArmed());
    }

    @Test
    void testBlockGroupClassification() {
        assertEquals(SmartUpgradeFeature.BlockGroup.CONVEYOR,
                SmartUpgradeFeature.getGroup(new Conveyor("test-bg-conveyor")));
        assertEquals(SmartUpgradeFeature.BlockGroup.CONVEYOR,
                SmartUpgradeFeature.getGroup(new StackConveyor("test-bg-stack-conveyor")));
        assertEquals(SmartUpgradeFeature.BlockGroup.CONVEYOR,
                SmartUpgradeFeature.getGroup(new Duct("test-bg-duct")));

        assertEquals(SmartUpgradeFeature.BlockGroup.CONDUIT,
                SmartUpgradeFeature.getGroup(new Conduit("test-bg-conduit")));

        assertEquals(SmartUpgradeFeature.BlockGroup.ITEM_BRIDGE,
                SmartUpgradeFeature.getGroup(new ItemBridge("test-bg-item-bridge")));
        assertEquals(SmartUpgradeFeature.BlockGroup.ITEM_BRIDGE,
                SmartUpgradeFeature.getGroup(new BufferedItemBridge("test-bg-buffered-bridge")));
        assertEquals(SmartUpgradeFeature.BlockGroup.ITEM_BRIDGE,
                SmartUpgradeFeature.getGroup(new DuctBridge("test-bg-duct-bridge")));

        assertEquals(SmartUpgradeFeature.BlockGroup.LIQUID_BRIDGE,
                SmartUpgradeFeature.getGroup(new LiquidBridge("test-bg-liquid-bridge")));

        assertEquals(SmartUpgradeFeature.BlockGroup.WALL,
                SmartUpgradeFeature.getGroup(new Wall("test-bg-wall")));

        assertEquals(SmartUpgradeFeature.BlockGroup.DRILL,
                SmartUpgradeFeature.getGroup(new Drill("test-bg-drill")));
        assertEquals(SmartUpgradeFeature.BlockGroup.DRILL,
                SmartUpgradeFeature.getGroup(new BeamDrill("test-bg-beam-drill")));

        assertEquals(SmartUpgradeFeature.BlockGroup.NONE,
                SmartUpgradeFeature.getGroup(new Router("test-bg-router")));
        assertEquals(SmartUpgradeFeature.BlockGroup.NONE,
                SmartUpgradeFeature.getGroup(null));
    }

    @Test
    void testUpgradeCandidates() {
        SmartUpgradeFeature feature = new SmartUpgradeFeature();

        Conveyor conv1 = new Conveyor("cand-conv-1");
        conv1.size = 1;
        Conveyor conv2 = new Conveyor("cand-conv-2");
        conv2.size = 1;
        Conveyor convLarge = new Conveyor("cand-conv-large");
        convLarge.size = 2;
        Wall wall = new Wall("cand-wall-1");
        wall.size = 1;

        Seq<Block> candidates = feature.getUpgradeCandidates(conv1);
        assertEquals(1, candidates.size);
        assertEquals(conv2, candidates.first());

        // For wall: no other wall of size 1 exists in content
        Seq<Block> wallCandidates = feature.getUpgradeCandidates(wall);
        assertTrue(wallCandidates.isEmpty());

        // For null block
        assertTrue(feature.getUpgradeCandidates(null).isEmpty());
    }

    @Test
    void testBridgeUpgradeCandidates() {
        SmartUpgradeFeature feature = new SmartUpgradeFeature();

        ItemBridge bridge1 = new ItemBridge("cand-item-bridge-1");
        bridge1.size = 1;
        BufferedItemBridge bridge2 = new BufferedItemBridge("cand-item-bridge-2");
        bridge2.size = 1;

        Seq<Block> bridgeCandidates = feature.getUpgradeCandidates(bridge1);
        assertEquals(1, bridgeCandidates.size);
        assertEquals(bridge2, bridgeCandidates.first());
    }
}
