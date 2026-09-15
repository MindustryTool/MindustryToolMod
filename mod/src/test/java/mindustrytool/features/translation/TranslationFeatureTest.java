package mindustrytool.features.translation;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import mindustrytool.features.translation.providers.GeminiTranslationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TranslationFeatureTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
    }

    @Test
    void testResetToDefaultsPreservesApiKeys() {
        TranslationFeature feature = new TranslationFeature();

        feature.geminiApiKeyConfig.set("secret-key");
        feature.providerConfig.set("custom");
        feature.showOriginalConfig.set(false);
        feature.outgoingTargetLangConfig.set("Spanish");
        feature.geminiTimeoutConfig.set(99);

        assertEquals("secret-key", feature.geminiApiKeyConfig.get());
        assertEquals("Spanish", feature.outgoingTargetLangConfig.get());

        feature.resetToDefaults();

        assertEquals(GeminiTranslationProvider.ID, feature.providerConfig.get());
        assertTrue(feature.showOriginalConfig.get());
        assertEquals("English", feature.outgoingTargetLangConfig.get());
        assertEquals(10, feature.geminiTimeoutConfig.get());
        assertEquals("secret-key", feature.geminiApiKeyConfig.get());
    }
}
