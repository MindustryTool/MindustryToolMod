package mindustrytool.services.auth;

import arc.Core;
import arc.Events;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Timer;
import arc.util.serialization.Jval;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import mindustry.Vars;
import mindustrytool.Config;
import mindustrytool.events.LoginUriEvent;
import mindustrytool.models.response.UserSession;
import mindustrytool.services.HttpException;
import mindustrytool.services.MindustryTool;
import mindustrytool.services.Request;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Merged auth provider for the rewritten codebase. Implements AuthProvider and owns a Request
 * instance, and now also owns session/login/logout lifecycle previously in AuthService. All HTTP
 * for auth/* goes via MindustryTool typed facade except refresh which uses its own Request with
 * withoutAuth to avoid recursion.
 */
public class MindustryAuthProvider implements AuthProvider {
	private static MindustryAuthProvider instance;

	public static final String KEY_ACCESS_TOKEN = "mindustrytool.auth.access-token";
	public static final String KEY_REFRESH_TOKEN = "mindustrytool.auth.refresh-token";
	public static final String KEY_LOGIN_ID = "mindustrytool.auth.login-id";
	public static final String KEY_LOGIN_EXPIRY = "mindustrytool.auth.login-expiry";

	/** Marker for user-cancelled login so UI can stay silent instead of showing an error. */
	public static final class LoginCancelled extends RuntimeException {
		public LoginCancelled() {
			super();
		}
	}

	private final Request api;
	private CompletableFuture<Void> refreshFuture;
	private CompletableFuture<Void> loginFuture;
	private volatile boolean loginCancelled = false;
	private volatile @Nullable String activeLoginId;

	/**
	 * Single reactive source of truth for session identity (null when no session is loaded).
	 *
	 * <p><b>Threading contract:</b> mutated exclusively on the main thread. Solim signals notify
	 * observers synchronously on the calling thread, so setting from a network callback would run
	 * UI-bound computeds off the GL thread. All sets go through {@code Core.app.post}.
	 */
	private final Signal<UserSession> session = Signal.of(null);
	private final Signal<Boolean> sessionLoading = Signal.of(false);
	private final Signal<Throwable> sessionError = Signal.of(null);

	public static MindustryAuthProvider getInstance() {
		if (instance == null) {
			instance = new MindustryAuthProvider();
		}
		return instance;
	}

	private MindustryAuthProvider() {
		this.api = Request.builder()
				.baseUrl(Config.API_URL)
				.timeout(Duration.ofSeconds(10))
				.authProvider(this)
				.build();
	}

	public Request getApi() {
		return api;
	}

	// ─── Token storage ──────────────────────────────

	@Override
	public String getAccessToken() {
		return Core.settings.getString(KEY_ACCESS_TOKEN, null);
	}

	public String getRefreshToken() {
		return Core.settings.getString(KEY_REFRESH_TOKEN, null);
	}

	public void saveTokens(String accessToken, String refreshToken) {
		Core.settings.put(KEY_ACCESS_TOKEN, accessToken);
		Core.settings.put(KEY_REFRESH_TOKEN, refreshToken);
		Core.settings.forceSave();
	}

	public boolean isTokenNearExpiry(String token) {
		if (token == null) {
			return true;
		}
		try {
			String[] parts = token.split("\\.");
			if (parts.length < 2) return true;
			String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
			Jval json = Jval.read(payload);
			long exp = json.getLong("exp", 0);
			long now = System.currentTimeMillis() / 1000;
			return (exp - now) < 60;
		} catch (Exception e) {
			Log.err("Failed to parse token expiry", e);
			return true;
		}
	}

	@Override
	public synchronized CompletableFuture<Void> refreshIfNeeded() {
		if (refreshFuture != null && !refreshFuture.isDone()) {
			return refreshFuture;
		}

		refreshFuture = new CompletableFuture<>();

		String accessToken = getAccessToken();
		String refreshToken = getRefreshToken();

		if (refreshToken == null) {
			refreshFuture.complete(null);
			return refreshFuture;
		}

		if (accessToken != null && !isTokenNearExpiry(accessToken)) {
			refreshFuture.complete(null);
			return refreshFuture;
		}

		if (isTokenNearExpiry(refreshToken)) {
			Log.info("Refresh token near expiry, removed it");
			Core.settings.remove(KEY_REFRESH_TOKEN);
			refreshFuture.complete(null);
			return refreshFuture;
		}

		Jval json = Jval.newObject();
		json.put("refreshToken", refreshToken);

		api.post("auth/app/refresh")
				.withoutAuth()
				.json(json.toString())
				.timeout(Duration.ofSeconds(10))
				.sendAsync()
				.whenComplete((res, err) -> {
					if (err != null) {
						Throwable cause = err;
						if (cause instanceof CompletionException && cause.getCause() != null) {
							cause = cause.getCause();
						}
                        
						if (cause instanceof HttpException) {
							HttpException httpErr = (HttpException) cause;
							if (httpErr.statusCode() == 401) {
								Core.settings.remove(KEY_ACCESS_TOKEN);
								Core.settings.remove(KEY_REFRESH_TOKEN);
								Log.info("Remove tokens after 401");
								Log.err(httpErr.rawBody());
								refreshFuture.completeExceptionally(new RuntimeException("Refresh failed: HTTP 401 " + httpErr.rawBody(), httpErr));
								return;
							}
							Log.err("Failed to refresh token: HTTP " + httpErr.statusCode() + " " + httpErr.rawBody());
							refreshFuture.completeExceptionally(
									new RuntimeException("Refresh failed: HTTP " + httpErr.statusCode() + " " + httpErr.rawBody(), httpErr));
							return;
						}
						Log.err("Failed to refresh token", err);
						refreshFuture.completeExceptionally(err);
						return;
					}
					int code = res.statusCode();
					String body = res.body();
					if (code < 200 || code >= 300) {
						Log.err("Failed to refresh token: HTTP " + code + " " + body);
						refreshFuture.completeExceptionally(
								new RuntimeException("Refresh failed: HTTP " + code + " " + body));
						return;
					}
					try {
						Jval resJson = Jval.read(body);
						if (resJson.has("accessToken") && resJson.has("refreshToken")) {
							saveTokens(resJson.getString("accessToken"), resJson.getString("refreshToken"));
							refreshFuture.complete(null);
						} else {
							refreshFuture.completeExceptionally(
									new RuntimeException("Invalid refresh response: " + resJson));
						}
					} catch (Exception e) {
						refreshFuture.completeExceptionally(
								new RuntimeException("Failed to refresh token: exception", e));
					}
				});

		return refreshFuture;
	}

