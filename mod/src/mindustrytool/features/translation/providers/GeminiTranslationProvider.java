package mindustrytool.features.translation.providers;

import arc.Core;
import arc.struct.Seq;
import arc.util.serialization.Jval;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.features.translation.TranslationProvider;
import mindustrytool.services.Request;

/**
 * Translation provider using Google Gemini Generative Language API.
 */
public class GeminiTranslationProvider implements TranslationProvider {

	public static final String ID = "gemini";
	public static final String[] MODELS = {
		"gemini-2.5-flash",
		"gemini-2.5-flash-lite",
		"gemini-2.5-pro",
		"gemini-2.0-flash",
		"gemini-2.0-flash-lite"
	};

	private static final String BASE_URL = "https://generativelanguage.googleapis.com";
	private final TranslationFeature feature;
	private final Seq<String> history = new Seq<>();

	public GeminiTranslationProvider(TranslationFeature feature) {
		this.feature = feature;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return Core.bundle.get("feature.translation.provider.gemini", "Google Gemini");
	}

	@Override
	public boolean isConfigured() {
		String key = feature.geminiApiKeyConfig.get();
		return key != null && !key.trim().isEmpty();
	}

	@Override
	public CompletableFuture<String> translate(String text, String targetLanguage) {
		if (text == null || text.trim().isEmpty()) {
			return CompletableFuture.completedFuture("");
		}

		String apiKey = feature.geminiApiKeyConfig.get();
		if (apiKey == null || apiKey.trim().isEmpty()) {
			CompletableFuture<String> failed = new CompletableFuture<>();
			failed.completeExceptionally(
					new IllegalStateException(Core.bundle.get("feature.translation.error.no-api-key")));
			return failed;
		}

		int timeout = feature.geminiTimeoutConfig.get();
		String model = feature.geminiModelConfig.get();
		if (model == null || model.trim().isEmpty()) {
			model = MODELS[0];
		}

		int maxHist = feature.geminiMaxHistoryConfig.get();
		String historySnippet = buildHistorySnippet(maxHist);

		String prompt = "Translate the following Mindustry game chat message into " + targetLanguage + ".\n"
				+ "If it is already in " + targetLanguage + ", return it unchanged.\n"
				+ "Do not add explanations, notes, or extra formatting. Return ONLY the translation.\n\n"
				+ (historySnippet.isEmpty() ? "" : "Recent chat context for disambiguation:\n" + historySnippet + "\n\n")
				+ "Message: " + text;

		Jval body = Jval.newObject();
		Jval contents = Jval.newArray();
		Jval content = Jval.newObject();
		Jval parts = Jval.newArray();
		Jval part = Jval.newObject();
		part.put("text", prompt);
		parts.add(part);
		content.put("parts", parts);
		contents.add(content);
		body.put("contents", contents);

		Request request = Request.builder()
				.baseUrl(BASE_URL)
				.timeout(Duration.ofSeconds(Math.max(3, timeout)))
				.build();

		return request.post("/v1beta/models/" + model + ":generateContent")
				.header("x-goog-api-key", apiKey.trim())
				.json(body.toString())
				.withoutAuth()
				.sendAsync()
				.thenApply(response -> {
					String result = parseGeminiResponse(response.statusCode(), response.body(), text);
					recordHistory(text, maxHist);
					return result;
				});
	}

	public static String parseGeminiResponse(int statusCode, String jsonResponse, String fallback) {
		if (statusCode == 401 || statusCode == 403) {
			throw new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.invalid-key", "Invalid API key")
					: "Invalid API key");
		}
		if (statusCode == 404) {
			throw new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.model-not-found", "Model not found")
					: "Model not found");
		}
		if (statusCode == 429) {
			throw new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.rate-limit", "Rate limit exceeded")
					: "Rate limit exceeded");
		}
		if (statusCode >= 500) {
			throw new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.server-error", "Server error")
					: "Server error");
		}
		if (statusCode != 200) {
			throw new RuntimeException((Core.bundle != null
					? Core.bundle.get("feature.translation.error.network", "Network error")
					: "Network error") + " (HTTP " + statusCode + ")");
		}

		if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
			return fallback;
		}

		try {
			Jval root = Jval.read(jsonResponse);
			if (root.has("candidates") && !root.get("candidates").asArray().isEmpty()) {
				Jval firstCandidate = root.get("candidates").asArray().get(0);
				if (firstCandidate.has("content")) {
					Jval contentObj = firstCandidate.get("content");
					if (contentObj.has("parts") && !contentObj.get("parts").asArray().isEmpty()) {
						String text = contentObj.get("parts").asArray().get(0).getString("text", fallback);
						return text != null ? text.trim() : fallback;
					}
				}
			}
			return fallback;
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
		}
	}

	private synchronized String buildHistorySnippet(int maxHist) {
		if (maxHist <= 0 || history.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		int start = Math.max(0, history.size - maxHist);
		for (int i = start; i < history.size; i++) {
			sb.append("- ").append(history.get(i)).append("\n");
		}
		return sb.toString().trim();
	}

	private synchronized void recordHistory(String text, int maxHist) {
		if (maxHist <= 0) {
			history.clear();
			return;
		}
		history.add(text);
		while (history.size > Math.max(10, maxHist)) {
			history.remove(0);
		}
	}
}
