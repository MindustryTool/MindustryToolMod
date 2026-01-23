package mindustrytool.features.chat.translation;

import arc.Core;
import arc.scene.ui.layout.Table;

import java.util.concurrent.CompletableFuture;

public class NoopTranslationProvider implements TranslationProvider {
    @Override
    public CompletableFuture<String> translate(String message) {
        return CompletableFuture.completedFuture(message);
    }

    @Override
    public Table settings() {
        return new Table();
    }

    @Override
    public void init() {
    }

    @Override
    public String getName() {
        return Core.bundle.get("chat-translation.provider.none");
    }

    @Override
    public String getId() {
        return "noop";
    }
}