	public boolean isLoggedIn() {
		return Core.settings.has(KEY_ACCESS_TOKEN) && Core.settings.has(KEY_REFRESH_TOKEN);
	}

	// ─── Session ──────────────────────────────

	/** Reactive session identity for UI bindings. Prefer this over {@link #getSession()}. */
	public Readable<UserSession> session() {
		return session;
	}

	/** Reactive fetch-in-flight flag for UI bindings. */
	public Readable<Boolean> sessionLoading() {
		return sessionLoading;
	}

	/** Reactive fetch failure for UI bindings (null when no error). */
	public Readable<Throwable> sessionError() {
		return sessionError;
	}

	/**
	 * Non-subscribing read of the current session for non-reactive contexts (e.g. the static
	 * chat message parser). Reactive code MUST use {@link #session()} instead so updates propagate.
	 */
	public @Nullable UserSession getSession() {
		return session.peek();
	}

	public CompletableFuture<UserSession> fetchSession() {
		Core.app.post(() -> {
			sessionLoading.set(true);
			sessionError.set(null);
		});
		return MindustryTool.getSession().handle((fetched, err) -> {
			if (err != null) {
				Throwable cause = err.getCause() != null ? err.getCause() : err;
				Core.app.post(() -> {
					sessionError.set(cause);
					sessionLoading.set(false);
				});
				throw new RuntimeException(cause);
			}
			UserSession finalSession = fetched;
			Core.app.post(() -> {
				session.set(finalSession);
				sessionError.set(null);
				sessionLoading.set(false);
			});
			return fetched;
		});
	}

	// ─── Login ──────────────────────────────

	public synchronized CompletableFuture<Void> login() {
		if (loginFuture != null && !loginFuture.isDone()) {
			return loginFuture;
		}

		loginCancelled = false;
		loginFuture = new CompletableFuture<>();

		MindustryTool.getLoginUri().whenComplete((uri, err) -> {
			if (err != null) {
				activeLoginId = null;
				loginFuture.completeExceptionally(new RuntimeException("Failed to get login URI", err));
				return;
			}
			try {
				String loginUrl = uri.getLoginUrl();
				String loginId = uri.getLoginId();

				Core.settings.put(KEY_LOGIN_ID, loginId);
				Core.settings.put(
						KEY_LOGIN_EXPIRY,
						Instant.now().plus(Duration.ofMinutes(5)).toEpochMilli());
				Core.settings.forceSave();
				activeLoginId = loginId;

				Core.app.post(() -> Events.fire(new LoginUriEvent(loginUrl, loginId)));

				pollWithRetry(loginId, true).whenComplete((v, e) -> {
					if (loginId.equals(activeLoginId)) {
						activeLoginId = null;
					}
					if (e != null) {
						loginFuture.completeExceptionally(e);
					} else {
						loginFuture.complete(null);
					}
				});

				if (!Core.app.openURI(loginUrl)) {
					Core.app.setClipboardText(loginUrl);
					Core.app.post(() -> Vars.ui.showInfoFade(Core.bundle.get("auth.login.browser-fallback")));
				}
			} catch (Exception e) {
				activeLoginId = null;
				loginFuture.completeExceptionally(new RuntimeException("Failed to start login flow", e));
			}
		});

		return loginFuture;
	}

	public synchronized void cancelLogin() {
		if (loginFuture != null && !loginFuture.isDone()) {
			loginCancelled = true;
			activeLoginId = null;
			clearPendingLoginKeys();
			loginFuture.completeExceptionally(new LoginCancelled());
		}
	}

	public CompletableFuture<Void> pollLoginToken(String loginId) {
		return pollWithRetry(loginId, false);
	}

