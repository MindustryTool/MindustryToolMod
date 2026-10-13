package mindustrytool.features.translation;

import java.util.concurrent.CompletableFuture;

public interface TranslationProvider {

    String getId();

    String getName();

    CompletableFuture<String> translate(String text, String targetLanguage);

    boolean isConfigured();

    default void init() {
    }
}
