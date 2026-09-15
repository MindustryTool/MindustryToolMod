package mindustrytool.services;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import mindustrytool.Config;
import mindustrytool.models.response.TaskResponse;
import mindustrytool.utils.JsonUtils;

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

	/**
	 * Session memo for one endpoint. Holds the shared in-flight or completed future; a failed
	 * future is dropped so the next call retries live. Never caches failures.
	 */
	private static final class Memo<T> {
		private final AtomicReference<CompletableFuture<T>> ref = new AtomicReference<>();

		synchronized CompletableFuture<T> get(Supplier<CompletableFuture<T>> loader) {
			CompletableFuture<T> existing = ref.get();
			if (existing != null) {
				return existing;
			}
			CompletableFuture<T> created = loader.get();
			ref.set(created);
			created.whenComplete((value, err) -> {
				if (err != null) {
					ref.compareAndSet(created, null);
				}
			});
			return created;
		}
	}

	private static final Memo<String> modHjsonMemo = new Memo<>();
	private static final Memo<String> releasesMemo = new Memo<>();
	private static final ConcurrentHashMap<String, Memo<String>> pagedReleasesMemos = new ConcurrentHashMap<>();

	private Github() {}

	/**
	 * Warms both live cacheable endpoints once, fire-and-forget. Safe to call repeatedly; extra
	 * calls reuse the memoized futures. Never blocks the caller.
	 */
	public static void prefetchAll() {
		try {
			getModHjson().exceptionally(err -> null);
		} catch (Exception ignored) {
		}
		try {
			getReleases().exceptionally(err -> null);
		} catch (Exception ignored) {
		}
	}

	// ─── Mod metadata ──────────────────────────────────────────────

	/** Raw mod.hjson is not a JSON object mapping to a DTO; keep as String. */
	public static CompletableFuture<String> getModHjson() {
		return modHjsonMemo.get(() -> rawApi.get(Config.MOD_HJSON_URL).sendAsync().thenApply(r -> r.body()));
	}

	// ─── Releases ──────────────────────────────────────────────────

	/**
	 * GitHub releases return a heterogeneous JSON array; callers parse via Jval/JsonUtils. Kept as
	 * String to avoid coupling to GitHub schema; use JsonUtils if typed parsing needed.
	 */
	public static CompletableFuture<String> getReleases() {
		return releasesMemo.get(() -> githubApi.get("").sendAsync().thenApply(r -> r.body()));
	}

	public static CompletableFuture<String> getReleases(int page, int perPage) {
		String key = page + "x" + perPage;
		Memo<String> memo = pagedReleasesMemos.get(key);
		if (memo == null) {
			Memo<String> created = new Memo<>();
			Memo<String> existing = pagedReleasesMemos.putIfAbsent(key, created);
			memo = existing != null ? existing : created;
		}
		return memo.get(() -> rawApi.get(Config.GITHUB_API_URL + "?page=" + page + "&per_page=" + perPage)
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
