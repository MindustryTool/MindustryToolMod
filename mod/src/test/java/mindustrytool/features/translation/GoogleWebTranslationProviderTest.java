package mindustrytool.features.translation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import mindustrytool.features.translation.providers.GoogleWebTranslationProvider;
import mindustrytool.test.MindustryTestEnv;

class GoogleWebTranslationProviderTest extends MindustryTestEnv {

    @Test
    void testBasicProperties() {
        GoogleWebTranslationProvider provider = new GoogleWebTranslationProvider();

        assertEquals(GoogleWebTranslationProvider.ID, provider.getId());
        assertNotNull(provider.getName());
        assertTrue(provider.isConfigured());
    }

    @Test
    void testResolveGoogleLang() {
        assertEquals("en", GoogleWebTranslationProvider.resolveGoogleLang("en"));
        assertEquals("vi", GoogleWebTranslationProvider.resolveGoogleLang("VI"));
        assertEquals("vi", GoogleWebTranslationProvider.resolveGoogleLang("Vietnamese"));
        assertEquals("es", GoogleWebTranslationProvider.resolveGoogleLang("Spanish"));
        assertEquals("de", GoogleWebTranslationProvider.resolveGoogleLang("German"));
        assertEquals("zh-CN", GoogleWebTranslationProvider.resolveGoogleLang("Chinese"));
        assertEquals("ja", GoogleWebTranslationProvider.resolveGoogleLang("Japanese"));
        assertEquals("ru", GoogleWebTranslationProvider.resolveGoogleLang("Russian"));
        assertEquals("fr", GoogleWebTranslationProvider.resolveGoogleLang("French"));
    }

    @Test
    void testParseGoogleResponseSinglePart() {
        String json = "[[[\"Hello world\",\"Xin chao the gioi\",null,null,1]],null,\"vi\"]";
        String parsed = GoogleWebTranslationProvider.parseGoogleResponse(200, json, "fallback");
        assertEquals("Hello world", parsed);
    }

    @Test
    void testParseGoogleResponseMultiPart() {
        String json = "[[[\"Hello \",\"Xin \",null,null,1],[\"world\",\"chao\",null,null,1]],null,\"vi\"]";
        String parsed = GoogleWebTranslationProvider.parseGoogleResponse(200, json, "fallback");
        assertEquals("Hello world", parsed);
    }

    @Test
    void testParseGoogleResponseMalformedFallback() {
        assertEquals("fallback", GoogleWebTranslationProvider.parseGoogleResponse(200, "", "fallback"));
        assertEquals("fallback", GoogleWebTranslationProvider.parseGoogleResponse(200, null, "fallback"));
        assertEquals("fallback", GoogleWebTranslationProvider.parseGoogleResponse(200, "invalid-json", "fallback"));
        assertEquals("fallback", GoogleWebTranslationProvider.parseGoogleResponse(200, "[]", "fallback"));
    }

    @Test
    void testMapGoogleError() {
        RuntimeException e403 = GoogleWebTranslationProvider.mapGoogleError(403);
        assertNotNull(e403.getMessage());

        RuntimeException e429 = GoogleWebTranslationProvider.mapGoogleError(429);
        assertNotNull(e429.getMessage());

        RuntimeException e500 = GoogleWebTranslationProvider.mapGoogleError(500);
        assertNotNull(e500.getMessage());

        assertThrows(RuntimeException.class, () ->
                GoogleWebTranslationProvider.parseGoogleResponse(429, "{}", "fallback"));
    }

    @Test
    void testEmptyTextReturnsImmediately() {
        GoogleWebTranslationProvider provider = new GoogleWebTranslationProvider();
        CompletableFuture<String> empty = provider.translate("", "English");
        assertTrue(empty.isDone());
        assertEquals("", empty.join());

        CompletableFuture<String> whitespace = provider.translate("   ", "English");
        assertTrue(whitespace.isDone());
        assertEquals("", whitespace.join());
    }

    @Test
    void testCachingBehavior() {
        GoogleWebTranslationProvider provider = new GoogleWebTranslationProvider();
        provider.putCache("Hello", "Vietnamese", "Xin chao");

        assertEquals("Xin chao", provider.getCached("Hello", "Vietnamese"));

        CompletableFuture<String> cachedFuture = provider.translate("Hello", "Vietnamese");
        assertTrue(cachedFuture.isDone());
        assertEquals("Xin chao", cachedFuture.join());
    }

    @Test
    void testCacheEvictionBound() {
        GoogleWebTranslationProvider provider = new GoogleWebTranslationProvider();
        for (int i = 0; i < 250; i++) {
            provider.putCache("Text" + i, "English", "Translated" + i);
        }
        assertTrue(provider.getCacheSize() <= 200, "Cache should not exceed MAX_CACHE_ENTRIES");
    }

    @Test
    void testProviderRegistrationInFeature() {
        TranslationFeature feature = new TranslationFeature();
        boolean hasGoogle = false;
        for (TranslationProvider p : feature.getProviders()) {
            if (GoogleWebTranslationProvider.ID.equals(p.getId())) {
                hasGoogle = true;
                break;
            }
        }
        assertTrue(hasGoogle, "GoogleWebTranslationProvider should be registered in TranslationFeature");
    }
}