	private CompletableFuture<Void> pollWithRetry(String loginId, boolean cancellable) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		long expiryMillis = pendingLoginExpiry();
		pollAttempt(loginId, expiryMillis, future, cancellable);
		return future;
	}

	private void pollAttempt(String loginId, long expiryMillis, CompletableFuture<Void> future, boolean cancellable) {
		if (future.isDone()) {
			return;
		}
		if (cancellable && loginCancelled) {
			future.completeExceptionally(new LoginCancelled());
			return;
		}
		String stored = Core.settings.getString(KEY_LOGIN_ID, null);
		if (stored == null || !stored.equals(loginId)) {
			future.completeExceptionally(new RuntimeException(Core.bundle.get("auth.login.failed")));
			return;
		}
		if (Instant.now().toEpochMilli() >= expiryMillis) {
			clearPendingLoginKeys();
			future.completeExceptionally(new RuntimeException(Core.bundle.get("auth.login.timeout")));
			return;
		}

		MindustryTool.pollLoginToken(loginId).whenComplete((token, err) -> {
			if (future.isDone()) {
				return;
			}
			if (err != null) {
				if (isTerminalLoginError(err)) {
					clearPendingLoginKeys();
					future.completeExceptionally(new RuntimeException("Failed to get login token", err));
					return;
				}
				if (Instant.now().toEpochMilli() >= expiryMillis) {
					clearPendingLoginKeys();
					future.completeExceptionally(new RuntimeException(Core.bundle.get("auth.login.timeout"), err));
					return;
				}
				schedulePollRetry(loginId, expiryMillis, future, cancellable);
				return;
			}
			try {
				boolean hasTokens = token != null && token.getAccessToken() != null && token.getRefreshToken() != null;
				if (hasTokens) {
					if (cancellable && loginCancelled) {
						future.completeExceptionally(new LoginCancelled());
						return;
					}
					saveTokens(token.getAccessToken(), token.getRefreshToken());
					clearPendingLoginKeys();

					fetchSession().whenComplete((v, e) -> {
						if (e != null) {
							future.completeExceptionally(e);
						} else {
							future.complete(null);
						}
					});
				} else if (Instant.now().toEpochMilli() >= expiryMillis) {
					clearPendingLoginKeys();
					future.completeExceptionally(new RuntimeException(Core.bundle.get("auth.login.timeout")));
				} else {
					schedulePollRetry(loginId, expiryMillis, future, cancellable);
				}
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		});
	}

	private long pendingLoginExpiry() {
		long expiryMillis = Core.settings.getLong(KEY_LOGIN_EXPIRY, 0);
		return expiryMillis > 0 ? expiryMillis
				: Instant.now().plus(Duration.ofMinutes(5)).toEpochMilli();
	}

	private void clearPendingLoginKeys() {
		Core.settings.remove(KEY_LOGIN_ID);
		Core.settings.remove(KEY_LOGIN_EXPIRY);
	}

	private void schedulePollRetry(String loginId, long expiryMillis, CompletableFuture<Void> future, boolean cancellable) {
		Timer.schedule(() -> pollAttempt(loginId, expiryMillis, future, cancellable), 2);
	}

	private boolean isTerminalLoginError(Throwable err) {
		Throwable cause = err instanceof CompletionException && err.getCause() != null ? err.getCause() : err;
		if (cause instanceof HttpException) {
			int code = ((HttpException) cause).statusCode();
			return code >= 400 && code < 500 && code != 408;
		}
		return false;
	}

	// ─── Logout ──────────────────────────────

	public void logout() {
		String accessToken = Core.settings.getString(KEY_ACCESS_TOKEN, "");
		String refreshToken = Core.settings.getString(KEY_REFRESH_TOKEN, "");

		if (!accessToken.isEmpty() && !refreshToken.isEmpty()) {
			MindustryTool.logout(accessToken, refreshToken).whenComplete((v, err) -> {
				if (err != null) {
					Log.err("Logout failed", err);
				} else {
					Log.info("Logout successful");
				}
			});
		}

		Core.settings.remove(KEY_ACCESS_TOKEN);
		Core.settings.remove(KEY_REFRESH_TOKEN);
		Core.settings.remove(KEY_LOGIN_ID);

		Core.app.post(() -> {
			session.set(null);
			sessionError.set(null);
			sessionLoading.set(false);
		});

		Log.info("Logged out");
	}

	// ─── Init ──────────────────────────────

	public void init() {
		fetchSession();

		Timer.schedule(
				() -> {
					if (isLoggedIn()) {
						fetchSession();
					}
				},
				60 * 5,
				60 * 5);

		String loginId = Core.settings.getString(KEY_LOGIN_ID, null);

		if (loginId != null && !loginId.isEmpty()) {
			Instant expiry = Instant.ofEpochMilli(Core.settings.getLong(KEY_LOGIN_EXPIRY, 0));

			if (expiry.isBefore(Instant.now())) {
				clearPendingLoginKeys();
			} else {
				pollLoginToken(loginId).exceptionally(e -> {
					Log.err("Background login polling failed", e);
					return null;
				});
			}
		}
	}
}
