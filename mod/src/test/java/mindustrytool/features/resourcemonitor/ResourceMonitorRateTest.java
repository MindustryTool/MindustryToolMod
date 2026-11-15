package mindustrytool.features.resourcemonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.graphics.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.ctype.MappableContent;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.world.modules.ItemModule;
import org.junit.jupiter.api.Test;
import mindustrytool.test.MindustryTestEnv;

class ResourceMonitorRateTest extends MindustryTestEnv {

    private static Item allocateItem(short id, String name) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        Field nameField = MappableContent.class.getDeclaredField("name");
        nameField.setAccessible(true);

        Item item = (Item) allocateInstance.invoke(unsafe, Item.class);
        nameField.set(item, name);
        item.id = id;
        return item;
    }

    @Test
    void testConfigSettersAndResetToDefaults() {
        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        feature.opacityConfig.set(0.5f);
        feature.scaleConfig.set(1.5f);
        feature.overlayWidthConfig.set(0.4f);
        feature.overlayHeightConfig.set(0.4f);
        feature.showUnitsConfig.set(true);
        feature.showStoredPowerConfig.set(true);
        feature.alwaysShowFlowRateConfig.set(false);
        feature.expandedConfig.set(false);

        assertEquals(0.5f, feature.opacityConfig.get(), 0.001f);
        assertEquals(1.5f, feature.scaleConfig.get(), 0.001f);
        assertEquals(0.4f, feature.overlayWidthConfig.get(), 0.001f);
        assertEquals(0.4f, feature.overlayHeightConfig.get(), 0.001f);
        assertTrue(feature.showUnitsConfig.get());
        assertTrue(feature.showStoredPowerConfig.get());
        assertFalse(feature.alwaysShowFlowRateConfig.get());
        assertFalse(feature.expandedConfig.get());

        feature.resetToDefaults();

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
        assertTrue(feature.hideZeroUnitsConfig.get());
        assertFalse(feature.hideZeroItemsConfig.get());
        assertTrue(feature.expandedConfig.get());
    }

    @Test
    void testFeatureConfigSignals() {
        ResourceMonitorFeature feature = new ResourceMonitorFeature();

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
    }

    @Test
    void testItemFlowRateCalculationAndFormatting() throws Exception {
        Vars.content = new ContentLoader();
        Item copper = allocateItem((short) 0, "copper");
        Item lead = allocateItem((short) 1, "lead");
        Vars.content.items().clear();
        Vars.content.items().add(copper);
        Vars.content.items().add(lead);

        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        ResourceMonitorState state = feature.getState();

        ItemModule module = new ItemModule();
        module.add(copper, 100);
        module.add(lead, 200);
        state.setCoreItems(module);

        assertTrue(state.stepRates());
        assertEquals(0, state.getItemRate(copper));
        assertEquals("0/s", state.getFormattedRate(copper));
        assertEquals(Color.gray, state.getRateColor(copper));

        module.add(copper, 25);
        module.remove(lead, 15);
        assertTrue(state.stepRates());

        assertEquals(50, state.getItemRate(copper));
        assertEquals("+50/s", state.getFormattedRate(copper));
        assertEquals(Pal.heal, state.getRateColor(copper));

        assertEquals(-30, state.getItemRate(lead));
        assertEquals("-30/s", state.getFormattedRate(lead));
        assertEquals(Pal.remove, state.getRateColor(lead));

        feature.alwaysShowFlowRateConfig.set(true);
        String copperFormatted = state.formatItem(copper).toString();
        assertTrue(copperFormatted.contains("125"));
        assertTrue(copperFormatted.contains("+50"));

        feature.alwaysShowFlowRateConfig.set(false);
        state.setViewingStats(false);
        assertEquals("[lime]125", state.formatItem(copper).toString());
        assertEquals("[scarlet]185", state.formatItem(lead).toString());

        state.setViewingStats(true);
        assertTrue(state.formatItem(copper).toString().contains("+50"));
        assertTrue(state.formatItem(lead).toString().contains("-30"));
    }

    @Test
    void testRefreshZeroFiltersRemovesDepletedItems() throws Exception {
        Vars.content = new ContentLoader();
        Item copper = allocateItem((short) 0, "copper");
        Item lead = allocateItem((short) 1, "lead");
        Vars.content.items().clear();
        Vars.content.items().add(copper);
        Vars.content.items().add(lead);

        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        ResourceMonitorState state = feature.getState();

        ItemModule module = new ItemModule();
        module.add(copper, 50);
        state.setCoreItems(module);

        state.getUsedItems().add(copper);
        state.getUsedItems().add(lead);

        feature.hideZeroItemsConfig.set(true);
        state.refreshZeroFilters();

        assertTrue(state.getUsedItems().contains(copper));
        assertFalse(state.getUsedItems().contains(lead));
        assertEquals(1, state.usedItemsSignal.get().size);
        assertEquals(copper, state.usedItemsSignal.get().first());
    }

    @Test
    void testStateSignalsAndNullSafeFormatters() {
        ResourceMonitorFeature feature = new ResourceMonitorFeature();
        ResourceMonitorState state = feature.getState();

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
