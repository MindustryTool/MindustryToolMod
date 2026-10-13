package mindustrytool.features.callout;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import mindustrytool.test.MindustryTestEnv;

class CalloutFeatureTest extends MindustryTestEnv {

    @Test
    void testConfigDefaults() {
        CalloutFeature feature = new CalloutFeature();

        assertTrue(feature.teamChatConfig.get(), "teamChatConfig should default to true");
        assertTrue(feature.closeOnSendConfig.get(), "closeOnSendConfig should default to true");
        assertTrue(feature.isEnabled(), "CalloutFeature should be enabled by default");
    }

    @Test
    void testEnableDisable() {
        CalloutFeature feature = new CalloutFeature();

        assertTrue(feature.isEnabled());

        feature.disable();
        assertFalse(feature.isEnabled());

        feature.enable();
        assertTrue(feature.isEnabled());
    }

    @Test
    void testResetToDefaults() {
        CalloutFeature feature = new CalloutFeature();

        feature.teamChatConfig.set(false);
        feature.closeOnSendConfig.set(false);

        assertFalse(feature.teamChatConfig.get());
        assertFalse(feature.closeOnSendConfig.get());

        feature.resetToDefaults();

        assertTrue(feature.teamChatConfig.get());
        assertTrue(feature.closeOnSendConfig.get());
    }

    @Test
    void testFormatMessage() {
        CalloutFeature feature = new CalloutFeature();

        assertEquals("/t Need Silicon", feature.formatMessage("Need Silicon", true));
        assertEquals("/t Need Silicon", feature.formatMessage("/t Need Silicon", true));
        assertEquals("/t Attack!", feature.formatMessage("  Attack!  ", true));

        assertEquals("Need Silicon", feature.formatMessage("Need Silicon", false));
        assertEquals("Attack!", feature.formatMessage("  Attack!  ", false));
        assertEquals("/t Defend core", feature.formatMessage("/t Defend core", false));

        assertEquals("", feature.formatMessage(null, true));
        assertEquals("", feature.formatMessage(null, false));
        assertEquals("", feature.formatMessage("   ", true));
        assertEquals("", feature.formatMessage("   ", false));
    }

    @Test
    void testSendCalloutSafelyHandlesNullAndEmpty() {
        CalloutFeature feature = new CalloutFeature();

        assertDoesNotThrow(() -> feature.sendCallout(null));
        assertDoesNotThrow(() -> feature.sendCallout(""));
        assertDoesNotThrow(() -> feature.sendCallout("   "));
    }
}
