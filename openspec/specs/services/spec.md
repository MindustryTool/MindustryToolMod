# services Specification

## Purpose

Mechanical merge of 8 specs per change `spec-domain-merge` (stage 2 services, concat-then-dedupe). Sources: http-client, api-models, auth-service, auth-session-signal, github-service, mindustrytool-api, request-query-builder, update-service. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work.

## Requirements

**Source: http-client**

Instance-based HTTP client for `mindustrytool.services` that wraps `java.net.http.HttpClient`/`HttpRequest`/`HttpResponse`, supports per-`Request` optional `AuthProvider` (Bearer only), fluent per-request builder, and multiple independent API clients.
### Requirement: Instance-based Request with builder

`Request` SHALL be an instantiable class wrapping a shared `java.net.http.HttpClient`, with per-instance `baseUrl`, default `timeout`, and optional `AuthProvider`. It SHALL be created via builder `Request.builder().baseUrl(...).timeout(...).authProvider(...).build()` and MUST NOT use global static auth state.

#### Scenario: Builder creates independent instances
- **WHEN** `Request.builder().baseUrl("https://example.com/api/").build()` and `Request.builder().baseUrl("https://other.example.com/").authProvider(otherProvider).build()` are created
- **THEN** each instance stores its own `baseUrl`/`timeout`/`authProvider` independently and `authProvider` may be `null` for public APIs

#### Scenario: Public API without authentication
- **WHEN** `Request.builder().baseUrl("https://example.com/api/").build()` is used
- **THEN** the instance has no `AuthProvider` and `sendAsync()` sends without `Authorization` header

#### Scenario: Authenticated API with Bearer provider
- **WHEN** `Request.builder().baseUrl(Config.API_URL).authProvider(authService).build()` is used where `authService` implements `AuthProvider`
- **THEN** the instance stores the provider and uses it for all requests unless `withoutAuth()` is set

#### Scenario: Shared HttpClient may be reused
- **WHEN** multiple `Request` instances are created
- **THEN** the underlying `HttpClient` MAY be a shared static instance (e.g. `CLIENT`) with `connectTimeout(10s)`; each `Request` MUST keep its own `baseUrl`/`timeout`/`authProvider`

#### Scenario: No global static auth provider
- **WHEN** `src/mindustrytool/services/Request.java` is inspected
- **THEN** it contains no `static AuthProvider`, no `setAuthProvider(...)`, and no `static volatile` token state; auth is per-instance only

### Requirement: AuthProvider interface for Bearer token

A new `mindustrytool.services.AuthProvider` SHALL provide Bearer-only authentication via `CompletableFuture<Void> refreshIfNeeded()` and `String getAccessToken()` and MUST NOT support Basic/API-key/chains/interceptors.

#### Scenario: AuthProvider contract
- **WHEN** `AuthProvider` is inspected
- **THEN** it declares `refreshIfNeeded()` returning `CompletableFuture<Void>` and `getAccessToken()` returning `String`/`null`, with no other auth methods

#### Scenario: Request uses provider in sendAsync flow
- **WHEN** `api.get("users/me").sendAsync()` is called on a `Request` with `AuthProvider`
- **THEN** `sendAsync` first calls `authProvider.refreshIfNeeded()`, then `getAccessToken()`, then adds `Authorization: Bearer <token>` if non-null before `HttpClient.sendAsync()`

#### Scenario: withoutAuth bypasses provider
- **WHEN** `api.get("auth/app/login-uri").withoutAuth().sendAsync()` is called
- **THEN** the builder skips `refreshIfNeeded()` and does not add `Authorization` even if the `Request` has an `AuthProvider`

### Requirement: Fluent per-request builder

Each `Request` SHALL expose `get/post/put/delete(String url)` returning a fluent `RequestBuilder` that supports `header`, `timeout`, `body`, `json`, `bytes`, `withoutAuth`, `sendAsync()`, and `sendAsync(BodyHandler<T>)` without duplicated `auth*` methods.

#### Scenario: Fluent GET with header and timeout
- **WHEN** `api.get("users/me").header("Accept","application/json").timeout(Duration.ofSeconds(30)).sendAsync()` is called
- **THEN** it builds a `GET` `HttpRequest` with the header, per-request timeout override, and shared/client timeout fallback

#### Scenario: POST JSON helper
- **WHEN** `api.post("items").json(json).sendAsync()` is called
- **THEN** it sets `Content-Type: application/json` and `BodyPublishers.ofString(json)`

#### Scenario: Body variants
- **WHEN** `body(String)` is called it uses `BodyPublishers.ofString`; **WHEN** `bytes(byte[])` is called it uses `BodyPublishers.ofByteArray`; `header` appends to `LinkedHashMap` for stable order

#### Scenario: No auth* duplicated methods
- **WHEN** `Request` and `RequestBuilder` are inspected
- **THEN** there are no `authGet/authPost/authPut/authDelete` methods; authentication is automatic via the instance's `AuthProvider`

### Requirement: URL resolution and timeout handling

`RequestBuilder` SHALL resolve relative URLs against `baseUrl` and absolute URLs ( `http://`/`https://` ) as-is, and SHALL apply per-request `timeout` or instance default `DEFAULT_TIMEOUT(10s)`.

#### Scenario: Relative URL resolves against baseUrl
- **WHEN** `baseUrl="https://api.example.com/api/v4"` and `get("users/me")` is called
- **THEN** resolved URL is `https://api.example.com/api/v4/users/me` (handling trailing/leading slash and `?`/`#` without extra slash)

#### Scenario: Absolute URL bypasses baseUrl
- **WHEN** `api.get("https://other-api.example.com/data").sendAsync()` is called
- **THEN** the request URI is exactly `https://other-api.example.com/data` regardless of `baseUrl`

#### Scenario: Per-request timeout overrides instance timeout
- **WHEN** instance `timeout=10s` and `get("/maps/1/data").timeout(Duration.ofSeconds(60)).sendAsync(BodyHandlers.ofByteArray())` is called
- **THEN** the `HttpRequest` is built with `timeout(60s)`; otherwise it uses the instance default

### Requirement: AuthService implements AuthProvider with deduplicated refresh

`mindustrytool.services.MindustryAuthProvider` (preserved, NOT replaced by `AuthService`) SHALL implement `AuthProvider`, own `private final Request api = Request.builder().baseUrl(Config.API_URL).authProvider(this).build()`, handle token storage in `Core.settings` `mindustrytool.auth.*`, JWT `exp` check (`<60s` near-expiry via `Jval`), and deduplicated concurrent refresh via `single refreshFuture: CompletableFuture<Void>`. It SHALL remain the sole `AuthProvider` wired to `MindustryTool` (`Request.builder().baseUrl(Config.API_URL).authProvider(MindustryAuthProvider.getInstance()).build()` for `api`). New `mindustrytool.services.AuthService` SHALL NOT implement `AuthProvider`, SHALL NOT own a `Request` for `auth/*`, and SHALL delegate its `refreshIfNeeded`-like needs to the preserved provider indirectly via `MindustryTool`/`MindustryAuthProvider` (no second `api.post("auth/app/refresh").withoutAuth().json(...)` in `AuthService`).

#### Scenario: Refresh deduplication still in preserved provider
- **WHEN** three concurrent `api.get("/auth/session").sendAsync()` calls via `MindustryTool.getSession()` find the access token near expiry
- **THEN** only one `POST auth/app/refresh` with `withoutAuth()` from `MindustryAuthProvider.refreshIfNeeded()` is sent; all three await the same `refreshFuture`; success completes all, failure fails all exceptionally; `AuthService` does not send its own refresh

