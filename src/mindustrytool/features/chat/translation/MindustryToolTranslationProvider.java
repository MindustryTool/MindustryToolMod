package mindustrytool.features.chat.translation;

import arc.Core;
import arc.util.Http;
import arc.util.Http.HttpStatusException;
import mindustry.Vars;
import mindustry.ui.dialogs.LanguageDialog;
import mindustrytool.Config;
import mindustrytool.Utils;
import arc.scene.ui.layout.Table;
import arc.scene.ui.Slider;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class MindustryToolTranslationProvider implements TranslationProvider {

    private int getTimeout() {
        return Core.settings.getInt(ChatTranslationConfig.MINDUSTRYTOOL_TIMEOUT, 120);
    }

    private void setTimeout(int timeout) {
        Core.settings.put(ChatTranslationConfig.MINDUSTRYTOOL_TIMEOUT, timeout);
    }

    @Override
    public CompletableFuture<String> translate(String message) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (message == null || message.isEmpty()) {
            future.complete("");
            return future;
        }

        if (message.length() > 1028) {
            message = message.substring(0, 1028);
        }

        if (Vars.ui.language.getLocale() == null) {
            future.completeExceptionally(new IllegalArgumentException("Invalid locale: null"));
            return future;
        }

        String locale = LanguageDialog.getDisplayName(Vars.ui.language.getLocale());

        if (locale.isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("Invalid locale: " + locale));
            return future;
        }

        if (locale.length() > 16) {
            locale = locale.substring(0, 16);
        }

        try {
            HashMap<String, Object> body = new HashMap<>();

            body.put("content", message);
            body.put("target", locale);

            Http.post(Config.API_v4_URL + "translations/translate", Utils.toJson(body))
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
                        future.complete(res.getResultAsString());
                    });

        } catch (Exception e) {
            future.completeExceptionally(new RuntimeException("Translation error", e));
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

        Slider slider = new Slider(30, 120, 1, false);
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
}
