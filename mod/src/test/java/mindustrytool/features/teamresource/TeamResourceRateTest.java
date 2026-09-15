package mindustrytool.features.teamresource;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import arc.graphics.Color;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;

class TeamResourceRateTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
    }

    @Test
    void testConfigDefaults() {
        TeamResourceFeature feature = new TeamResourceFeature();
        assertEquals(1f, feature.opacityConfig.get(), 0.001f);
        assertEquals(1f, feature.scaleConfig.get(), 0.001f);
        assertEquals(0.28f, feature.overlayWidthConfig.get(), 0.001f);
        assertEquals(0.60f, feature.overlayHeightConfig.get(), 0.001f);
        assertTrue(feature.showItemsConfig.get());
        assertFalse(feature.showUnitsConfig.get());
        assertTrue(feature.showPowerConfig.get());
        assertFalse(feature.showStoredPowerConfig.get());
        assertFalse(feature.hideBackgroundConfig.get());
        assertTrue(feature.alwaysShowFlowRateConfig.get());
        assertTrue(feature.expandedConfig.get());
    }

    @Test
    void testConfigSetters() {
        TeamResourceFeature feature = new TeamResourceFeature();
        feature.opacityConfig.set(0.8f);
        assertEquals(0.8f, feature.opacityConfig.get(), 0.001f);

        feature.scaleConfig.set(1.2f);
        assertEquals(1.2f, feature.scaleConfig.get(), 0.001f);

        feature.overlayWidthConfig.set(0.4f);
        assertEquals(0.4f, feature.overlayWidthConfig.get(), 0.001f);

        feature.overlayHeightConfig.set(0.85f);
        assertEquals(0.85f, feature.overlayHeightConfig.get(), 0.001f);

        feature.showUnitsConfig.set(true);
        assertTrue(feature.showUnitsConfig.get());

        feature.showStoredPowerConfig.set(true);
        assertTrue(feature.showStoredPowerConfig.get());

        feature.alwaysShowFlowRateConfig.set(true);
        assertTrue(feature.alwaysShowFlowRateConfig.get());
    }

    @Test
    void testResetToDefaults() {
        TeamResourceFeature feature = new TeamResourceFeature();
        feature.opacityConfig.set(0.5f);
        feature.scaleConfig.set(1.5f);
        feature.overlayHeightConfig.set(0.4f);
        feature.showUnitsConfig.set(true);
        feature.alwaysShowFlowRateConfig.set(false);
        feature.expandedConfig.set(false);

        feature.opacityConfig.reset();
        feature.scaleConfig.reset();
        feature.overlayWidthConfig.reset();
        feature.overlayHeightConfig.reset();
        feature.showUnitsConfig.reset();
        feature.alwaysShowFlowRateConfig.reset();
        feature.expandedConfig.reset();

        assertEquals(1f, feature.opacityConfig.get(), 0.001f);
        assertEquals(1f, feature.scaleConfig.get(), 0.001f);
        assertEquals(0.28f, feature.overlayWidthConfig.get(), 0.001f);
        assertEquals(0.60f, feature.overlayHeightConfig.get(), 0.001f);
        assertTrue(feature.showItemsConfig.get());
        assertFalse(feature.showUnitsConfig.get());
        assertTrue(feature.showPowerConfig.get());
        assertFalse(feature.showStoredPowerConfig.get());
        assertFalse(feature.hideBackgroundConfig.get());
        assertTrue(feature.alwaysShowFlowRateConfig.get());
        assertTrue(feature.expandedConfig.get());
    }

    @Test
    void testTeamResourceStateInit() {
        TeamResourceState state = new TeamResourceState();
        assertNotNull(state.getUsedItems());
        assertNotNull(state.getUsedUnits());
        assertNotNull(state.getTeamGraphs());
        assertEquals(0f, state.getTotalPowerBalance(), 0.001f);
        assertEquals(0f, state.getTotalStoredPower(), 0.001f);
    }

    @Test
    void testTableLayout() {
        Table table = new Table();
        table.defaults().growX();
        int cols = 4;
        for (int i = 0; i < 5; i++) {
            table.add(new Table());
            if ((i + 1) % cols == 0) {
                table.row();
            }
        }
        Table table2 = new Table();
        table2.defaults().growX();
        int total = 2;
        int cols2 = 4;
        for (int i = 0; i < total; i++) {
            table2.add(new Table());
        }
        if (total % cols2 != 0) {
            for (int k = total % cols2; k < cols2; k++) {
                table2.add().growX();
            }
        }
        table2.setSize(300, 100);
        table2.layout();
        @SuppressWarnings("rawtypes")
        Seq<Cell> cells2 = table2.getCells();
        assertEquals(4, cells2.size);
        assertEquals(75f, cells2.get(0).get().getWidth(), 0.1f);
        assertEquals(75f, cells2.get(1).get().getWidth(), 0.1f);
    }

    @Test
    void testFitContentCalculation() {
        float contentHeight = 180f;
        float maxAllowedHeight = 600f;
        float actualHeight = Math.min(contentHeight, maxAllowedHeight);
        assertEquals(180f, actualHeight, 0.001f);

        float largeContentHeight = 850f;
        float cappedHeight = Math.min(largeContentHeight, maxAllowedHeight);
        assertEquals(600f, cappedHeight, 0.001f);
    }

    @Test
    void testFeatureConfigSignals() {
        TeamResourceFeature feature = new TeamResourceFeature();

        assertEquals(1f, feature.opacityConfig.get(), 0.001f);
        assertEquals(1f, feature.scaleConfig.get(), 0.001f);
        assertTrue(feature.showItemsConfig.get());
        assertFalse(feature.showUnitsConfig.get());
        assertTrue(feature.showPowerConfig.get());
        assertFalse(feature.showStoredPowerConfig.get());
        assertFalse(feature.hideBackgroundConfig.get());
        assertTrue(feature.alwaysShowFlowRateConfig.get());
        assertTrue(feature.expandedConfig.get());

        feature.opacityConfig.set(0.75f);
        assertEquals(0.75f, feature.opacityConfig.signal().get(), 0.001f);

        feature.scaleConfig.set(1.25f);
        assertEquals(1.25f, feature.scaleConfig.signal().get(), 0.001f);

        feature.showUnitsConfig.set(true);
        assertTrue(feature.showUnitsConfig.signal().get());

        feature.x(150f);
        feature.y(250f);
        assertEquals(150f, feature.xSignal.get(), 0.001f);
        assertEquals(250f, feature.ySignal.get(), 0.001f);

        feature.resetToDefaults();
        assertEquals(1f, feature.opacityConfig.get(), 0.001f);
        assertEquals(1f, feature.scaleConfig.get(), 0.001f);
        assertFalse(feature.showUnitsConfig.get());

        feature.resetPosition();
        assertNotNull(feature.xSignal.get());
        assertNotNull(feature.ySignal.get());
    }

    @Test
    void testStateSignalsAndFormatters() {
        TeamResourceFeature feature = new TeamResourceFeature();
        TeamResourceState state = feature.getState();

        assertNotNull(state.selectedTeamSignal.get());
        long initialTick = state.tickSignal.get();

        state.reset();
        assertTrue(state.tickSignal.get() > initialTick);

        assertEquals("0", state.getFormattedAmount(null));
        assertEquals("0/s", state.getFormattedRate(null));
        assertEquals(Color.gray, state.getRateColor(null));
        assertEquals("", state.getUnitCountText(null));
        assertTrue(state.getFormattedPowerBalance().contains("0"));
        assertTrue(state.getFormattedStoredPower().contains("/"));
    }
}
