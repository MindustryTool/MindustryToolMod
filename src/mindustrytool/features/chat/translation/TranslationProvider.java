package mindustrytool.features.chat.translation;

import arc.scene.ui.layout.Table;

import java.util.concurrent.CompletableFuture;

public interface TranslationProvider {
    CompletableFuture<String> translate(String message);

    Table settings();

    void init();

    String getName();

    String getId();
}
