package mindustrytool.features.visualizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.func.Prov;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import mindustry.core.ContentLoader;
import mindustry.gen.Icon;
import mindustry.Vars;
import mindustry.world.blocks.defense.BuildTurret;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustrytool.features.FeatureKeybind;
import mindustrytool.test.MindustryTestEnv;
import solim.overlay.SolimDialog;

class VisualizerFeatureTest extends MindustryTestEnv {

    @BeforeEach
    void setUp() {
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
        if (Icon.distribution == null) {
            Icon.distribution = new TextureRegionDrawable(new TextureRegion());
        }
        if (Icon.refresh == null) {
            Icon.refresh = new TextureRegionDrawable(new TextureRegion());
        }
    }

    @Test
    void testDefaultConfigs() {
        VisualizerFeature feature = new VisualizerFeature();

        // Bridges
        assertTrue(feature.showItemBridgesConfig.get());
        assertTrue(feature.showDuctBridgesConfig.get());
        assertTrue(feature.showLiquidBridgesConfig.get());
        assertEquals(1.0f, feature.bridgeItemScaleConfig.get(), 0.001f);
        assertEquals(1.0f, feature.bridgeOpacityConfig.get(), 0.001f);
        assertTrue(feature.showBridgeFlowRateConfig.get());
        assertFalse(feature.hideIdleBridgeFlowConfig.get());
        assertEquals(1.0f, feature.bridgeFlowRateScaleConfig.get(), 0.001f);

        // Turrets
        assertTrue(feature.showAmmoBadgeConfig.get());
        assertTrue(feature.showTargetLineConfig.get());
        assertTrue(feature.targetLineAllyConfig.get());
        assertFalse(feature.targetLineEnemyConfig.get());
        assertFalse(feature.onlyWhenShootingConfig.get());
        assertEquals(1.0f, feature.turretBadgeScaleConfig.get(), 0.001f);
        assertEquals(0.6f, feature.targetLineOpacityConfig.get(), 0.001f);
    }

    @Test
    void testConfigMutationsAndReset() {
        VisualizerFeature feature = new VisualizerFeature();

        // Mutate bridge configs
        feature.showItemBridgesConfig.set(false);
        feature.showDuctBridgesConfig.set(false);
        feature.showLiquidBridgesConfig.set(false);
        feature.bridgeItemScaleConfig.set(1.5f);
        feature.bridgeOpacityConfig.set(0.7f);
        feature.showBridgeFlowRateConfig.set(false);
        feature.hideIdleBridgeFlowConfig.set(true);
        feature.bridgeFlowRateScaleConfig.set(1.4f);

        // Mutate turret configs
        feature.showAmmoBadgeConfig.set(false);
        feature.showTargetLineConfig.set(false);
        feature.targetLineAllyConfig.set(false);
        feature.targetLineEnemyConfig.set(true);
        feature.onlyWhenShootingConfig.set(true);
        feature.turretBadgeScaleConfig.set(1.8f);
        feature.targetLineOpacityConfig.set(0.9f);

        // Verify mutations
        assertFalse(feature.showItemBridgesConfig.get());
        assertFalse(feature.showDuctBridgesConfig.get());
        assertFalse(feature.showLiquidBridgesConfig.get());
        assertEquals(1.5f, feature.bridgeItemScaleConfig.get(), 0.001f);
        assertEquals(0.7f, feature.bridgeOpacityConfig.get(), 0.001f);
        assertFalse(feature.showBridgeFlowRateConfig.get());
        assertTrue(feature.hideIdleBridgeFlowConfig.get());
        assertEquals(1.4f, feature.bridgeFlowRateScaleConfig.get(), 0.001f);

        assertFalse(feature.showAmmoBadgeConfig.get());
        assertFalse(feature.showTargetLineConfig.get());
        assertFalse(feature.targetLineAllyConfig.get());
        assertTrue(feature.targetLineEnemyConfig.get());
        assertTrue(feature.onlyWhenShootingConfig.get());
        assertEquals(1.8f, feature.turretBadgeScaleConfig.get(), 0.001f);
        assertEquals(0.9f, feature.targetLineOpacityConfig.get(), 0.001f);

        // Reset
        feature.resetToDefaults();

        // Verify defaults restored
        assertTrue(feature.showItemBridgesConfig.get());
        assertTrue(feature.showDuctBridgesConfig.get());
        assertTrue(feature.showLiquidBridgesConfig.get());
        assertEquals(1.0f, feature.bridgeItemScaleConfig.get(), 0.001f);
        assertEquals(1.0f, feature.bridgeOpacityConfig.get(), 0.001f);
        assertTrue(feature.showBridgeFlowRateConfig.get());
        assertFalse(feature.hideIdleBridgeFlowConfig.get());
        assertEquals(1.0f, feature.bridgeFlowRateScaleConfig.get(), 0.001f);

        assertTrue(feature.showAmmoBadgeConfig.get());
        assertTrue(feature.showTargetLineConfig.get());
        assertTrue(feature.targetLineAllyConfig.get());
        assertFalse(feature.targetLineEnemyConfig.get());
        assertFalse(feature.onlyWhenShootingConfig.get());
        assertEquals(1.0f, feature.turretBadgeScaleConfig.get(), 0.001f);
        assertEquals(0.6f, feature.targetLineOpacityConfig.get(), 0.001f);
    }

