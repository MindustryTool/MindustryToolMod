package mindustrytool.features.chat.translation;

import arc.Core;
import arc.util.Http;
import arc.util.Http.HttpStatusException;
import arc.util.serialization.Jval;
import arc.scene.ui.layout.Table;
import java.util.concurrent.CompletableFuture;

public class GeminiTranslationProvider implements TranslationProvider {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";
    private String apiKey = "";

    @Override
    public void init() {
        apiKey = Core.settings.getString(ChatTranslationConfig.GEMINI_API_KEY, "");
    }

    @Override
    public CompletableFuture<String> translate(String message) {
        CompletableFuture<String> future = new CompletableFuture<>();

        if (apiKey.isEmpty()) {
            future.complete("[red]No API Key[]");
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

            Http.post(API_URL, body.toString())
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .timeout(10000)
                    .error(e -> {
                        if (e instanceof HttpStatusException httpStatusException) {
                            future.completeExceptionally(new RuntimeException(
                                    "[red]Translation Error[]: " + httpStatusException.response.getResultAsString()));
                            return;
                        }
                        future.completeExceptionally(
                                new RuntimeException("[red]Translation Error[]: " + e.getMessage()));
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
                            future.completeExceptionally(new RuntimeException("[red]Parse Error[]", e));
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
        table.add("Gemini API Key:").left().row();
        table.label(() -> "Get key from Google AI Studio")
                .style(mindustry.ui.Styles.outlineLabel)
                .fontScale(0.8f)
                .color(arc.graphics.Color.gray)
                .left()
                .row();

        table.field(apiKey, val -> {
            apiKey = val;
            Core.settings.put(ChatTranslationConfig.GEMINI_API_KEY, val);
        }).growX().valid(v -> !v.isEmpty() && v.startsWith("AIza")).row();

        return table;
    }

    @Override
    public String getName() {
        return "Gemini AI";
    }

    @Override
    public String getId() {
        return "gemini";
    }
}
