package mindustrytool.features.translation.providers;

import arc.Core;
import arc.util.serialization.Jval;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import mindustry.Vars;
import mindustrytool.features.translation.TranslationFeature;
import mindustrytool.features.translation.TranslationProvider;
import mindustrytool.services.HttpException;
import mindustrytool.services.Request;

/**
 * Translation provider using DeepL API (v2 translate endpoint).
 */
public class DeepLTranslationProvider implements TranslationProvider {

	public static final String ID = "deepl";
	private static final String API_URL_FREE = "https://api-free.deepl.com";
	private static final String API_URL_PRO = "https://api.deepl.com";

	private final TranslationFeature feature;

	public DeepLTranslationProvider(TranslationFeature feature) {
		this.feature = feature;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return Core.bundle.get("feature.translation.provider.deepl", "DeepL");
	}

	@Override
	public boolean isConfigured() {
		String key = feature.deeplApiKeyConfig.get();
		return key != null && !key.trim().isEmpty();
	}

	@Override
	public CompletableFuture<String> translate(String text, String targetLanguage) {
		if (text == null || text.trim().isEmpty()) {
			return CompletableFuture.completedFuture("");
		}

		String apiKey = feature.deeplApiKeyConfig.get();
		if (apiKey == null || apiKey.trim().isEmpty()) {
			CompletableFuture<String> failed = new CompletableFuture<>();
			failed.completeExceptionally(
					new IllegalStateException(Core.bundle.get("feature.translation.error.no-api-key")));
			return failed;
		}

		apiKey = apiKey.trim();
		String baseUrl = apiKey.endsWith(":fx") ? API_URL_FREE : API_URL_PRO;
		int timeout = feature.deeplTimeoutConfig.get();
		String deeplLang = resolveDeepLLang(targetLanguage);

		Jval body = Jval.newObject();
		Jval textArray = Jval.newArray();
		textArray.add(text);
		body.put("text", textArray);
		body.put("target_lang", deeplLang);

		Request request = Request.builder()
				.baseUrl(baseUrl)
				.timeout(Duration.ofSeconds(Math.max(2, timeout)))
				.build();

		return request.post("/v2/translate")
				.header("Authorization", "DeepL-Auth-Key " + apiKey)
				.json(body.toString())
				.withoutAuth()
				.sendAsync()
				.thenApply(response -> parseDeepLResponse(response.statusCode(), response.body(), text))
				.exceptionally(err -> {
					Throwable cause = err instanceof CompletionException && err.getCause() != null ? err.getCause() : err;
					if (cause instanceof HttpException) {
						HttpException httpErr = (HttpException) cause;
						throw mapDeepLError(httpErr.statusCode());
					}
					if (cause instanceof RuntimeException) {
						throw (RuntimeException) cause;
					}
					throw new RuntimeException(cause);
				});
	}

	public static String resolveDeepLLang(String lang) {
		if (lang == null || lang.trim().isEmpty()) {
			return Core.bundle.getLocale().getLanguage().toUpperCase(Locale.ROOT);
		}
		String clean = lang.trim();
		if (clean.length() == 2) {
			return clean.toUpperCase(Locale.ROOT);
		}
		for (Locale loc : Vars.locales) {
			if (clean.equalsIgnoreCase(loc.toString()) || clean.equalsIgnoreCase(loc.getLanguage())) {
				return loc.getLanguage().toUpperCase(Locale.ROOT);
			}
		}
		String lower = clean.toLowerCase(Locale.ROOT);
		switch (lower) {
			case "english": return "EN";
			case "russian": return "RU";
			case "japanese": return "JA";
			case "chinese": return "ZH";
			case "german": return "DE";
			case "french": return "FR";
			case "spanish": return "ES";
			case "polish": return "PL";
			case "italian": return "IT";
			case "portuguese": return "PT";
			case "korean": return "KO";
			case "vietnamese": return "VI";
			default:
				return Core.bundle != null ? Core.bundle.getLocale().getLanguage().toUpperCase(Locale.ROOT) : "EN";
		}
	}

	public static RuntimeException mapDeepLError(int statusCode) {
		if (statusCode == 401 || statusCode == 403) {
			return new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.invalid-key", "Invalid API key")
					: "Invalid API key");
		}
		if (statusCode == 429 || statusCode == 456) {
			return new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.rate-limit", "Rate limit exceeded")
					: "Rate limit exceeded");
		}
		if (statusCode >= 500) {
			return new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.server-error", "Server error")
					: "Server error");
		}
		return new RuntimeException((Core.bundle != null
				? Core.bundle.get("feature.translation.error.network", "Network error")
				: "Network error") + " (HTTP " + statusCode + ")");
	}

	public static String parseDeepLResponse(int statusCode, String jsonResponse, String fallback) {
		if (statusCode != 200) {
			throw mapDeepLError(statusCode);
		}

		if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
			return fallback;
		}

		try {
			Jval root = Jval.read(jsonResponse);
			if (root.has("translations") && !root.get("translations").asArray().isEmpty()) {
				String text = root.get("translations").asArray().get(0).getString("text", fallback);
				return text != null ? text.trim() : fallback;
			}
			return fallback;
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse DeepL response: " + e.getMessage(), e);
		}
	}
}
