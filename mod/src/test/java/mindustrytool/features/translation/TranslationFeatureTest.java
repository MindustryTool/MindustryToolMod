package mindustrytool.features.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import mindustrytool.features.translation.providers.GeminiTranslationProvider;
import mindustrytool.test.MindustryTestEnv;

class TranslationFeatureTest extends MindustryTestEnv {

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
    void testTranslationNetworkOnly() {
        TranslationFeature feature = new TranslationFeature();
        MockTranslationProvider mock = new MockTranslationProvider("mock-provider");
        feature.getProviders().add(mock);
        feature.providerConfig.set("mock-provider");

        // Network-only by contract: every invocation hits the provider, nothing retained
        CompletableFuture<String> f1 = feature.translate("Hello", "Vietnamese");
        CompletableFuture<String> f2 = feature.translate("Hello", "Vietnamese");

        assertEquals(2, mock.callCount.get(), "Provider should be invoked on every call without caching");
        assertEquals("[Vietnamese]: Hello", f1.join());
        assertEquals("[Vietnamese]: Hello", f2.join());

        CompletableFuture<String> f3 = feature.translate("Goodbye", "Vietnamese");
        assertEquals(3, mock.callCount.get());
        assertEquals("[Vietnamese]: Goodbye", f3.join());

        CompletableFuture<String> f4 = feature.translate("Hello", "French");
        assertEquals(4, mock.callCount.get());
        assertEquals("[French]: Hello", f4.join());

        flushEffects();
    }

    @Test
    void testConcurrentTranslationsAreIndependent() {
        TranslationFeature feature = new TranslationFeature();
        MockTranslationProvider mock = new MockTranslationProvider("mock-provider");
        feature.getProviders().add(mock);
        feature.providerConfig.set("mock-provider");

        CompletableFuture<String> delayed = new CompletableFuture<>();
        mock.nextFuture = delayed;

        CompletableFuture<String> f1 = feature.translate("Concurrent", "German");
        CompletableFuture<String> f2 = feature.translate("Concurrent", "German");

        assertEquals(2, mock.callCount.get(), "Concurrent requests each invoke the provider without joining");
        delayed.complete("[German]: Concurrent");

        assertEquals("[German]: Concurrent", f1.join());
        assertEquals("[German]: Concurrent", f2.join());
    }

    @Test
    void testOutgoingTranslationErrorSetsLastError() {
        TranslationFeature feature = new TranslationFeature();
        MockTranslationProvider mock = new MockTranslationProvider("mock-err");
        mock.errorToThrow = new RuntimeException("API key invalid");
        feature.getProviders().add(mock);
        feature.providerConfig.set("mock-err");

        java.util.concurrent.atomic.AtomicReference<String> delivered = new java.util.concurrent.atomic.AtomicReference<>();
        feature.handleOutgoingMessage("Hello world", delivered::set);

        assertEquals("Hello world", delivered.get(), "Original message must never be dropped on failure");
        assertEquals("API key invalid", feature.lastError.get(), "lastError signal must be set on failure");
    }

    @Test
    void testUnconfiguredProviderSetsLastError() {
        TranslationFeature feature = new TranslationFeature();
        MockTranslationProvider mock = new MockTranslationProvider("mock-unconfigured");
        mock.configured = false;
        feature.getProviders().add(mock);
        feature.providerConfig.set("mock-unconfigured");

        java.util.concurrent.atomic.AtomicReference<String> delivered = new java.util.concurrent.atomic.AtomicReference<>();
        feature.handleOutgoingMessage("Test unconfigured", delivered::set);

        assertEquals("Test unconfigured", delivered.get());
        assertTrue(feature.lastError.get() != null && !feature.lastError.get().isEmpty());
    }

    @Test
    void testIncomingTranslationErrorSetsLastError() {
        TranslationFeature feature = new TranslationFeature();
        feature.setEnabled(true);
        MockTranslationProvider mock = new MockTranslationProvider("mock-incoming-err");
        mock.errorToThrow = new RuntimeException("Rate limit 429");
        feature.getProviders().add(mock);
        feature.providerConfig.set("mock-incoming-err");

        java.util.concurrent.atomic.AtomicReference<String> delivered = new java.util.concurrent.atomic.AtomicReference<>();
        feature.handleIncomingMessage("Chat test", delivered::set);

        assertEquals("Rate limit 429", feature.lastError.get());
    }

    static class MockTranslationProvider implements TranslationProvider {
        private final String id;
        final AtomicInteger callCount = new AtomicInteger(0);
        CompletableFuture<String> nextFuture = null;
        Throwable errorToThrow = null;
        boolean configured = true;

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
            return configured;
        }

        @Override
        public CompletableFuture<String> translate(String text, String targetLanguage) {
            callCount.incrementAndGet();
            if (errorToThrow != null) {
                CompletableFuture<String> failed = new CompletableFuture<>();
                failed.completeExceptionally(errorToThrow);
                return failed;
            }
            if (nextFuture != null) {
                return nextFuture;
            }
            return CompletableFuture.completedFuture("[" + targetLanguage + "]: " + text);
        }
    }
}
