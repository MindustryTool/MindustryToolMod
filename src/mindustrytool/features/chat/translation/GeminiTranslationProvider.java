package mindustrytool.features.chat.translation;

import arc.Core;
import arc.util.Http;
import arc.util.Http.HttpStatusException;
import arc.util.serialization.Jval;
import arc.scene.ui.layout.Table;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Slider;
import mindustry.ui.Styles;
import java.util.concurrent.CompletableFuture;

public class GeminiTranslationProvider implements TranslationProvider {
    private static final String API_URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String[] MODELS = {
            "gemini-2.0-flash-lite-preview-02-05",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b",
            "gemini-1.5-pro",
    };

    private String getApiKey() {
        return Core.settings.getString(ChatTranslationConfig.GEMINI_API_KEY, "");
    }

    private void setApiKey(String key) {
        Core.settings.put(ChatTranslationConfig.GEMINI_API_KEY, key);
    }

    private String getModel() {
        return Core.settings.getString(ChatTranslationConfig.GEMINI_MODEL, MODELS[0]);
    }

    private void setModel(String model) {
        Core.settings.put(ChatTranslationConfig.GEMINI_MODEL, model);
    }

    private int getTimeout() {
        return Core.settings.getInt(ChatTranslationConfig.GEMINI_TIMEOUT, 10);
    }

    private void setTimeout(int timeout) {
        Core.settings.put(ChatTranslationConfig.GEMINI_TIMEOUT, timeout);
    }

    @Override
    public void init() {
    }

    @Override
    public CompletableFuture<String> translate(String message) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (getApiKey().isEmpty()) {
            future.complete(Core.bundle.get("chat-translation.gemini.no-api-key"));
            return future;
        }

        try {
            Jval body = Jval.newObject();
            Jval contents = Jval.newArray();
            Jval content = Jval.newObject();
            Jval parts = Jval.newArray();
            Jval part = Jval.newObject();

            String prompt = "Translate the following Mindustry game chat message to "
                    + Core.bundle.getLocale().getDisplayName()
                    + ". If it is already" + Core.bundle.getLocale().getDisplayName()
                    + ", just return it as is. Message: "
                    + message;

            part.put("text", prompt);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            body.put("contents", contents);

            Http.post(API_URL_BASE + getModel() + ":generateContent", body.toString())
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", getApiKey())
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
                            // Response structure: candidates[0].content.parts[0].text
                            if (json.has("candidates") && !json.get("candidates").asArray().isEmpty()) {
                                String result = json.get("candidates").asArray().get(0).asObject()
                                        .get("content").asObject()
                                        .get("parts").asArray().get(0)
                                        .getString("text", message).trim();
                                future.complete(result);
                            } else {
                                future.complete(message);
                            }
                        } catch (Exception e) {
                            future.completeExceptionally(
                                    new RuntimeException(Core.bundle.get("chat-translation.gemini.parse-error"), e));
                        }
                    });
        } catch (Exception e) {
            future.completeExceptionally(new RuntimeException("Gemini translation error", e));
        }

        return future;
    }

    @Override
    public Table settings() {
        Table table = new Table();
        table.add(Core.bundle.get("chat-translation.gemini.api-key-label")).left().row();
        table.label(() -> Core.bundle.get("chat-translation.gemini.get-key-info"))
                .style(mindustry.ui.Styles.outlineLabel)
                .fontScale(0.8f)
                .color(arc.graphics.Color.gray)
                .left()
                .row();

        table.field(getApiKey(), this::setApiKey).growX().valid(v -> !v.isEmpty() && v.startsWith("AIza")).row();

        table.add(Core.bundle.get("chat-translation.gemini.model-label")).left().padTop(10).row();

        Table modelTable = new Table();
        ButtonGroup<CheckBox> group = new ButtonGroup<>();
        for (String m : MODELS) {
            CheckBox box = new CheckBox(m);
            box.setStyle(Styles.defaultCheck);
            box.changed(() -> {
                if (box.isChecked()) {
                    setModel(m);
                }
            });
            if (getModel().equals(m)) {
                box.setChecked(true);
            }
            group.add(box);
            modelTable.add(box).left().padRight(10).row();
        }
        table.add(modelTable).left().row();

        table.add(Core.bundle.get("chat-translation.gemini.timeout-label") + ": " + getTimeout() + "s").left()
                .padTop(10)
                .update(l -> l
                        .setText(Core.bundle.get("chat-translation.gemini.timeout-label") + ": " + getTimeout() + "s"))
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
        return Core.bundle.get("chat-translation.provider.gemini");
    }

    @Override
    public String getId() {
        return "gemini";
    }
}
