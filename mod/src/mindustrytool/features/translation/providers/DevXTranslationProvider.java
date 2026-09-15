package mindustrytool.features.translation.providers;

import arc.Core;
import arc.struct.Seq;
import arc.util.serialization.Jval;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustrytool.features.translation.DevXSecrets;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.features.translation.TranslationProvider;
import mindustrytool.services.Request;

public class DevXTranslationProvider implements TranslationProvider {

	public static final String ID = "devx";
	private static final String API_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
	public static final String MODEL = "nvidia/riva-translate-4b-instruct-v2";
	public static final String DEFAULT_API_KEY = "API";
	private final TranslationFeature feature;
	private final Seq<String> history = new Seq<>();

	public DevXTranslationProvider(TranslationFeature feature) {
		this.feature = feature;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return Core.bundle != null
				? Core.bundle.get("feature.translation.provider.devx", "DevX")
				: "DevX";
	}

	@Override
	public boolean isConfigured() {
		String key = getApiKey();
		return key != null && !key.trim().isEmpty();
	}

	public String getApiKey() {
		if (DevXSecrets.hasBuiltInKey()) {
			String builtIn = DevXSecrets.getApiKey();
			if (builtIn != null && !builtIn.trim().isEmpty()) {
				return builtIn.trim();
			}
		}
		String key = feature.devxApiKeyConfig.get();
		if (key != null && !key.trim().isEmpty() && !DEFAULT_API_KEY.equalsIgnoreCase(key.trim())) {
			return key.trim();
		}
		String legacy = Core.settings != null ? Core.settings.getString("devx.apiKey", null) : null;
		if (legacy != null && !legacy.trim().isEmpty() && !DEFAULT_API_KEY.equalsIgnoreCase(legacy.trim())) {
			return legacy.trim();
		}
		return "";
	}

	private String normalizeLanguage(String lang) {
		if (lang == null || lang.trim().isEmpty()) {
			return "English";
		}
		String clean = lang.trim().toLowerCase();
		return switch (clean) {
			case "vi", "vn", "vietnamese", "tiếng việt" -> "Vietnamese";
			case "en", "us", "uk", "english", "tiếng anh" -> "English";
			case "ru", "russian", "tiếng nga" -> "Russian";
			case "zh", "cn", "chinese", "tiếng trung" -> "Chinese";
			case "ja", "japanese", "tiếng nhật" -> "Japanese";
			case "ko", "korean", "tiếng hàn" -> "Korean";
			case "fr", "french", "tiếng pháp" -> "French";
			case "de", "german", "tiếng đức" -> "German";
			case "es", "spanish", "tiếng tây ban nha" -> "Spanish";
			default -> {
				for (Locale loc : Vars.locales) {
					if (clean.equalsIgnoreCase(loc.toString()) || clean.equalsIgnoreCase(loc.getLanguage())) {
						yield loc.getDisplayLanguage(Locale.ENGLISH);
					}
				}
				yield lang.trim();
			}
		};
	}

	@Override
	public CompletableFuture<String> translate(String text, String targetLanguage) {
		if (text == null || text.trim().isEmpty()) {
			return CompletableFuture.completedFuture("");
		}

		String apiKey = getApiKey();
		if (apiKey.isEmpty()) {
			CompletableFuture<String> failed = new CompletableFuture<>();
			failed.completeExceptionally(
					new IllegalStateException(Core.bundle != null
							? Core.bundle.get("feature.translation.error.no-api-key", "API key is missing")
							: "API key is missing"));
			return failed;
		}

		int timeout = Math.max(feature.devxTimeoutConfig.get(), 25);
		String model = feature.devxModelConfig.get();
		if (model == null || model.trim().isEmpty()) {
			model = MODEL;
		}

		String resolvedTarget = normalizeLanguage(targetLanguage);
		int maxHist = feature.devxMaxHistoryConfig.get();
		String historySnippet = buildHistorySnippet(maxHist);

		String prompt = "Translate to " + resolvedTarget + ": " + text;
		if (!historySnippet.isEmpty()) {
			prompt = "Context:\n" + historySnippet + "\n\n" + prompt;
		}

		Jval body = Jval.newObject();
		body.put("model", model);
		body.put("temperature", 0.1);
		body.put("max_tokens", 256);

		Jval messages = Jval.newArray();
		Jval userMsg = Jval.newObject();
		userMsg.put("role", "user");
		userMsg.put("content", prompt);
		messages.add(userMsg);

		body.put("messages", messages);

		Request request = Request.builder()
				.baseUrl(API_URL)
				.timeout(Duration.ofSeconds(timeout))
				.build();

		return request.post(API_URL)
				.header("Authorization", "Bearer " + apiKey)
				.json(body.toString())
				.withoutAuth()
				.sendAsync()
				.thenApply(response -> parseTranslation(response.body(), text, response.statusCode()))
				.exceptionally(throwable -> {
					String msg = throwable.getMessage() != null ? throwable.getMessage() : throwable.toString();
					if (msg.contains("timed out") || msg.contains("Timeout")) {
						msg = Core.bundle != null
								? Core.bundle.get("feature.translation.error.timeout", "Request timed out")
								: "Request timed out";
					}
					throw new RuntimeException(msg, throwable);
				});
	}

	public static String parseDevXResponse(int statusCode, String jsonResponse, String fallback) {
		return parseTranslation(jsonResponse, fallback, statusCode);
	}

	private static String parseTranslation(String jsonResponse, String fallback, int statusCode) {
		if (statusCode == 401 || statusCode == 403) {
			throw new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.invalid-api-key", "Invalid API key")
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
			if (root.has("choices") && !root.get("choices").asArray().isEmpty()) {
				Jval choice = root.get("choices").asArray().get(0);
				if (choice.has("message")) {
					Jval message = choice.get("message");
					if (message.has("content")) {
						String content = message.getString("content", fallback);
						if (content != null) {
							if (content.contains("<think>")) {
								content = content.replaceAll("(?s)<think>.*?</think>", "").trim();
							}
							content = content.replaceAll("(?i)^(translation|translated|bản dịch|vietnamese|tiếng việt|english|tiếng anh)\\s*:\\s*", "").trim();
							if (content.startsWith("\"") && content.endsWith("\"") && content.length() > 1) {
								content = content.substring(1, content.length() - 1).trim();
							}
							return content.isEmpty() ? fallback : content;
						}
						return fallback;
					}
				}
			}
			return fallback;
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse DevX response: " + e.getMessage(), e);
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
}
