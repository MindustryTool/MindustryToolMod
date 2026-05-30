# Auth Audit

## Findings

### Security

- **CRITICAL** `AuthService.java:293-295` — `isTokenNearExpiry` decodes JWT without signature verification; trusts any JWT's `exp` claim
- **HIGH** `AuthService.java:96` — Access token stored in `Core.settings` as plain string — Arc settings may be persisted unencrypted
- **HIGH** `AuthHttp.java:67-69` — Token refresh is called synchronously before every request; if refresh fails, ALL API calls that depend on auth will fail simultaneously
- **MEDIUM** `AuthService.java:196` — Logout removes tokens but HTTP POST to server for logout confirmation is fire-and-forget with error only logged
- **MEDIUM** `AuthService.java:267-271` — On 401 from token refresh, silently removes tokens without notifying user

### Concurrency

- **HIGH** `AuthService.java:82-88` — `loginFuture` is shared across threads; `synchronized` on `login()` but inner callbacks (`pollLoginToken`, `loginDialog.show()`) run on arbitrary threads
- **MEDIUM** `AuthService.java:53` — `refreshFuture` shared across threads; `synchronized` method but CompletableFuture chains may interleave
- **MEDIUM** `AuthService.java:253-254` — `saveTokens` called from `pollLoginToken` which runs on HTTP callback thread; `Core.settings` may not be thread-safe

### Anti-Patterns

- **MEDIUM** `AuthService.java:20-26` — `KEY_*` constants use `mindustrytool.auth.` prefix as hardcoded string — not centralized like other config classes
- **MEDIUM** `AuthHttp.java:27` — `AuthRequest` class uses `HashMap` for headers; doesn't set `Content-Type` automatically for POST requests
- **LOW** `AuthService.java:233` — `isLoggedIn()` checks both `currentSession != null` AND settings keys — potential inconsistency if settings and session are out of sync
- **MEDIUM** `AuthLoginDialog.java:14` — Login dialog created once and reused; if user cancels and re-triggers, dialog is already shown

### Dead Code / Tech Debt

- **LOW** `AuthService.java:110` — `authWindow` is stored but `toFront()` is called once; no re-ordering logic if other UI elements overlap
- **LOW** `AuthService.java:68-72` — Background login polling continues even if `login()` was never called; could waste resources if user never triggers login

### Performance

- **LOW** `AuthService.java:48-51` — Timer scheduled every 5 minutes for session refresh; even if not logged in, the timer runs and checks `isLoggedIn()`