#### Scenario: Recursion prevention unchanged
- **WHEN** `MindustryAuthProvider.refreshIfNeeded()` triggers its `api.post("auth/app/refresh").withoutAuth().json(...).sendAsync()`
- **THEN** that inner request has `useAuth=false` so it does not call `refreshIfNeeded()` again; similarly `MindustryTool.getLoginUri()`/`pollLoginToken()` use `publicApi.withoutAuth()`

#### Scenario: Authenticated session fetch uses automatic Bearer via preserved provider
- **WHEN** `MindustryTool.getSession()` (`api.get("/auth/session").sendAsync()`) is called or `AuthService.fetchSession()` delegates to it
- **THEN** it automatically triggers `MindustryAuthProvider.refreshIfNeeded()` and sends `Authorization: Bearer <latest token>` without caller manually adding the header

#### Scenario: AuthService does not own auth Request nor duplicate refresh
- **WHEN** `src/mindustrytool/services/AuthService.java` and `src/mindustrytool/services/MindustryAuthProvider.java` are inspected
- **THEN** `AuthService` contains no `Request api = Request.builder().baseUrl(Config.API_URL).authProvider(this).build()` and no `refreshFuture: CompletableFuture<Void>` for token refresh; only `MindustryAuthProvider` contains that `Request api` and `refreshFuture`; `AuthService` contains `CompletableFuture<Void> loginFuture` for login deduplication only

#### Scenario: MindustryTool wiring preserved
- **WHEN** `src/mindustrytool/services/MindustryTool.java` is inspected
- **THEN** it still contains `private static final Request api = Request.builder().baseUrl(Config.API_URL).authProvider(MindustryAuthProvider.getInstance()).build()` and `private static final Request publicApi` without provider; it does NOT wire `AuthService.getInstance()` as `authProvider`

### Requirement: Multiple independent API clients

The architecture SHALL support `mindustrytool` API with auth, another API with different auth, and a public API without auth simultaneously without interference.

#### Scenario: Three clients coexist
- **WHEN** `Request mindustryApi = Request.builder().baseUrl(Config.API_URL).authProvider(mindustryAuthProvider).build()`, `Request otherApi = Request.builder().baseUrl(OTHER_API_URL).authProvider(otherAuthProvider).build()`, and `Request publicApi = Request.builder().baseUrl(PUBLIC_API_URL).build()` exist
- **THEN** each sends with its own provider (or none) and tokens do not leak between clients

#### Scenario: MindustryTool uses two internal clients
- **WHEN** `MindustryTool` is inspected
- **THEN** it holds `private static final Request api` with `MindustryAuthProvider` (AuthProvider) and `private static final Request publicApi` without provider, using `api` for `/chats/*`/`/auth/session` and `publicApi` for `/ping`, `/maps/*`, `/schematics/*`, images, and unauth auth endpoints (`/auth/app/login-uri` with `withoutAuth`)

### Requirement: Multipart upload helper retained

`Request` SHALL retain `static byte[] buildMultipartBody(boundary, fileBytes, fileName, hash)` building `multipart/form-data` with `hash` and `file` parts.

#### Scenario: Multipart body structure
- **WHEN** `buildMultipartBody` is called
- **THEN** it writes `hash` part and `file` part with `Content-Type: application/octet-stream` bounded by `--boundary` and `--boundary--`

### Requirement: Import hygiene for Request

`Request` SHALL use explicit imports for `HttpClient`, `HttpRequest`, `HttpResponse`, `BodyHandlers`, `BodyPublishers`, `URI`, `Duration`, `StandardCharsets` and MUST NOT use fully-qualified inline names inside method bodies.

#### Scenario: No fully-qualified Http types in method bodies
- **WHEN** `src/mindustrytool/services/Request.java` is inspected
- **THEN** it contains `import java.net.http.HttpRequest` etc. and bodies reference `HttpRequest`, `BodyHandlers`, `BodyPublishers`, `Duration` without `java.net.http.*` prefixes, and contains no `import old.*`

### Requirement: Non-success HTTP status throws HttpException

`Request.sendAsync()` SHALL fail exceptionally with `HttpException` when receiving any HTTP response status code that is `< 200` or `>= 400`. Response status codes in the range `200..399` SHALL be treated as successful and processed by the configured `BodyHandler<T>`.

#### Scenario: Status code 4xx throws HttpException with JSON error body
- **WHEN** an HTTP request completes with a status code of 400, 401, 403, or 404 and the response body is valid JSON
- **THEN** `sendAsync()` completes exceptionally with an `HttpException`
- **AND** `HttpException.statusCode()` returns the response status code
- **AND** `HttpException.errorBody()` returns a Jackson `JsonNode` representing the parsed JSON structure

#### Scenario: Status code 5xx throws HttpException with text fallback
- **WHEN** an HTTP request completes with a status code of 500 or 502 and the response body is plain text or HTML
- **THEN** `sendAsync()` completes exceptionally with an `HttpException`
- **AND** `HttpException.statusCode()` returns the response status code
- **AND** `HttpException.errorBody()` returns a Jackson `TextNode` containing the raw response string
- **AND** `HttpException.rawBody()` returns the raw response string

#### Scenario: Empty error body produces null errorBody
- **WHEN** an HTTP request completes with a status code >= 400 and an empty response body
- **THEN** `sendAsync()` completes exceptionally with an `HttpException`
- **AND** `HttpException.errorBody()` is `null`
- **AND** `HttpException.rawBody()` is `""` or `null`

#### Scenario: Status codes 2xx and 3xx succeed normally
- **WHEN** an HTTP request completes with a status code between 200 and 399
- **THEN** `sendAsync()` completes successfully returning a `Response<T>` containing the parsed body

**Source: api-models**

Self-contained DTOs and JSON utilities under `mindustrytool.*` that support instance-based `Request` clients without depending on `old.*`.
### Requirement: Self-contained models under mindustrytool.models

All DTOs used by `MindustryTool` and `Github` SHALL be copied from `src/old` into `src/mindustrytool/models` with package `mindustrytool.models` and MUST NOT import `old.*`. New auth code SHALL use `mindustrytool.models.UserSession` etc., copied from old, with no `old.` reference in `src/mindustrytool`.

#### Scenario: Models exist in new package
- **WHEN** `src/mindustrytool/models` is listed
- **THEN** it contains `MapData`, `MapDetailData`, `SchematicData`, `SchematicDetailData`, `TagCategory`, `TagData`, `UserData`, `ModData`, `ServerData`, `PlayerConnectRoom`, `PlayerConnectProvider`, `ChannelDto`, `ChatMessage`, `ChatUser`, `UserSession`, `AuthTokenResponse`, `LoginUriResponse`, `TaskData`, `TaskResponse`, `Sort`, and any other DTOs required by retained endpoints, each with `package mindustrytool.models`

#### Scenario: No old imports in new code
- **WHEN** `grep -R "old\.mindustrytool" src/mindustrytool` or `Select-String` is run
- **THEN** it returns no results (except comments); all `src/mindustrytool/**/*.java` compile without `import old.*`

### Requirement: DTO fidelity and Jackson compatibility

Copied DTOs SHALL preserve field names and Jackson annotations to parse actual API JSON (`FAIL_ON_UNKNOWN_PROPERTIES=false`).

