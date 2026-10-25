package mindustrytool.ui.dock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import mindustrytool.test.MindustryTestEnv;
import solim.config.ConfigGroup;
import solim.config.ContextualConfigValue;
import solim.overlay.DockAnchor;
import solim.overlay.DockSide;
import solim.reactive.Signals;

class DockPositionConfigTest extends MindustryTestEnv {

    private DockPositionConfig createConfig(String namespace) {
        ConfigGroup group = ConfigGroup.of(namespace);
        ContextualConfigValue<Float, Boolean> xConfig = group.floatValueKeyed(
                "x",
                Signals.isPortrait(),
                portrait -> portrait ? "portrait" : "landscape",
                120f);
        ContextualConfigValue<Float, Boolean> yConfig = group.floatValueKeyed(
                "y",
                Signals.isPortrait(),
                portrait -> portrait ? "portrait" : "landscape",
                240f);
        return new DockPositionConfig(group, xConfig, yConfig);
    }

    @AfterEach
    void resetOrientation() {
        Signals.isPortrait().set(false);
        flushEffects();
    }

    @Test
    void persistsDockedAnchorAndClearsOnFreeSave() {
        DockPositionConfig config = createConfig("dock-position-test");
        DockAnchor anchor = new DockAnchor("minimap", DockSide.BOTTOM, 50f, 210f, 146f);

        config.save(anchor);
        flushEffects();

        DockAnchor loaded = config.load();
        assertEquals(anchor, loaded);
        assertTrue(config.isDocked());

        config.save(null);
        flushEffects();

        assertNull(config.load());
        assertFalse(config.isDocked());
    }

    @Test
    void keepsPortraitAndLandscapeAnchorsSeparate() {
        DockPositionConfig config = createConfig("dock-position-orientation-test");
        DockAnchor portraitAnchor = new DockAnchor("minimap", DockSide.BOTTOM, 10f, 100f, 200f);
        DockAnchor landscapeAnchor = new DockAnchor("minimap", DockSide.RIGHT, 20f, 300f, 400f);

        Signals.isPortrait().set(true);
        flushEffects();
        config.save(portraitAnchor);
        flushEffects();

        Signals.isPortrait().set(false);
        flushEffects();
        assertNull(config.load(), "Landscape must not reuse the portrait anchor");

        config.save(landscapeAnchor);
        flushEffects();

        Signals.isPortrait().set(true);
        flushEffects();
        assertEquals(portraitAnchor, config.load());

        Signals.isPortrait().set(false);
        flushEffects();
        assertEquals(landscapeAnchor, config.load());
    }

    @Test
    void readsLegacyCoordinatesWhenNoDockIsSaved() {
        Core.settings.put("legacy-dock.x.landscape", 321f);
        Core.settings.put("legacy-dock.y.landscape", 654f);

        ConfigGroup group = ConfigGroup.of("dock-migration-test");
        ContextualConfigValue<Float, Boolean> xConfig = group.floatValueKeyed(
                "x",
                Signals.isPortrait(),
                portrait -> portrait ? "portrait" : "landscape",
                portrait -> Core.settings.getFloat("legacy-dock.x." + (portrait ? "portrait" : "landscape"),
                        120f));
        ContextualConfigValue<Float, Boolean> yConfig = group.floatValueKeyed(
                "y",
                Signals.isPortrait(),
                portrait -> portrait ? "portrait" : "landscape",
                portrait -> Core.settings.getFloat("legacy-dock.y." + (portrait ? "portrait" : "landscape"),
                        240f));

        DockPositionConfig config = new DockPositionConfig(group, xConfig, yConfig);

        assertNull(config.load());
        assertEquals(321f, config.x(120f), 0.01f);
        assertEquals(654f, config.y(240f), 0.01f);
    }

    @Test
    void resetClearsDockAndRestoresFreeCoordinates() {
        DockPositionConfig config = createConfig("dock-position-reset-test");
        config.save(new DockAnchor("minimap", DockSide.BOTTOM, 50f, 210f, 146f));

        config.resetTo(80f, 90f);
        flushEffects();

        assertNull(config.load());
        assertEquals(80f, config.x(0f), 0.01f);
        assertEquals(90f, config.y(0f), 0.01f);
    }
}
