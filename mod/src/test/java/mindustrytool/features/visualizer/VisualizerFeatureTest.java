package mindustrytool.features.visualizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.game.Rules;
import mindustry.content.UnitTypes;
import mindustry.gen.BlockUnitUnit;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.type.UnitType;
import mindustry.world.Tile;
import mindustry.Vars;
import mindustry.world.blocks.defense.BuildTurret;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.ItemTurret.ItemTurretBuild;
import mindustrytool.features.FeatureKeybind;
import mindustrytool.features.visualizer.VisualizerFeature.BridgeFlowState;
import mindustrytool.test.MindustryTestEnv;

class VisualizerFeatureTest extends MindustryTestEnv {

    @BeforeEach
    void setUp() {
        Vars.content = new ContentLoader();
        if (Vars.state == null) {
            Vars.state = new GameState();
        }
        if (Vars.state.rules == null) {
            Vars.state.rules = new Rules();
        }
        if (Icon.distribution == null) {
            Icon.distribution = new TextureRegionDrawable(new TextureRegion());
        }
        if (Icon.refresh == null) {
            Icon.refresh = new TextureRegionDrawable(new TextureRegion());
        }
        if (UnitTypes.block == null) {
            UnitTypes.block = new UnitType("block");
            UnitTypes.block.constructor = BlockUnitUnit::create;
        }
    }

    @Test
    void testConfigMutationsAndReset() {
        VisualizerFeature feature = new VisualizerFeature();

        feature.showItemBridgesConfig.set(false);
        feature.showDuctBridgesConfig.set(false);
        feature.showLiquidBridgesConfig.set(false);
        feature.bridgeItemScaleConfig.set(1.5f);
        feature.bridgeOpacityConfig.set(0.7f);
        feature.showBridgeFlowRateConfig.set(false);
        feature.hideIdleBridgeFlowConfig.set(true);
        feature.bridgeFlowRateScaleConfig.set(1.4f);

        feature.showAmmoBadgeConfig.set(false);
        feature.showTargetLineConfig.set(false);
        feature.targetLineAllyConfig.set(false);
        feature.targetLineEnemyConfig.set(true);
        feature.onlyWhenShootingConfig.set(true);
        feature.turretBadgeScaleConfig.set(1.8f);
        feature.targetLineOpacityConfig.set(0.9f);

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

        feature.resetToDefaults();

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

        FeatureKeybind settingsBind = feature.getKeybinds().find(k -> k.getBind().name.equals("visualizersettings"));
        assertNotNull(settingsBind);
        assertFalse(settingsBind.isRequireEnabled());
    }

    @Test
    void testTurretFilterLogic() {
        VisualizerFeature feature = new VisualizerFeature();
        ItemTurret duo = new ItemTurret("vis-test-duo");
        ItemTurret lancer = new ItemTurret("vis-test-lancer");
        BuildTurret buildTower = new BuildTurret("vis-test-build-tower");

        assertTrue(feature.isTurretBlock(duo));
        assertTrue(feature.isTurretBlock(lancer));
        assertFalse(feature.isTurretBlock(buildTower));

        feature.rebuildBitSet();

        assertTrue(feature.isTurretEnabled(duo));
        assertTrue(feature.isTurretEnabled(lancer));

        feature.setTurretEnabled(duo, false);
        flushEffects();
        assertFalse(feature.isTurretEnabled(duo));
        assertFalse(feature.getTurretSignal(duo).peek());
        assertTrue(feature.isTurretEnabled(lancer));

        feature.setTurretEnabled(duo, true);
        flushEffects();
        assertTrue(feature.isTurretEnabled(duo));
        assertTrue(feature.getTurretSignal(duo).peek());

        feature.setAllTurretsEnabled(false);
        flushEffects();
        assertFalse(feature.isTurretEnabled(duo));
        assertFalse(feature.isTurretEnabled(lancer));

        feature.setAllTurretsEnabled(true);
        flushEffects();
        assertTrue(feature.isTurretEnabled(duo));
        assertTrue(feature.isTurretEnabled(lancer));

        feature.setTurretEnabled(duo, false);
        flushEffects();
        assertFalse(feature.isTurretEnabled(duo));
        feature.resetToDefaults();
        flushEffects();
        assertTrue(feature.isTurretEnabled(duo));
    }

    @Test
    void testCalculateBadgeSize() {
        VisualizerFeature feature = new VisualizerFeature();

        assertEquals(3.9f, feature.calculateBadgeSize(1), 0.001f);

        assertEquals(5.4f, feature.calculateBadgeSize(2), 0.001f);

        assertEquals(6.9f, feature.calculateBadgeSize(3), 0.001f);

        feature.turretBadgeScaleConfig.set(0.5f);
        flushEffects();
        assertEquals(1.95f, feature.calculateBadgeSize(1), 0.001f);
        assertEquals(2.7f, feature.calculateBadgeSize(2), 0.001f);
    }

