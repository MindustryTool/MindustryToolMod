package mindustrytool.features.smartdrill;

import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartDrillFeatureTest extends MindustryTestEnv {

    private SmartDrillFeature feature;

    @BeforeEach
    void setUp() {
        feature = new SmartDrillFeature();
    }

    @Test
    void autoDisableStartsEnabledAndResetRestoresIt() {
        assertTrue(feature.autoDisableConfig.get());

        feature.autoDisableConfig.set(false);
        assertFalse(feature.autoDisableConfig.get());

        feature.resetToDefaults();
        assertTrue(feature.autoDisableConfig.get());
    }

    @Test
    void bindingsStartEmpty() {
        assertEquals("", feature.getBinding());
    }

    @Test
    void planStartsEmptyAndPanelStartsClosed() {
        assertTrue(feature.getPlan().builds.isEmpty());
        assertFalse(feature.isPanelOpen());
    }

    @Test
    void rectStartsAtOneTile() {
        assertNotNull(feature.getRect());
        assertEquals(1, feature.getRect().width);
        assertEquals(1, feature.getRect().height);
    }

    @Test
    void drillListIsEmptyWithoutATarget() {
        assertTrue(feature.drills().get().isEmpty());
        assertTrue(feature.rigs().get().isEmpty());
        assertFalse(feature.hasRigs().get());
    }
}