package mindustrytool.services;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import mindustrytool.Config;
import mindustrytool.models.response.TaskResponse;
import mindustrytool.utils.JsonUtils;
import solim.reactive.QueryCache;
import solim.reactive.QueryKey;

public final class Github {

	private static final Request githubApi = Request.builder()
			.baseUrl(Config.GITHUB_API_URL)
			.timeout(Duration.ofSeconds(10))
			.build();

	private static final Request projectApi = Request.builder()
			.baseUrl(Config.PROJECT_URL)
			.timeout(Duration.ofSeconds(10))
			.build();

	private static final Request rawApi =
			Request.builder().timeout(Duration.ofSeconds(10)).build();

	private Github() {}

	/**
	 * Warms both live cacheable endpoints once using QueryCache prefetching.
	 * Safe to call repeatedly. Never blocks the caller.
	 */
	public static void prefetchAll() {
		QueryCache cache = QueryCache.getInstance();
		cache.prefetch(QueryKey.of("github", "mod.hjson"),
				() -> rawApi.get(Config.MOD_HJSON_URL).sendAsync().thenApply(r -> r.body()));
		cache.prefetch(QueryKey.of("github", "releases"),
				() -> githubApi.get("").sendAsync().thenApply(r -> r.body()));
	}

	// ─── Mod metadata ──────────────────────────────────────────────

	/** Raw mod.hjson is not a JSON object mapping to a DTO; keep as String. */
	public static CompletableFuture<String> getModHjson() {
		return QueryCache.getInstance().fetchCached(QueryKey.of("github", "mod.hjson"),
				() -> rawApi.get(Config.MOD_HJSON_URL).sendAsync().thenApply(r -> r.body()));
	}

	// ─── Releases ──────────────────────────────────────────────────

	/**
	 * GitHub releases return a heterogeneous JSON array; callers parse via Jval/JsonUtils. Kept as
	 * String to avoid coupling to GitHub schema; use JsonUtils if typed parsing needed.
	 */
	public static CompletableFuture<String> getReleases() {
		return QueryCache.getInstance().fetchCached(QueryKey.of("github", "releases"),
				() -> githubApi.get("").sendAsync().thenApply(r -> r.body()));
	}

	public static CompletableFuture<String> getReleases(int page, int perPage) {
		return QueryCache.getInstance().fetchCached(QueryKey.of("github", "releases", page, perPage),
				() -> rawApi.get(Config.GITHUB_API_URL + "?page=" + page + "&per_page=" + perPage)
						.sendAsync()
						.thenApply(r -> r.body()));
	}

	// ─── Project tasks ─────────────────────────────────────────────

	/**
	 * Live on every call by contract: status-parameterized and freshness-sensitive. Never memoized.
	 */
	public static CompletableFuture<TaskResponse> getProjectTasks(String status) {
		return projectApi
				.get("/api/v1/projects/" + Config.PROJECT_ID + "/tasks?status=" + status)
				.timeout(Duration.ofMillis(20_000))
				.sendAsync()
				.thenApply(r -> JsonUtils.fromJson(TaskResponse.class, r.body()));
	}
}
