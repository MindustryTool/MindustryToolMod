package mindustrytool.features.smartupgrade;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.files.Fi;
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
import mindustrytool.features.FeatureMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Properties;

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
    void verifyBundles() throws Exception {
        File dir = new File("../assets/bundles");
        if (!dir.exists()) {
            dir = new File("assets/bundles");
        }
        assertTrue(dir.exists(), "assets/bundles directory must exist");

        File enBundle = new File(dir, "bundle.properties");
        File viBundle = new File(dir, "bundle_vi.properties");

        assertTrue(enBundle.exists());
        assertTrue(viBundle.exists());

        // Strict validation on all bundle files
        File[] files = dir.listFiles((d, name) -> name.endsWith(".properties"));
        assertNotNull(files);

        for (File f : files) {
            byte[] bytes = Files.readAllBytes(f.toPath());
            if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                fail("File " + f.getName() + " contains UTF-8 BOM! Bundles must be UTF-8 without BOM.");
            }

            Properties props = new Properties();
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                props.load(isr);
            }
            assertFalse(props.isEmpty(), "Bundle " + f.getName() + " should not be empty");

            for (String key : props.stringPropertyNames()) {
                assertFalse(key.startsWith("\uFEFF"), "Key in " + f.getName() + " starts with BOM: " + key);
                assertFalse(key.contains("\uFEFF"), "Key in " + f.getName() + " contains BOM: " + key);
            }
        }

        // 4. Test Arc I18NBundle loading on bundle.properties and bundle_vi.properties
        Fi baseFi = new Fi(new File(dir, "bundle"));
        I18NBundle testBundle = I18NBundle.createBundle(baseFi, Locale.ENGLISH);
        assertNotNull(testBundle);
        assertEquals("Smart Upgrade", testBundle.get("feature.smart-upgrade.name"));
        assertEquals("Smart Upgrade Settings", testBundle.get("feature.smart-upgrade.settings.title"));

        I18NBundle testViBundle = I18NBundle.createBundle(baseFi, new Locale("vi"));
        assertNotNull(testViBundle);
        assertEquals("Nâng Cấp Thông Minh", testViBundle.get("feature.smart-upgrade.name"));
        assertEquals("Cài Đặt Nâng Cấp Thông Minh", testViBundle.get("feature.smart-upgrade.settings.title"));
        assertEquals("Số công trình tối đa mỗi lần nâng cấp", testViBundle.get("feature.smart-upgrade.settings.max-upgrades"));
        assertEquals("Thời gian nhấn giữ (ms)", testViBundle.get("feature.smart-upgrade.settings.hold-duration"));
        assertEquals("Bật/Tắt Nâng Cấp Thông Minh", testViBundle.get("keybind.smartUpgradeToggle.name"));
    }

    @Test
    void testMetadata() {
        SmartUpgradeFeature feature = new SmartUpgradeFeature();
        FeatureMetadata meta = feature.getMetadata();

        assertNotNull(meta);
        assertEquals("smart-upgrade", meta.getId());
        assertNotNull(meta.getIcon());
        assertTrue(meta.isEnabledByDefault());
        assertTrue(meta.isQuickAccess());
        assertFalse(meta.isDevelopment());
    }

    @Test
    void testDefaultConfigs() {
        SmartUpgradeFeature feature = new SmartUpgradeFeature();

        assertEquals(500, feature.maxUpdatesConfig.get());
        assertEquals(300, feature.holdDurationConfig.get());
        assertFalse(feature.onlySameTypeConfig.get());
        assertTrue(feature.traverseBridgesConfig.get());
    }

    @Test
    void testConfigMutationsAndReset() {
        SmartUpgradeFeature feature = new SmartUpgradeFeature();

        feature.maxUpdatesConfig.set(1000);
        feature.holdDurationConfig.set(450);
        feature.onlySameTypeConfig.set(true);
        feature.traverseBridgesConfig.set(false);

        assertEquals(1000, feature.maxUpdatesConfig.get());
        assertEquals(450, feature.holdDurationConfig.get());
        assertTrue(feature.onlySameTypeConfig.get());
        assertFalse(feature.traverseBridgesConfig.get());

        feature.resetToDefaults();

        assertEquals(500, feature.maxUpdatesConfig.get());
        assertEquals(300, feature.holdDurationConfig.get());
        assertFalse(feature.onlySameTypeConfig.get());
        assertTrue(feature.traverseBridgesConfig.get());
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

    @Test
    void testNullSafeExecution() {
        SmartUpgradeFeature feature = new SmartUpgradeFeature();

        assertDoesNotThrow(() -> feature.upgradeChain(null, null));
        assertDoesNotThrow(feature::triggerHoveredUpgrade);
        assertDoesNotThrow(() -> feature.showMenu(null));
        assertDoesNotThrow(feature::closeMenu);
        assertDoesNotThrow(feature::onDisable);
    }

    @Test
    void testSettingDialogProvider() {
        SmartUpgradeFeature feature = new SmartUpgradeFeature();
        assertNotNull(feature.getSettingDialog());
    }
}
