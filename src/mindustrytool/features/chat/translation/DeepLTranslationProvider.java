package mindustrytool.features.chat.translation;

import arc.Core;
import arc.util.Http;
import arc.util.Http.HttpStatusException;
import arc.util.serialization.Jval;
import arc.scene.ui.layout.Table;
import arc.scene.ui.Slider;
import java.util.concurrent.CompletableFuture;

public class DeepLTranslationProvider implements TranslationProvider {
    private static final String API_URL_FREE = "https://api-free.deepl.com/v2/translate";
    private static final String API_URL_PRO = "https://api.deepl.com/v2/translate";

    private String getApiKey() {
        return Core.settings.getString(ChatTranslationConfig.DEEPL_API_KEY, "");
    }

    private void setApiKey(String key) {
        Core.settings.put(ChatTranslationConfig.DEEPL_API_KEY, key);
    }

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

        String apiKey = getApiKey();

        if (apiKey.isEmpty()) {
            future.completeExceptionally(
                    new IllegalArgumentException(Core.bundle.get("chat-translation.deepl.no-api-key")));
            return future;
        }

        String apiUrl = apiKey.endsWith(":fx") ? API_URL_FREE : API_URL_PRO;
        String targetLang = Core.bundle.getLocale().getLanguage().toUpperCase();

        try {
            Jval body = Jval.newObject();
            Jval textArray = Jval.newArray();
            textArray.add(message);
            body.put("text", textArray);
            body.put("target_lang", targetLang);

            Http.post(apiUrl, body.toString())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "DeepL-Auth-Key " + apiKey)
                    .timeout(getTimeout() * 1000)
                    .error(e -> {
                        if (e instanceof HttpStatusException httpStatusException) {
                            future.completeExceptionally(new RuntimeException(
                                    Core.bundle.get("chat-translation.error.prefix")
                                            + httpStatusException.response.getResultAsString()));
                            return;
                        }
                        future.completeExceptionally(
                                new RuntimeException(
                                        Core.bundle.get("chat-translation.error.prefix") + e.getMessage()));
                    })
                    .submit(res -> {
                        String jsonString = res.getResultAsString();
                        try {
                            Jval json = Jval.read(jsonString);
                            if (json.has("translations") && !json.get("translations").asArray().isEmpty()) {
                                String result = json.get("translations").asArray().get(0)
                                        .getString("text", message).trim();
                                future.complete(result);
                            } else {
                                future.complete(message);
                            }
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
        table.add(Core.bundle.get("chat-translation.deepl.api-key-label")).left().row();
        table.label(() -> Core.bundle.get("chat-translation.deepl.get-key-info"))
                .style(mindustry.ui.Styles.outlineLabel)
                .fontScale(0.8f)
                .color(arc.graphics.Color.gray)
                .left()
                .row();

        table.field(getApiKey(), this::setApiKey).valid(text -> text.length() > 0).growX().row();

        table.add(Core.bundle.get("chat-translation.deepl.timeout-label") + ": " + getTimeout() + "s").left()
                .padTop(10)
                .update(l -> l
                        .setText(Core.bundle.get("chat-translation.deepl.timeout-label") + ": " + getTimeout() + "s"))
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
        return Core.bundle.get("chat-translation.provider.deepl");
    }

    @Override
    public String getId() {
        return "deepl";
    }
}
