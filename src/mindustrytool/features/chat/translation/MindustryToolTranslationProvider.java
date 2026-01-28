package mindustrytool.features.chat.translation;

import arc.Core;
import arc.util.Http;
import arc.util.Http.HttpStatusException;
import lombok.Data;
import mindustrytool.Config;
import mindustrytool.Utils;
import arc.scene.ui.layout.Table;
import arc.scene.ui.Slider;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class MindustryToolTranslationProvider implements TranslationProvider {

    private int getTimeout() {
        return Core.settings.getInt(ChatTranslationConfig.DEEPL_TIMEOUT, 10);
    }

    private void setTimeout(int timeout) {
        Core.settings.put(ChatTranslationConfig.DEEPL_TIMEOUT, timeout);
    }

    @Override
    public void init() {
    }

    @Override
    public CompletableFuture<String> translate(String message) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String locale = Core.bundle.getLocale().getLanguage();

        if (locale == null || locale.isEmpty()) {
            future.completeExceptionally(
                    new IllegalArgumentException("Invalid locale: " + Core.bundle.getLocale().getDisplayName()));
            return future;
        }

        try {
            HashMap<String, Object> body = new HashMap<>();

            body.put("q", message);
            body.put("source", "auto");
            body.put("target", locale);

            Http.post(Config.API_v4_URL + "/libre", Utils.toJson(body))
                    .header("Content-Type", "application/json")
                    .timeout(getTimeout() * 1000)
                    .error(e -> {
                        if (e instanceof HttpStatusException httpStatusException) {
                            future.completeExceptionally(new RuntimeException(
                                    Core.bundle.get("chat-translation.error.prefix")
                                            + httpStatusException.response.getResultAsString()));
                            return;
                        }
                        future.completeExceptionally(new RuntimeException(
                                Core.bundle.get("chat-translation.error.prefix") + e.getMessage()));
                    })
                    .submit(res -> {
                        String jsonString = res.getResultAsString();
                        try {
                            Response response = Utils.fromJson(Response.class, jsonString);
                            future.complete(response.getTranslatedText());
                        } catch (Exception e) {
                            future.completeExceptionally(
                                    new RuntimeException(Core.bundle.get("chat-translation.deepl.parse-error"), e));
                        }
                    });

        } catch (Exception e) {
            future.completeExceptionally(new RuntimeException("DeepL translation error", e));
        }

        return future;
    }

    @Override
    public Table settings() {
        Table table = new Table();

        table.add(Core.bundle.get("chat-translation.timeout-label") + ": " + getTimeout() + "s").left()
                .padTop(10)
                .update(l -> l
                        .setText(Core.bundle.get("chat-translation.timeout-label") + ": " + getTimeout() + "s"))
                .row();

        Slider slider = new Slider(2, 20, 1, false);
        slider.setValue(getTimeout());
        slider.moved(val -> {
            setTimeout((int) val);
        });
        table.add(slider).growX().row();

        return table;
    }

    @Override
    public String getName() {
        return Core.bundle.get("chat-translation.provider.mindustry-tool");
    }

    @Override
    public String getId() {
        return "mindustrytool";
    }

    @Data
    private static class Response {
        private String translatedText;
    }
}
