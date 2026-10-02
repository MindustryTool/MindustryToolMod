package mindustrytool.features.translation.providers;

import arc.Core;
import arc.util.Nullable;
import arc.util.serialization.Jval;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import mindustry.Vars;
import mindustrytool.features.translation.TranslationProvider;
import mindustrytool.services.HttpException;
import mindustrytool.services.Request;

/**
 * Translation provider using Google Translate web API (translate_a/single).
 * Requires no API key, includes in-memory LRU caching and request throttling.
 */
public class GoogleWebTranslationProvider implements TranslationProvider {

	public static final String ID = "googleweb";
	private static final String API_URL = "https://translate.googleapis.com";
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
	private static final long MIN_DELAY_MS = 500L;
	private static final int MAX_CACHE_ENTRIES = 200;

	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "GoogleWeb-Throttler");
		thread.setDaemon(true);
		return thread;
	});

	private final Map<String, String> cache = Collections.synchronizedMap(
			new LinkedHashMap<String, String>(MAX_CACHE_ENTRIES + 1, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
					return size() > MAX_CACHE_ENTRIES;
				}
			}
	);

	private final Object throttleLock = new Object();
	private long nextAvailableTime = 0L;

	public @Nullable String getCached(String text, String targetLanguage) {
		String targetCode = resolveGoogleLang(targetLanguage);
		return cache.get(targetCode + ":" + text);
	}

	public void putCache(String text, String targetLanguage, String result) {
		String targetCode = resolveGoogleLang(targetLanguage);
		cache.put(targetCode + ":" + text, result);
	}

	public int getCacheSize() {
		return cache.size();
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return Core.bundle != null
				? Core.bundle.get("feature.translation.provider.googleweb", "Google (Free)")
				: "Google (Free)";
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public CompletableFuture<String> translate(String text, String targetLanguage) {
		if (text == null || text.trim().isEmpty()) {
			return CompletableFuture.completedFuture("");
		}

		String targetCode = resolveGoogleLang(targetLanguage);
		String cacheKey = targetCode + ":" + text;

		String cached = cache.get(cacheKey);
		if (cached != null) {
			return CompletableFuture.completedFuture(cached);
		}

		long scheduledTime;
		synchronized (throttleLock) {
			long now = System.currentTimeMillis();
			scheduledTime = Math.max(now, nextAvailableTime);
			nextAvailableTime = scheduledTime + MIN_DELAY_MS;
		}

		long delay = scheduledTime - System.currentTimeMillis();
		if (delay <= 0) {
			return executeHttpRequest(text, targetCode, cacheKey);
		}

		CompletableFuture<String> future = new CompletableFuture<>();
		SCHEDULER.schedule(() -> {
			executeHttpRequest(text, targetCode, cacheKey).whenComplete((res, err) -> {
				if (err != null) {
					future.completeExceptionally(err);
				} else {
					future.complete(res);
				}
			});
		}, delay, TimeUnit.MILLISECONDS);
		return future;
	}

	private CompletableFuture<String> executeHttpRequest(String text, String targetCode, String cacheKey) {
		Request request = Request.builder()
				.baseUrl(API_URL)
				.timeout(Duration.ofSeconds(10))
				.header("User-Agent", USER_AGENT)
				.build();

		return request.get("/translate_a/single")
				.query("client", "gtx")
				.query("sl", "auto")
				.query("tl", targetCode)
				.query("dt", "t")
				.query("q", text)
				.withoutAuth()
				.sendAsync()
				.thenApply(response -> {
					String parsed = parseGoogleResponse(response.statusCode(), response.body(), text);
					cache.put(cacheKey, parsed);
					return parsed;
				})
				.exceptionally(err -> {
					Throwable cause = err instanceof CompletionException && err.getCause() != null ? err.getCause() : err;
					if (cause instanceof HttpException) {
						HttpException httpErr = (HttpException) cause;
						throw mapGoogleError(httpErr.statusCode());
					}
					if (cause instanceof RuntimeException) {
						throw (RuntimeException) cause;
					}
					throw new RuntimeException(cause);
				});
	}

	public static String parseGoogleResponse(int statusCode, @Nullable String jsonResponse, String fallback) {
		if (statusCode != 200) {
			throw mapGoogleError(statusCode);
		}

		if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
			return fallback;
		}

		try {
			Jval root = Jval.read(jsonResponse);
			if (root.isArray() && !root.asArray().isEmpty()) {
				Jval sentences = root.asArray().get(0);
				if (sentences != null && sentences.isArray()) {
					StringBuilder sb = new StringBuilder();
					for (Jval item : sentences.asArray()) {
						if (item != null && item.isArray() && !item.asArray().isEmpty()) {
							String part = item.asArray().get(0).asString();
							if (part != null) {
								sb.append(part);
							}
						}
					}
					String result = sb.toString().trim();
					return !result.isEmpty() ? result : fallback;
				}
			}
			return fallback;
		} catch (Exception e) {
			return fallback;
		}
	}

	public static RuntimeException mapGoogleError(int statusCode) {
		if (statusCode == 403) {
			return new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.google-forbidden", "Google Translate access denied or blocked.")
					: "Google Translate access denied or blocked.");
		}
		if (statusCode == 429) {
			return new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.rate-limit", "Rate limit exceeded. Please wait a moment.")
					: "Rate limit exceeded. Please wait a moment.");
		}
		if (statusCode >= 500) {
			return new RuntimeException(Core.bundle != null
					? Core.bundle.get("feature.translation.error.server-error", "Translation service encountered a server error.")
					: "Translation service encountered a server error.");
		}
		return new RuntimeException((Core.bundle != null
				? Core.bundle.get("feature.translation.error.network", "Network error or request timed out.")
				: "Network error or request timed out.") + " (HTTP " + statusCode + ")");
	}

	public static String resolveGoogleLang(@Nullable String lang) {
		if (lang == null || lang.trim().isEmpty()) {
			return Core.bundle != null ? Core.bundle.getLocale().getLanguage().toLowerCase(Locale.ROOT) : "en";
		}
		String clean = lang.trim();
		if (clean.length() == 2) {
			return clean.toLowerCase(Locale.ROOT);
		}
		if (Vars.locales != null) {
			for (Locale loc : Vars.locales) {
				if (clean.equalsIgnoreCase(loc.toString()) || clean.equalsIgnoreCase(loc.getLanguage())) {
					return loc.getLanguage().toLowerCase(Locale.ROOT);
				}
			}
		}
		String lower = clean.toLowerCase(Locale.ROOT);
		switch (lower) {
			case "english": return "en";
			case "russian": return "ru";
			case "japanese": return "ja";
			case "chinese": return "zh-CN";
			case "german": return "de";
			case "french": return "fr";
			case "spanish": return "es";
			case "polish": return "pl";
			case "italian": return "it";
			case "portuguese": return "pt";
			case "korean": return "ko";
			case "vietnamese": return "vi";
			default:
				return Core.bundle != null ? Core.bundle.getLocale().getLanguage().toLowerCase(Locale.ROOT) : "en";
		}
	}
}