#### Scenario: DTO parses sample API JSON
- **WHEN** sample JSON for `MapData` from `GET /maps/{id}` is deserialized via `JsonUtils.fromJson(MapData.class, json)`
- **THEN** all required fields populate and unknown fields are ignored without exception

#### Scenario: Inner ServerDto extracted
- **WHEN** `ServerService` inner `ServerDto` is inspected
- **THEN** a top-level `mindustrytool.models.ServerData` exists with `id`, `name`, `address`, `port`, `status` and Lombok `@Data` or equivalent

### Requirement: Copied JSON utility in new codebase

A new `mindustrytool.utils.JsonUtils` SHALL be copied from `old.mindustrytool.Utils` JSON methods (Jackson `ObjectMapper` with `JavaTimeModule`, `toJson`, `fromJson`, `fromJsonArray`) and expose static helpers for the new services. It SHALL be the only JSON helper used by `MindustryTool`/`Github`/`Request` callers in `src/mindustrytool`.

#### Scenario: JsonUtils handles DTO parsing
- **WHEN** `JsonUtils.fromJson(MapDetailData.class, json)` is called
- **THEN** it returns a deserialized instance using a shared `ObjectMapper` configured with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES=false` and `JavaTimeModule`

#### Scenario: Json array parsing for lists
- **WHEN** `JsonUtils.fromJsonArray(TagData.class, jsonArray)` is called
- **THEN** it returns `List<TagData>` via `readerForListOf`

#### Scenario: No old.Utils import in new services
- **WHEN** `MindustryTool.java`, `Github.java`, `AuthService`, `AuthOverlay`, `MindustryAuthProvider`, and `Request.java` are inspected
- **THEN** they import `mindustrytool.utils.JsonUtils` and not `old.mindustrytool.Utils`; the only `Jval` usage allowed is for JWT `exp` parsing in `MindustryAuthProvider` (via `arc.util.serialization.Jval`)

### Requirement: AuthProvider and MindustryAuthProvider are self-contained

`AuthProvider` and `mindustrytool.services.MindustryAuthProvider` (preserved) SHALL live in `mindustrytool.services`, implement Bearer-only auth, and NOT import `old.mindustrytool.features.auth.*`. New `mindustrytool.services.AuthService` (logic) and `mindustrytool.ui.AuthOverlay` (UI) SHALL also NOT import `old.*` and SHALL reuse `mindustrytool.auth.*` keys via `Core.settings` for interop. All auth DTOs/events used (`UserSession`, `AuthTokenResponse`, `LoginUriResponse`, `SessionLoadEvent`, `LoginEvent`, `LogoutEvent`) SHALL live in `mindustrytool.models`/`mindustrytool.events` with package `mindustrytool.models`/`events` and no `old.` reference.

#### Scenario: Preserved provider and new services have no old imports
- **WHEN** `src/mindustrytool/services/AuthProvider.java`, `MindustryAuthProvider.java`, `AuthService.java`, and `mindustrytool/ui/AuthOverlay.java` are inspected
- **THEN** they import only `java.*`, `arc.*`, `mindustrytool.Config`, `mindustrytool.services.Request` (provider/overlay only), `mindustrytool.services.MindustryTool`, `mindustrytool.models.*`/`events.*`, `mindustrytool.utils.JsonUtils` (if needed), and contain no `import old.`

#### Scenario: MindustryAuthProvider still owns instance Request and refresh
- **WHEN** `MindustryAuthProvider` is inspected
- **THEN** it still declares `private final Request api = Request.builder().baseUrl(Config.API_URL).authProvider(this).build()` and deduplicates refresh via `synchronized refreshIfNeeded()` with `CompletableFuture<Void> refreshFuture`; `AuthService` does NOT declare that `Request api` nor `refreshFuture` for token refresh

#### Scenario: Auth events/DTOs are self-contained
- **WHEN** `src/mindustrytool/models/UserSession.java`, `AuthTokenResponse.java`, `LoginUriResponse.java` and `src/mindustrytool/events/SessionLoadEvent.java` etc. are listed
- **THEN** each has `package mindustrytool.models` or `mindustrytool.events` and contains no `import old.*`; `AuthService`/`AuthOverlay` import the new packages

**Source: auth-service**

TBD - created by archiving change rewrite-auth-service. Update Purpose after archive.
### Requirement: AuthService logic split from UI
`mindustrytool.services.AuthService` SHALL be a pure-logic singleton with no Arc scene/UI imports, owning `UserSession currentSession`, `CompletableFuture<Void> loginFuture`, `KEY_*` constants (`mindustrytool.auth.access-token`, `mindustrytool.auth.refresh-token`, `mindustrytool.auth.login-id`, `mindustrytool.auth.login-expiry`), and delegating every HTTP call through `mindustrytool.services.MindustryTool` typed methods (`getSession`, `getLoginUri`, `pollLoginToken`, `logout`) rather than constructing `Request` directly. `mindustrytool.ui.AuthOverlay` (and `mindustrytool.ui.AuthLoginDialog`) SHALL be the sole UI owners of `authWindow`/`wholeViewport` and dialog rendering. `MindustryAuthProvider` SHALL remain preserved unchanged as the sole `AuthProvider` and `MindustryTool.api` wiring `authProvider(MindustryAuthProvider.getInstance())` SHALL NOT change.

#### Scenario: AuthService has no UI imports and no Request for auth endpoints
- **WHEN** `src/mindustrytool/services/AuthService.java` is inspected
- **THEN** it contains no `import arc.scene.*`, no `import mindustry.ui.*`, no `import mindustrytool.ui.*`, and contains no `api.get("auth/app/login-uri")`, `api.get("auth/app/login-token")`, `api.post("auth/app/refresh")`, `api.post("auth/app/logout")`; all such calls go via `MindustryTool.*`

#### Scenario: AuthOverlay owns UI and AuthService owns logic
- **WHEN** `src/mindustrytool/ui/AuthOverlay.java` and `AuthLoginDialog.java` are inspected
- **THEN** `AuthOverlay` owns `Table authWindow`/`wholeViewport` and `AuthLoginDialog loginDialog`, while `AuthService` owns `currentSession` and `loginFuture`; `AuthService` does not reference `AuthOverlay` and `AuthOverlay` depends one-way on `AuthService`

#### Scenario: MindustryAuthProvider preserved and wired
- **WHEN** `src/mindustrytool/services/MindustryAuthProvider.java` is inspected and `MindustryTool.java` is inspected
- **THEN** `MindustryAuthProvider` still declares `private final Request api = Request.builder().baseUrl(Config.API_URL).authProvider(this).build()` and `synchronized refreshIfNeeded()` unchanged, and `MindustryTool` still contains `Request.builder().baseUrl(Config.API_URL).authProvider(MindustryAuthProvider.getInstance()).build()` for `api`; `AuthService` does NOT implement `AuthProvider` and does NOT appear as provider

#### Scenario: No old imports and no delegation shim
- **WHEN** `grep -R "old\.mindustrytool" src/mindustrytool` and `Select-String` are run
- **THEN** they return no results and `src/old/mindustrytool/features/auth/AuthService.java` does not exist (deleted without delegating wrapper)

### Requirement: AuthService session and event orchestration via MindustryTool
`AuthService` SHALL maintain `UserSession currentSession` cache, expose `UserSession getSession()` and `CompletableFuture<UserSession> fetchSession()` delegating to `MindustryTool.getSession()`, fire `SessionLoadEvent(user, error, isLoading)` (loading=true before request, then success/error on `Core.app.post`), fire raw `UserSession` event on non-null success, fire `LoginEvent` after successful token poll+fetch, fire `LogoutEvent` on logout, and expose `isLoggedIn()` as `Core.settings.has(KEY_ACCESS_TOKEN) && Core.settings.has(KEY_REFRESH_TOKEN)` (settings-only).

#### Scenario: fetchSession emits loading and result events
- **WHEN** `fetchSession()` is called
- **THEN** it posts `SessionLoadEvent(currentSession, null, true)` on `Core.app.post`, then on completion posts `SessionLoadEvent(finalSession, null, false)` on success or `SessionLoadEvent(currentSession, cause, false)` on error, and on non-null session also calls `Events.fire(session)`

#### Scenario: isLoggedIn reflects settings only
- **WHEN** `saveTokens` has been called but `fetchSession` has not yet completed
- **THEN** `isLoggedIn()` returns true because both settings keys exist

### Requirement: AuthService login flow and deduplication via MindustryTool and UI overlay
`AuthService` SHALL implement `synchronized CompletableFuture<Void> login()` with deduplicated `loginFuture`, persisting `KEY_LOGIN_ID`/`KEY_LOGIN_EXPIRY` (`+5m`), and delegating HTTP to `MindustryTool.getLoginUri()` and `MindustryTool.pollLoginToken(loginId)` (60s timeout). It SHALL coordinate with `AuthOverlay`/`AuthLoginDialog` to show `showLoading()` then `showLoginUrl(loginUrl)` on `Core.app.post`, open `Core.app.openURI(loginUrl)` fallback to `Core.app.setClipboardText(loginUrl)`, and expose `void cancelLogin()` completing `loginFuture` exceptionally. Token poll on failure removes `KEY_LOGIN_ID` and hides dialog via overlay.

#### Scenario: Login delegates to MindustryTool not raw Request
- **WHEN** `login()` is called
- **THEN** it calls `MindustryTool.getLoginUri()`, stores `loginId`/`loginExpiry`, opens URI, then calls `MindustryTool.pollLoginToken(loginId)` and on success `saveTokens` + `fetchSession` + `LoginEvent`; concurrent calls return same `loginFuture` and only one `getLoginUri()` is initiated

#### Scenario: Concurrent login returns same future
- **WHEN** `login()` is called twice before first completes
- **THEN** second call returns same `CompletableFuture<Void>` instance

#### Scenario: cancelLogin aborts pending login
- **WHEN** `loginFuture` pending and `cancelLogin()` called
- **THEN** `loginFuture` completes exceptionally with "Login cancelled"

### Requirement: AuthService init and periodic refresh (logic only)
`AuthService.init()` SHALL call `fetchSession()` immediately, schedule `Timer.schedule(() -> if(isLoggedIn()) fetchSession(), 300, 300)` every 5 minutes, and resume unexpired `KEY_LOGIN_ID` via `pollLoginToken(loginId)` in background (remove if expired). It SHALL NOT build `authWindow` or subscribe to `SessionLoadEvent` for rendering; that is `AuthOverlay` responsibility.

#### Scenario: Resume login on startup
- **WHEN** `Core.settings` contains `KEY_LOGIN_ID` with future `KEY_LOGIN_EXPIRY` at `init()` time
- **THEN** `pollLoginToken(loginId)` is called in background; **WHEN** expiry is past **THEN** both keys are removed and no poll occurs

#### Scenario: Periodic session refresh
- **WHEN** `init()` has completed and 5 minutes elapse while `isLoggedIn()` true
- **THEN** `fetchSession()` is invoked

### Requirement: AuthOverlay UI lifecycle
`mindustrytool.ui.AuthOverlay` SHALL expose `initUi()`/`init()` building `wholeViewport` fillParent top-right, `content` with `Styles.black6`, `authWindow.touchable = Touchable.childrenOnly`, adding to `Vars.ui.menuGroup` via `Core.app.post`, null-guarded for headless, and subscribing `Events.on(SessionLoadEvent.class, ...)` to render: `auth.session.loading` label, `auth.session.error` + message + `auth.session.retry` button → `AuthService.login()`, guest `auth.login` button, or authenticated avatar (`NetworkImage` 64) + name (desktop only) click → confirm dialog `auth.logout.confirm-title`/`auth.logout.confirm-message` (`{0}` player name) → `AuthService.logout()`. `AuthLoginDialog` SHALL display `auth.login.dialog-title`, `auth.login.cancel`, `auth.login.loading` label, and `auth.login.login-url` button with clipboard `auth.login.copied` toast, using `Core.bundle.get/format("auth.*")` only.

#### Scenario: AuthOverlay renders SessionLoadEvent states with auth.* keys
- **WHEN** `SessionLoadEvent(isLoading=true)` fired
- **THEN** `authWindow` content clears and shows `Core.bundle.get("auth.session.loading")`; **WHEN** error **THEN** `auth.session.error` + message + `auth.session.retry` button; **WHEN** user==null **THEN** `auth.login` button; **WHEN** user != null **THEN** avatar (if `imageUrl`) + name + click→confirm with `auth.logout.confirm-*` via `Core.bundle.format`

#### Scenario: AuthLoginDialog uses auth.* keys
- **WHEN** `AuthLoginDialog` is inspected
- **THEN** it uses `Core.bundle.get("auth.login.dialog-title")`, `get("auth.login.cancel")`, `get("auth.login.loading")`, `get("auth.login.copied")` and no `@login`/`@loading` generic keys nor hardcoded strings

#### Scenario: Headless init does not crash
- **WHEN** `AuthOverlay.init()` called with `Vars.ui == null`
- **THEN** it skips `authWindow`/`menuGroup` setup without exception; `AuthService.init()` still performs `fetchSession()`, timer, and loginId resume

### Requirement: Auth i18n namespace
All user-visible auth text SHALL be under `auth.*` keys in `assets/bundles/bundle.properties` with a mandatory comment directly above each key explaining where displayed, when used, and placeholder meanings (`{0}` etc.). Legacy generic keys (`@login`, `@loading`, `@error`, `@retry`, `@generate-loading-link`, `@copied`) SHALL NOT be used by auth after this change; they are cleared and replaced by `auth.*` equivalents. Dynamic text SHALL use `Core.bundle.format` with placeholders, not string concatenation.

#### Scenario: New keys exist with comments
- **WHEN** `assets/bundles/bundle.properties` is grepped for `^auth\.`
- **THEN** it contains at least `auth.login`, `auth.login.dialog-title`, `auth.login.cancel`, `auth.login.loading`, `auth.login.login-url`, `auth.login.copied`, `auth.login.success`, `auth.login.failed`, `auth.logout`, `auth.logout.confirm-title`, `auth.logout.confirm-message`, `auth.session.loading`, `auth.session.error`, `auth.session.retry`, each with a directly-above comment describing context and placeholders; **WHEN** `AuthService.java`/`AuthOverlay.java` are inspected **THEN** they contain `Core.bundle.get("auth.*")`/`format` and no hardcoded display strings like `"Login successful!"` or `"Logged in as "`

#### Scenario: No hardcoded user-visible text in AuthService/AuthOverlay
- **WHEN** `src/mindustrytool/services/AuthService.java` and `src/mindustrytool/ui/AuthOverlay.java` are inspected
- **THEN** they contain no string literals for UI messages outside `Core.bundle.get/format`; `Vars.ui.showConfirm` uses `Core.bundle.format("auth.logout.confirm-message", userName)` etc.

### Requirement: AuthService self-containment and import hygiene
`AuthService` and `AuthOverlay` SHALL import only `java.*`, `arc.*`, `mindustrytool.Config`, `mindustrytool.services.Request` (overlay only if needed), `mindustrytool.services.MindustryTool`, `mindustrytool.services.MindustryAuthProvider` (if token access needed), `mindustrytool.utils.JsonUtils` (if needed), `mindustrytool.models.*`/`events.*`, and SHALL NOT import `old.mindustrytool.*`. They SHALL use `Config.API_URL` (not `API_v4_URL`) and be safe when `Vars.ui` is null.

#### Scenario: No old imports and correct baseUrl
- **WHEN** `grep -R "old\.mindustrytool" src/mindustrytool/services/AuthService.java src/mindustrytool/ui/AuthOverlay.java` is run
- **THEN** returns no results and files reference `Config.API_URL` only

**Source: auth-session-signal**

Provides a single reactive source of truth for auth session state in `MindustryAuthProvider` — session identity, loading, and error signals with a main-thread update contract — replacing session broadcast events and token-snapshot re-derivation in consumers.

### Requirement: Reactive session source of truth
`MindustryAuthProvider` SHALL expose session identity as a reactive `Signal` (null when no session is loaded) alongside loading and error signals, and SHALL mutate all three exclusively on the main thread. The session broadcast events (`SessionLoadEvent`, `LoginEvent`, `LogoutEvent`) and the unlistened raw session fire SHALL be removed.

#### Scenario: Session fetch publishes through signals on the main thread
- **WHEN** `fetchSession()` completes successfully
- **THEN** the session signal holds the fetched session, loading is false, and all updates were applied on the main thread

#### Scenario: No broadcast events are fired
- **WHEN** session loads, login completes, or logout occurs
- **THEN** no `SessionLoadEvent`, `LoginEvent`, or `LogoutEvent` is fired and no raw session object is fired as an event

### Requirement: Loading and error session states
The provider SHALL expose reactive loading and error states for the session lifecycle: loading is true while a fetch is in flight, and error carries the fetch failure while preserving the previous session.

#### Scenario: Fetch failure preserves session and reports error
- **WHEN** `fetchSession()` fails
- **THEN** the error signal holds the failure, loading is false, and the session signal still holds the previous session

### Requirement: Chat login state derived from session
`ChatStore.loggedIn` SHALL be derived from the session signal (logged in if and only if a session is present) instead of token snapshots and event handlers.

#### Scenario: Cold start with valid tokens
- **WHEN** the app starts with stored tokens but the session fetch has not completed
- **THEN** chat login-gated UI treats the user as logged out until the session arrives

#### Scenario: Session arrival updates chat gating
- **WHEN** the session signal receives a session
- **THEN** chat login-gated UI treats the user as logged in without any event handling

### Requirement: Auth overlay state derived from session signals
`AuthOverlay` SHALL derive its displayed state (loading / error / login prompt / user card) from the session signals without subscribing to session events, rendering identically to before for each state.

#### Scenario: Overlay reflects signal states
- **WHEN** the session signals indicate loading, error, absence, or presence of a session
- **THEN** the overlay shows the loading indicator, error card with retry, login button, or user card respectively

### Requirement: Logout clears session without refetch
`logout()` SHALL remove stored tokens and clear the session signal (and error) directly instead of triggering a session fetch.

#### Scenario: Logout shows login prompt
- **WHEN** the user logs out
- **THEN** the session signal is null, no error state is produced, and the overlay shows the login button

### Requirement: Non-reactive session peek for the chat parser
The provider SHALL offer a non-subscribing session read for non-reactive contexts, and the chat message parser SHALL use it for mention detection and have its cache invalidated when the session changes.

#### Scenario: Late login fixes future mention parses
- **WHEN** messages were parsed while no session was present and a session subsequently arrives
- **THEN** the parser cache is invalidated so subsequent parses evaluate mentions against the current user

**Source: github-service**

External GitHub/project API facade that delegates all HTTP to instance-based `Request` clients without authentication.

### Requirement: Github service owns only external endpoints and delegates to instance Request

`Github` SHALL own only `Config.GITHUB_API_URL`, `Config.MOD_HJSON_URL`, and `Config.PROJECT_URL` endpoints, delegate every call to instance `Request` (`githubApi`, `projectApi`, `rawApi` built via `Request.builder()`) and contain no direct `HttpClient` construction and no auth provider.

#### Scenario: Github delegates to instance Request
- **WHEN** `src/mindustrytool/services/Github.java` is inspected
- **THEN** it declares `private static final Request githubApi = Request.builder().baseUrl(Config.GITHUB_API_URL).build()`, `projectApi` for `Config.PROJECT_URL`, and `rawApi` for absolute URLs; each method calls `githubApi.get("").sendAsync()` or `rawApi.get(Config.MOD_HJSON_URL).sendAsync()` / `rawApi.get(Config.GITHUB_API_URL + "?page=" + page + "...").sendAsync()` and maps `HttpResponse<String>.body()` via `JsonUtils` or returns String, with no `HttpClient`/`HttpRequest` instantiation

#### Scenario: No MindustryTool API URLs in Github
- **WHEN** `Github.java` URLs are listed
- **THEN** none start with `Config.API_URL`; all use `GITHUB_API_URL`, `MOD_HJSON_URL`, or `PROJECT_URL`

#### Scenario: No auth provider and no old imports
- **WHEN** `Github.java` is inspected
- **THEN** all three `Request` instances are built without `.authProvider(...)` and the file contains no `import old.*`

### Requirement: Typed returns for Github and project calls

`Github` methods SHALL return typed futures where applicable (e.g. release list parsed to DTO) instead of raw string where a model exists, with fallback to `String` for raw `mod.hjson`.

#### Scenario: getReleases returns String (raw) with rationale
- **WHEN** `Github.getReleases()` is called
- **THEN** it delegates to `githubApi.get("").sendAsync()` or `rawApi.get(GITHUB_API_URL + "?page=...").sendAsync()` and returns `CompletableFuture<String>` (raw mod.hjson/releases are heterogeneous and kept as String with Javadoc rationale)

#### Scenario: getProjectTasks returns typed response
- **WHEN** `Github.getProjectTasks(status)` is called
- **THEN** it calls `projectApi.get("/api/v1/projects/" + Config.PROJECT_ID + "/tasks?status=" + status).timeout(Duration.ofMillis(20000)).sendAsync()` and parses via `JsonUtils.fromJson(TaskResponse.class, body)`

### Requirement: Import hygiene for Github

`Github` SHALL use explicit imports for `Duration`, `CompletableFuture`, `mindustrytool.Config`, `mindustrytool.utils.JsonUtils`, `mindustrytool.models.TaskResponse` and SHALL NOT use fully-qualified inline names or `import static mindustrytool.services.Request.*`.

#### Scenario: No static Request import
- **WHEN** `Github.java` imports are inspected
- **THEN** they contain `import mindustrytool.services.Request` and no `import static ...Request.*`, and no `import old.*`

#### Scenario: Explicit timeout handling
- **WHEN** `getProjectTasks` needs 20s timeout
- **THEN** it uses `.timeout(Duration.ofMillis(20_000))` on the builder, not `int timeoutMs` parameter on static helper

**Source: mindustrytool-api**

Typed `mindustrytool.services.MindustryTool` API facade over `Config.API_URL` that delegates all HTTP to instance-based `Request` clients and parses JSON via copied `mindustrytool.utils.JsonUtils`.

### Requirement: MindustryTool delegates all HTTP to instance Request and uses only Config.API_URL

`MindustryTool` SHALL contain only methods that target `Config.API_URL`; it MUST NOT construct `HttpClient`/`HttpRequest` directly and SHALL delegate every call via instance `Request` (`api` with `MindustryAuthProvider`, `publicApi` without auth) using fluent `get/post/put/delete(...).header/timeout/json/bytes/withoutAuth().sendAsync()`.

#### Scenario: No direct HttpClient construction in MindustryTool
- **WHEN** `src/mindustrytool/services/MindustryTool.java` is inspected
- **THEN** it contains no `HttpClient.newBuilder`, no `HttpRequest.newBuilder`, and no inline `java.net.http.*` fully-qualified construction; all HTTP goes through `private static final Request api` and `publicApi` built via `Request.builder()`

#### Scenario: Auth vs public client split
- **WHEN** `MindustryTool` methods are listed
- **THEN** chat (`/chats/*` with `api`), `getSession` (`api.get("/auth/session")`), and similar use the authenticated `api`; `ping`, `maps/*`, `schematics/*`, `tags`, `users/batches`, `planets`, `servers`, `player-connect/*`, images, `submitCrashReport`, and unauth auth endpoints (`/auth/app/login-uri`, `/auth/app/login-token`, `/auth/app/refresh`, `/auth/app/logout` via `publicApi` with `withoutAuth`) use `publicApi`

#### Scenario: Chat stream and logout use Request with proper headers
- **WHEN** `chatStream(chatId)` is implemented
- **THEN** it uses `api.get("/chats/stream").header("Accept","text/event-stream").header("x-chat-id",chatId).timeout(Duration.ofMillis(0)).sendAsync(BodyHandlers.ofLines())` with a `SubmissionPublisher`; **WHEN** `logout(accessToken,refreshToken)` is implemented **THEN** it uses `publicApi.post("/auth/app/logout").withoutAuth().header("Authorization","Bearer "+accessToken).json(json).sendAsync()` to avoid refresh recursion

#### Scenario: Only API_URL endpoints remain
- **WHEN** all methods in `MindustryTool` are listed
- **THEN** every URL is relative (`/ping`, `/maps/...`) resolved against `Config.API_URL` or absolute `Config.*_URL` that equals `API_URL`; no GitHub or `PROJECT_URL` endpoints appear

### Requirement: Typed DTO returns via JsonUtils

`MindustryTool` methods SHALL return `CompletableFuture<DTO>` or `CompletableFuture<List<DTO>>` (or `CompletableFuture<byte[]>` for binary) by mapping `HttpResponse<String>` through `mindustrytool.utils.JsonUtils` instead of returning `CompletableFuture<String>`.

#### Scenario: Find map returns MapDetailData
- **WHEN** `MindustryTool.findMap(id)` is called
- **THEN** it calls `publicApi.get("/maps/"+id).sendAsync()` and maps `response.body()` via `JsonUtils.fromJson(MapDetailData.class, body)` to `CompletableFuture<MapDetailData>`

#### Scenario: Search maps returns List<MapData>
- **WHEN** `MindustryTool.searchMaps(page,size,sort,query,tags)` is called
- **THEN** it builds a paged URL and returns `CompletableFuture<List<MapData>>` via `JsonUtils.fromJsonArray(MapData.class, body)`

#### Scenario: Tags and users return typed lists
- **WHEN** `getTags` or `getUserBatch` is called
- **THEN** they return `CompletableFuture<List<TagCategory>>` / `List<UserData>` parsed from JSON, not raw string

#### Scenario: Binary endpoints remain byte[]
- **WHEN** `downloadMap` or `downloadSchematic` is called
- **THEN** they use `publicApi.get(...).timeout(LONG_TIMEOUT).sendAsync(BodyHandlers.ofByteArray())` and return `CompletableFuture<byte[]>`

### Requirement: No storage or translation endpoints

`MindustryTool` SHALL NOT contain storage (`/storage/*`) or translation (`/translations/translate`) endpoints.

#### Scenario: No storage methods
- **WHEN** `MindustryTool.java` is searched for `storageUrl`, `listStorageSlots`, `uploadFile`
- **THEN** no matches are found

#### Scenario: No translation methods
- **WHEN** `MindustryTool.java` is searched for `translate`
- **THEN** no method returns a translation endpoint

### Requirement: Import hygiene for MindustryTool

`MindustryTool` SHALL use explicit imports for `BodyHandlers`, `Duration`, `URLEncoder`, `StandardCharsets`, `Config`, `JsonUtils`, `models.*` and SHALL NOT use fully-qualified inline names for `old.*` and SHALL NOT `import old.mindustrytool.*` or `import static mindustrytool.services.Request.*`.

#### Scenario: No old imports and no static Request import
- **WHEN** `src/mindustrytool/services/MindustryTool.java` is inspected
- **THEN** it contains `import mindustrytool.services.Request`, `MindustryAuthProvider`, `AuthProvider` as needed, and no `import old.*`, no `import static ...Request.*`, and no `java.net.http.HttpRequest` direct construction

#### Scenario: Explicit model imports
- **WHEN** imports are listed
- **THEN** they include `ChannelDto`, `ChatMessage`, `ChatUser`, `AuthTokenResponse`, `LoginUriResponse`, `MapData`, `MapDetailData`, `ModData`, `PlayerConnectRoom/Provider`, `SchematicData/Detail`, `ServerData`, `TagCategory`, `UserData`, `UserSession` without wildcards

### Requirement: Retained endpoints stay complete with instance Request semantics

`MindustryTool` SHALL retain typed methods for ping, maps/schematics (find/download/search), tags, user batch, planets, servers, player-connect rooms/providers, chat (channels/messages/users/count/state/stream), auth (session/loginUri/pollLoginToken/logout/refreshToken), crash report, and images (schematic/map image) and implement them via `publicApi`/`api` with per-request `withoutAuth` where required.

#### Scenario: Auth session uses authenticated client automatically
- **WHEN** `getSession()` is called
- **THEN** it uses `api.get("/auth/session").sendAsync()` which triggers `refreshIfNeeded()` and Bearer header automatically

#### Scenario: Login/refresh use withoutAuth
- **WHEN** `getLoginUri()` / `pollLoginToken(loginId)` / `refreshToken(refreshToken)` are called
- **THEN** they use `publicApi.get/post(...).withoutAuth().json(...).sendAsync()` so they do not recurse

#### Scenario: Paged search helper remains
- **WHEN** `searchMaps`/`searchSchematics` build URLs
- **THEN** they use `buildPagedUrl` with `page`, `size<=100`, `sort`, `query`, `tags` encoded via `URLEncoder` with `StandardCharsets.UTF_8`

**Source: request-query-builder**

### Requirement: HTTP Request Builder
The system SHALL provide a fluent `RequestBuilder` for constructing HTTP requests with configurable method, URL, headers, body, timeout, auth, and query parameters. Query parameters SHALL be accumulated via `.query()` overloads and assembled into the final URL at send time. Null query parameter keys SHALL be silently skipped. Null or empty String values SHALL be skipped. The `Map<String, String>` overload SHALL replace existing entries for each key present in the map. All other overloads SHALL append to the key's list.

#### Scenario: Single query parameter
- **WHEN** `request.get("/maps").query("page", 1).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?page=1`

#### Scenario: Multiple query parameters
- **WHEN** `request.get("/maps").query("page", 1).query("size", 20).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?page=1&size=20`

#### Scenario: Repeated query parameter
- **WHEN** `request.get("/maps").query("tags", "a").query("tags", "b").sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?tags=a&tags=b`

#### Scenario: List query parameter
- **WHEN** `request.get("/maps").query("tags", List.of("a", "b")).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?tags=a&tags=b`

#### Scenario: Map query parameter replaces
- **WHEN** `request.get("/maps").query("tags", "a").query(Map.of("tags", "b")).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?tags=b`

#### Scenario: Null key is skipped
- **WHEN** `request.get("/maps").query(null, "value").sendAsync()` is called
- **THEN** the request URL SHALL be `/maps` (no parameter added)

#### Scenario: Null String value is skipped
- **WHEN** `request.get("/maps").query("sort", null).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps` (no `sort` parameter)

#### Scenario: Empty String value is skipped
- **WHEN** `request.get("/maps").query("sort", "").sendAsync()` is called
- **THEN** the request URL SHALL be `/maps` (no `sort` parameter)

#### Scenario: URL encoding
- **WHEN** `request.get("/maps").query("query", "hello world").sendAsync()` is called
- **THEN** the request URL SHALL contain `query=hello+world` or `query=hello%20world`

#### Scenario: Existing query string in URL
- **WHEN** `request.get("/maps?page=0").query("sort", "newest").sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?page=0&sort=newest`

**Source: update-service**

Update-check orchestration, version utilities, changelog formatting, and update dialog under `mindustrytool.update` with SOLID boundaries, instance-based `Request` HTTP, bundle-backed i18n, and unit-tested utils — replacing legacy `old.mindustrytool.services.UpdateService`.

### Requirement: Update package owns update-check orchestration with SOLID boundaries

`mindustrytool.update` SHALL own all update-check logic previously in `old.mindustrytool.services.UpdateService` and be decomposed into single-responsibility collaborators: `VersionUtils` (pure version math), `ChangelogFormatter` (pure release-to-string), `UpdateClient` (HTTP boundary delegating to `Request`/`Github`), `UpdateView` (dialog/UI), and `UpdateService` (orchestrator). `UpdateService` SHALL depend on collaborator interfaces (DIP) and be constructible via dependency injection, not hard-wired singletons.

#### Scenario: Package structure exists without old imports
- **WHEN** `src/mindustrytool/update/` is listed and each file is inspected
- **THEN** it contains `UpdateService.java`, `VersionUtils.java`, `ChangelogFormatter.java`, `UpdateClient.java` (or `GithubUpdateClient`), `UpdateView.java`, and `UpdateDialog.java`, each with `package mindustrytool.update` and no `import old.*`

#### Scenario: UpdateService depends on interfaces
- **WHEN** `UpdateService.java` fields and constructor are inspected
- **THEN** it holds `UpdateClient`/`ChangelogFormatter`/`UpdateView` (interfaces) injected via constructor, and contains no direct `HttpClient`/`HttpRequest` construction and no inline `arc.util.Http`

#### Scenario: Single-responsibility class split
- **WHEN** responsibilities are mapped
- **THEN** `VersionUtils` contains only `parseVersion`/`compare`/`format` statics, `ChangelogFormatter` only formats changelog text, `UpdateClient` only performs HTTP fetches, `UpdateView` only shows UI, and `UpdateService` only orchestrates `checkForUpdate`

### Requirement: VersionUtils is pure and covers legacy behavior with bug fix

`VersionUtils` SHALL expose pure static helpers derived from `UpdateService.extractVersionNumber:146`, `isVersionGreater:164`, and `versionToString:176`: `int[] parseVersion(String)` (strips leading `v`, removes suffix after `-`/`+`, keeps digits/dots, splits on `.`, falls back to `int[0]`), `boolean isGreater(int[],int[])` (lexicographic, longer wins), and `String format(int[])` (dot-join). It SHALL fix the `indexOf("v")`/`indexOf("-")` crash when version contains `v` without `-` and SHALL have no Arc/Mindustry dependencies.

#### Scenario: parseVersion handles v-prefix and suffix
- **WHEN** `parseVersion("v8-136")` is called
- **THEN** it returns `int[]{8,136}` or `int[]{8}` depending on stripping rule (documented), and `parseVersion("1.2.3-beta")` returns `int[]{1,2,3}`

#### Scenario: parseVersion is null/empty/invalid safe
- **WHEN** `parseVersion(null)` or `parseVersion("")` or `parseVersion("abc")` is called
- **THEN** it returns `int[0]` without throwing

#### Scenario: isGreater compares lexicographically and by length
- **WHEN** `isGreater(new int[]{1,3,0}, new int[]{1,2,9})` then `isGreater(new int[]{1,2}, new int[]{1,2,0})` then `isGreater(new int[]{1,2}, new int[]{1,2})`
- **THEN** results are `true`, `false` (shorter is not greater), `false` (equal)

#### Scenario: format round-trips
- **WHEN** `format(new int[]{1,2,3})` is called
- **THEN** it returns `"1.2.3"` and `format(new int[0])` returns `""`

### Requirement: HTTP via instance Request delegation

Update HTTP SHALL be performed only via instance-based `mindustrytool.services.Request` (through `Github` or `MindustryTool`), not `arc.util.Http` or direct `java.net.http.HttpClient` inside `UpdateService`. Version is fetched from `Config.MOD_HJSON_URL`, releases from `Config.GITHUB_API_URL` (via `Github.getModHjson()`/`Github.getReleases()` or equivalent `UpdateClient` methods), and ping via `MindustryTool.ping("mod-v8")`. On the beta channel (beta participation enabled), `UpdateService` SHALL skip the `mod.hjson` fetch and derive the latest version solely from the releases list as the maximum tag over all entries, and the beta update action SHALL install that exact tag via the `githubImportMod(repo, isJava, release, forceEnable)` overload.

#### Scenario: No direct HttpClient in update package
- **WHEN** `src/mindustrytool/update/*.java` are searched for `HttpClient.newBuilder`, `HttpRequest.newBuilder`, and `arc.util.Http`
- **THEN** no matches are found; all HTTP goes through `Github`/`MindustryTool`/`Request.builder()`

#### Scenario: Endpoints are correct
- **WHEN** `UpdateClient`/`GithubUpdateClient` is inspected
- **THEN** it references `Config.MOD_HJSON_URL` for version and `Config.GITHUB_API_URL` for releases, and no `Config.API_URL` GitHub confusion (per `github-service` spec)

#### Scenario: Error paths invoke done and show dialog
- **WHEN** release fetch fails (non-2xx) or JSON parse fails
- **THEN** `UpdateService` posts to `Core.app.post` and shows `UpdateView` with an error changelog string from bundle keys, then invokes the `done` runnable exactly as legacy `fetchReleasesAndShowDialog:70` does

#### Scenario: Beta channel derives latest from releases only
- **WHEN** beta participation is enabled and the releases list is fetched successfully
- **THEN** no `mod.hjson` fetch is performed and the latest version is the maximum release tag over all entries (stable and prerelease)

#### Scenario: Beta channel installs the exact winning tag
- **WHEN** the beta path shows an update dialog for a winning prerelease tag
- **THEN** activating Update installs that exact tag via the `githubImportMod(repo, isJava, release, forceEnable)` overload

#### Scenario: Beta channel failures are silent
- **WHEN** beta participation is enabled and the releases fetch fails or yields no usable entries
- **THEN** the failure is logged, the `done` runnable is still invoked, and no dialog of any kind is shown

### Requirement: ChangelogFormatter is pure and bundle-aware

`ChangelogFormatter` SHALL format up to 20 releases into Mindustry markup: `[accent]tag[white]`, optional `yyyy-MM-dd HH:mm` date in `ZoneId.systemDefault()`, `[gold]` download-count line from summed `assets[].download_count`, and `renderMarkdown(body)` conversion (links→`[sky]`, headers→`[accent]`, lists→`•`, bold/italic/code). Labels SHALL be bundle keys, not hard-coded English.

#### Scenario: Pure formatting without Arc runtime
- **WHEN** `ChangelogFormatter.format(releases)` is called with a parsed release list
- **THEN** it returns a `String` without touching `Core.app` or network, and caps output at 20 releases

#### Scenario: Markdown transforms preserved
- **WHEN** body contains `**bold**`, `*italic*`, `` `code` ``, `[text](url)`, `## header`, `- item`
- **THEN** output contains `[white]bold[white]`, `[lightgray]italic[white]`, `[cyan]code[white]`, `[sky]text[white]`, `[accent]header[white]`, `• item` respectively

### Requirement: Update dialog uses bundle keys for all user-visible text

`UpdateDialog` (refactored from `UpdateAvailableDialog:13`) SHALL use `Core.bundle.get`/`format` for every user-visible string and SHALL NOT contain hard-coded English literals for titles, buttons, or messages. All new keys SHALL be added to `assets/bundles/bundle.properties` per `AGENTS.md` key conventions (`update.*`).

#### Scenario: Dialog has no hard-coded display strings
- **WHEN** `UpdateDialog.java` string literals are inspected
- **THEN** title `"Update Available"`, button labels `"Cancel"`/`"Update"`, and status lines use `Core.bundle.get("update.dialog.title")`, `Core.bundle.get("update.button.cancel")`, etc., and `Core.bundle.format("update.message.new-version", currentVer, latestVer)` for the version banner

#### Scenario: Bundle keys exist and follow naming
- **WHEN** `assets/bundles/bundle.properties` is inspected
- **THEN** it contains keys such as `update.dialog.title=Update Available`, `update.message.new-version=…{0}…{1}…`, `update.button.cancel=Cancel`, `update.button.update=Update`, `update.changelog.download-count=Download count: {0}`, `update.error.fetch-releases=Could not fetch release notes.`, `update.error.fetch-releases-with-status=Could not fetch release notes: {0}`, `update.error.parse-releases=Could not parse release notes.`, `update.label.no-description=No description provided.` and all use lowercase dot-separated `update.*` names

#### Scenario: Dynamic values use format
- **WHEN** a message contains `currentVer`/`latestVer` or `downloadCount`
- **THEN** it uses `Core.bundle.format("update.*", value)` not string concatenation of translated fragments

### Requirement: Unit tests cover VersionUtils (and formatter)

The change SHALL ship JUnit 5 unit tests for `VersionUtils` (required) and `ChangelogFormatter` markdown helpers (recommended) that run via `./gradlew test` without a Mindustry runtime. Tests SHALL live under `src/test/java/mindustrytool/update/` (or project-standard test source set).

#### Scenario: VersionUtilsTest covers edge cases
- **WHEN** `./gradlew test --tests "mindustrytool.update.VersionUtilsTest"` runs
- **THEN** tests for `parseVersion` (null, empty, `"v8"`, `"1.2.3"`, `"v8-1.0"`, `"abc"`, `"1..2"`), `isGreater` (equal, greater major/minor, longer array), and `format` (empty, single, multi) all pass

#### Scenario: Tests do not require Arc/Core
- **WHEN** `VersionUtilsTest.java` imports are inspected
- **THEN** it imports only `org.junit.jupiter.api.*` and `mindustrytool.update.VersionUtils`, with no `arc.*` or `mindustry.*`

#### Scenario: Build is configured for JUnit 5
- **WHEN** `build.gradle:48` dependencies are inspected
- **THEN** it declares `testImplementation 'org.junit.jupiter:junit-jupiter:…'` and `tasks.test { useJUnitPlatform() }` (or equivalent), and `./gradlew test` succeeds

### Requirement: Legacy UpdateService is retired

After wiring, `src/old/mindustrytool/services/UpdateService.java:1` and `UpdateAvailableDialog.java:1` SHALL be removed or marked `@Deprecated` with Javadoc pointing to `mindustrytool.update.UpdateService`, and `mindustrytool.Main` SHALL not import `old.mindustrytool.services.*` for update checks.

#### Scenario: New Main wires new service
- **WHEN** `src/mindustrytool/Main.java:30` client-load path is inspected
- **THEN** it constructs or obtains `mindustrytool.update.UpdateService` and calls `checkForUpdate(Runnable)` instead of `old.mindustrytool.services.UpdateService.getInstance()`

#### Scenario: No old update import remains in new code
- **WHEN** `grep -R "old\\.mindustrytool\\.services\\.Update" src/mindustrytool` is run
- **THEN** it returns no results

**Source: cache-github-responses**

Github response caching (memoization scope, prefetch, success-only caching with live retry on failure, `getProjectTasks` exclusion).

### Requirement: Github memoizes cacheable responses
`Github.getModHjson()` and `Github.getReleases()` SHALL return a shared memoized future per session: concurrent callers reuse the in-flight request and repeat callers reuse the completed response, issuing at most one network trip per endpoint per session. `Github.getReleases(page, perPage)` SHALL be memoized per `(page, perPage)` key on demand.

#### Scenario: Repeat callers share one response
- **WHEN** `getReleases()` is called twice in one session with a successful first response
- **THEN** only one network request is issued and both callers receive the same body

#### Scenario: Concurrent callers share the in-flight request
- **WHEN** `getModHjson()` is called twice before the first request completes
- **THEN** only one network request is issued and both callers receive the same body

#### Scenario: Paged releases memoized per key
- **WHEN** `getReleases(1, 20)` is called twice with a successful first response
- **THEN** only one network request is issued for that key, and a different key issues its own request

### Requirement: Github prefetches cacheable endpoints at startup
`Github` SHALL expose `prefetchAll()` firing `getModHjson()` and `getReleases()` once, fire-and-forget on the existing `Request` executor without blocking the caller. The application SHALL invoke it once during startup next to the existing update check.

#### Scenario: Prefetch warms the cache
- **WHEN** `prefetchAll()` completes successfully at startup
- **THEN** the subsequent update check's `getModHjson()` and `getReleases()` calls complete without new network requests

#### Scenario: Prefetch never blocks startup
- **WHEN** `prefetchAll()` is invoked
- **THEN** it returns immediately and network work happens off the calling thread

### Requirement: Github caches successes only
Only successfully completed responses SHALL be retained; a failed request SHALL clear its memo holder so the next call re-issues live. `Github.getProjectTasks(status)` SHALL bypass all caching and issue a live request on every call.

#### Scenario: Failed call retries live
- **WHEN** a `getReleases()` call fails and it is called again
- **THEN** the second call issues a new network request instead of replaying the failure

#### Scenario: Project tasks always live
- **WHEN** `getProjectTasks(status)` is called twice with the same status
- **THEN** two network requests are issued

