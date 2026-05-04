package mindustrytool.features.chat.translation;

import arc.Core;
import arc.func.Cons;
import arc.scene.ui.Dialog;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.core.NetClient;
import mindustry.gen.SendMessageCallPacket;
import mindustry.gen.SendMessageCallPacket2;
import mindustrytool.Main;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import arc.struct.Seq;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ChatTranslationFeature implements Feature {
    private final Seq<TranslationProvider> providers = new Seq<>();
    private final TranslationProvider defaultTranslationProvider = new MindustryToolTranslationProvider();
    private String lastError = null;
    private TranslationProvider currentProvider = defaultTranslationProvider;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("chat-translation")
                .description("chat-translation.description")
                .icon(mindustry.gen.Icon.chat)
                .build();
    }

    public class SendTranslatedMessageCallPacket extends SendMessageCallPacket {

        @Override
        public void handleClient() {
            handleMessage(this.message, translated -> {
                NetClient.sendMessage(translated);
            });
        }
    }

    public class SendTranslatedMessageCallPacket2 extends SendMessageCallPacket2 {
        @Override
        public void handleClient() {
            if (Vars.player != this.playersender) {
                handleMessage(this.message, translated -> {
                    NetClient.sendMessage(translated, this.unformatted,
                            this.playersender);
                });
            } else {
                NetClient.sendMessage(this.message, this.unformatted,
                        this.playersender);
            }
        }
    }

    @Override
    public void init() {
        Main.registerPacketPlacement(SendMessageCallPacket.class, SendTranslatedMessageCallPacket::new);
        Main.registerPacketPlacement(SendMessageCallPacket2.class, SendTranslatedMessageCallPacket2::new);

        providers.add(defaultTranslationProvider);
        providers.add(new GeminiTranslationProvider());
        providers.add(new DeepLTranslationProvider());

        providers.each(TranslationProvider::init);

        loadProvider();

    }

    public void handleMessage(String message, Cons<String> cons) {
        if (!isEnabled()) {
            cons.get(message);
            return;
        }

        translateContent(message)
                .thenAccept(translated -> cons.get(Strings.format("@ [gold](@)[white]", message, translated)))
                .exceptionally(e -> {
                    lastError = e.getMessage();

                    String formated = Strings.format("@\n\n[scarlet]@[white]\n\n", message,
                            Core.bundle.get("chat-translation.error.prefix") + e.getMessage());

                    cons.get(formated);

                    Log.err(e.getMessage());
                    return null;
                });

    }

    public CompletableFuture<String> translateContent(String message) {
        if (!isEnabled()) {
            throw new IllegalArgumentException("ChatTranslationFeature is not enabled");
        }

        return currentProvider.translate(Strings.stripColors(message))
                .whenComplete((translated, error) -> {
                    if (error != null) {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        lastError = cause.getMessage();
                    }
                });
    }

    private void loadProvider() {
        String id = ChatTranslationConfig.getProviderId();
        currentProvider = providers.find(p -> p.getId().equals(id));

        if (currentProvider == null) {
            currentProvider = defaultTranslationProvider;
        }
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.of(new ChatTranslationSettingsDialog(this));
    }

    Seq<TranslationProvider> getProviders() {
        return providers;
    }

    TranslationProvider getCurrentProvider() {
        return currentProvider;
    }

    void setCurrentProvider(TranslationProvider currentProvider) {
        this.currentProvider = currentProvider;
    }

    String getLastError() {
        return lastError;
    }
}