    @Test
    void testKeybindsRegistration() {
        VisualizerFeature feature = new VisualizerFeature();
        assertEquals(2, feature.getKeybinds().size);

        FeatureKeybind toggleBind = feature.getKeybinds().find(k -> k.getBind().name.equals("visualizer"));
        assertNotNull(toggleBind);
        assertFalse(toggleBind.isRequireEnabled());

        boolean initial = feature.isEnabled();
        toggleBind.getAction().run();
        assertEquals(!initial, feature.isEnabled());

        FeatureKeybind settingsBind = feature.getKeybinds().find(k -> k.getBind().name.equals("visualizerSettings"));
        assertNotNull(settingsBind);
        assertFalse(settingsBind.isRequireEnabled());
    }

    @Test
    void testSettingDialog() {
        VisualizerFeature feature = new VisualizerFeature();
        Prov<SolimDialog> dialogProv = feature.getSettingDialog();
        assertNotNull(dialogProv);
    }

    @Test
    void testTurretFilterLogic() {
        VisualizerFeature feature = new VisualizerFeature();
        ItemTurret duo = new ItemTurret("test-duo");
        ItemTurret lancer = new ItemTurret("test-lancer");
        BuildTurret buildTower = new BuildTurret("test-build-tower");

        assertTrue(feature.isTurretBlock(duo));
        assertTrue(feature.isTurretBlock(lancer));
        assertFalse(feature.isTurretBlock(buildTower));

        feature.rebuildBitSet();

        // By default turrets are enabled
        assertTrue(feature.isTurretEnabled(duo));
        assertTrue(feature.isTurretEnabled(lancer));

        // Toggle individual turret
        feature.setTurretEnabled(duo, false);
        assertFalse(feature.isTurretEnabled(duo));
        assertFalse(feature.getTurretSignal(duo).peek());
        assertTrue(feature.isTurretEnabled(lancer));

        // Re-enable
        feature.setTurretEnabled(duo, true);
        assertTrue(feature.isTurretEnabled(duo));
        assertTrue(feature.getTurretSignal(duo).peek());

        // Test setAllTurretsEnabled
        feature.setAllTurretsEnabled(false);
        assertFalse(feature.isTurretEnabled(duo));
        assertFalse(feature.isTurretEnabled(lancer));

        feature.setAllTurretsEnabled(true);
        assertTrue(feature.isTurretEnabled(duo));
        assertTrue(feature.isTurretEnabled(lancer));

        // Test resetToDefaults
        feature.setTurretEnabled(duo, false);
        assertFalse(feature.isTurretEnabled(duo));
        feature.resetToDefaults();
        assertTrue(feature.isTurretEnabled(duo));
    }

    @Test
    void testCalculateBadgeSize() {
        VisualizerFeature feature = new VisualizerFeature();

        // Default scale (1.0f):
        // 1x1 turret: 2.4 + 1.5 = 3.9f
        assertEquals(3.9f, feature.calculateBadgeSize(1), 0.001f);
        // 2x2 turret: 2.4 + 3.0 = 5.4f
        assertEquals(5.4f, feature.calculateBadgeSize(2), 0.001f);
        // 3x3 turret: 2.4 + 4.5 = 6.9f
        assertEquals(6.9f, feature.calculateBadgeSize(3), 0.001f);

        // Mutated scale (0.5f)
        feature.turretBadgeScaleConfig.set(0.5f);
        assertEquals(1.95f, feature.calculateBadgeSize(1), 0.001f);
        assertEquals(2.7f, feature.calculateBadgeSize(2), 0.001f);
    }
}