    @Test
    void testBridgeFlowStateRollingCalculation() {
        BridgeFlowState state = new BridgeFlowState();
        assertEquals(0f, state.calculateRate(), 0.001f);
        assertEquals("0/s", state.formatRate(0f));

        state.step(10f);
        state.addItem(3f);
        assertEquals(3f, state.calculateRate(), 0.001f);

        state.step(20f);
        state.addItem(2f);

        assertEquals(5f, state.calculateRate(), 0.001f);

        state.step(70f);

        assertEquals(2f, state.calculateRate(), 0.001f);

        state.step(90f);
        assertEquals(0f, state.calculateRate(), 0.001f);
        assertEquals("0/s", state.formatRate(state.calculateRate()));
    }

    @Test
    void testFormatFlowRate() {
        assertEquals("0/s", VisualizerFeature.formatFlowRate(0f));
        assertEquals("0/s", VisualizerFeature.formatFlowRate(0.0005f));
        assertEquals("3.5/s", VisualizerFeature.formatFlowRate(3.5f));
        assertEquals("9.9/s", VisualizerFeature.formatFlowRate(9.9f));
        assertEquals("10/s", VisualizerFeature.formatFlowRate(10.0f));
        assertEquals("25/s", VisualizerFeature.formatFlowRate(25.4f));
        assertEquals("100/s", VisualizerFeature.formatFlowRate(100.2f));
    }

    @Test
    void testLiquidFlowRateCalculation() {
        VisualizerFeature feature = new VisualizerFeature();

        float rate = feature.calculateLiquidFlowRate(null, null, null, false);
        assertEquals(0f, rate, 0.001f);

        ItemTurret dummyBlock = new ItemTurret("vis-dummy-liq");
        dummyBlock.hasLiquids = false;
        Building b1 = Building.create();
        b1.block = dummyBlock;
        Building b2 = Building.create();
        b2.block = dummyBlock;

        assertEquals(0f, feature.calculateLiquidFlowRate(b1, b2, null, true), 0.001f);
    }

    @Test
    void testCalculateAmmoFraction() {
        VisualizerFeature feature = new VisualizerFeature();
        assertEquals(1f, feature.calculateAmmoFraction(null), 0.001f);

        ItemTurret duo = new ItemTurret("vis-ammo-duo");
        duo.maxAmmo = 30;
        ItemTurretBuild build = duo.new ItemTurretBuild();
        build.block = duo;
        build.tile = new Tile(2, 2);

        build.totalAmmo = 0;
        assertEquals(0f, feature.calculateAmmoFraction(build), 0.001f);

        build.totalAmmo = 5;
        assertEquals(5f / 30f, feature.calculateAmmoFraction(build), 0.001f);

        build.totalAmmo = 15;
        assertEquals(0.5f, feature.calculateAmmoFraction(build), 0.001f);

        build.totalAmmo = 30;
        assertEquals(1.0f, feature.calculateAmmoFraction(build), 0.001f);

        build.totalAmmo = 45;
        assertEquals(1.0f, feature.calculateAmmoFraction(build), 0.001f);
    }

    @Test
    void testTargetHealthFraction() {
        VisualizerFeature feature = new VisualizerFeature();

        assertEquals(1.0f, feature.getTargetHealthFraction(null), 0.001f);

        ItemTurret dummyBlock = new ItemTurret("vis-target-dummy");
        Building target = Building.create();
        target.block = dummyBlock;
        target.maxHealth = 200f;

        target.health = 200f;
        assertEquals(1.0f, feature.getTargetHealthFraction(target), 0.001f);

        target.health = 100f;
        assertEquals(0.5f, feature.getTargetHealthFraction(target), 0.001f);

        target.health = 30f;
        assertEquals(0.15f, feature.getTargetHealthFraction(target), 0.001f);

        target.health = 0f;
        assertEquals(0.0f, feature.getTargetHealthFraction(target), 0.001f);
    }

    @Test
    void testBridgeFlowStateManagement() {
        VisualizerFeature feature = new VisualizerFeature();
        ItemTurret dummy = new ItemTurret("vis-dummy-bridge");
        Building b = Building.create();
        b.block = dummy;
        b.tile = new Tile(5, 5);

        assertEquals(0f, feature.getBridgeFlowRate(b), 0.001f);

        feature.recordBridgeItemTransfer(b, 4f);
        assertEquals(4f, feature.getBridgeFlowRate(b), 0.001f);

        feature.resetToDefaults();
        assertEquals(0f, feature.getBridgeFlowRate(b), 0.001f);
    }
}
