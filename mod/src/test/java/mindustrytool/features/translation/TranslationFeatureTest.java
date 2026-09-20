package mindustrytool.features.translation;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import mindustrytool.features.translation.providers.GeminiTranslationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.QueryCache;

class TranslationFeatureTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        QueryCache.getInstance().clear();
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

    @Test
    void testTranslationCachingAndDeduplication() {
        TranslationFeature feature = new TranslationFeature();
        MockTranslationProvider mock = new MockTranslationProvider("mock-provider");
        feature.getProviders().add(mock);
        feature.providerConfig.set("mock-provider");

        CompletableFuture<String> f1 = feature.translate("Hello", "Vietnamese");
        CompletableFuture<String> f2 = feature.translate("Hello", "Vietnamese");

        assertEquals(1, mock.callCount.get(), "Provider should only be invoked once for cached query");
        assertEquals("[Vietnamese]: Hello", f1.join());
        assertEquals("[Vietnamese]: Hello", f2.join());

        // Different text triggers new call
        CompletableFuture<String> f3 = feature.translate("Goodbye", "Vietnamese");
        assertEquals(2, mock.callCount.get());
        assertEquals("[Vietnamese]: Goodbye", f3.join());

        // Different target language triggers new call
        CompletableFuture<String> f4 = feature.translate("Hello", "French");
        assertEquals(3, mock.callCount.get());
        assertEquals("[French]: Hello", f4.join());
    }

    @Test
    void testConcurrentTranslationDeduplication() {
        TranslationFeature feature = new TranslationFeature();
        MockTranslationProvider mock = new MockTranslationProvider("mock-provider");
        feature.getProviders().add(mock);
        feature.providerConfig.set("mock-provider");

        CompletableFuture<String> delayed = new CompletableFuture<>();
        mock.nextFuture = delayed;

        CompletableFuture<String> f1 = feature.translate("Concurrent", "German");
        CompletableFuture<String> f2 = feature.translate("Concurrent", "German");

        assertEquals(1, mock.callCount.get(), "Concurrent requests should be joined to 1 invocation");
        delayed.complete("[German]: Concurrent");

        assertEquals("[German]: Concurrent", f1.join());
        assertEquals("[German]: Concurrent", f2.join());
    }

    static class MockTranslationProvider implements TranslationProvider {
        private final String id;
        final AtomicInteger callCount = new AtomicInteger(0);
        CompletableFuture<String> nextFuture = null;

        MockTranslationProvider(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return id;
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public CompletableFuture<String> translate(String text, String targetLanguage) {
            callCount.incrementAndGet();
            if (nextFuture != null) {
                return nextFuture;
            }
            return CompletableFuture.completedFuture("[" + targetLanguage + "]: " + text);
        }
    }
}
