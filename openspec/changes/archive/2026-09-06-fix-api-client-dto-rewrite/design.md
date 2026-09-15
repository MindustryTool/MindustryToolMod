## Context

Current rewrite placed typed logic across `Request.java`, `MindustryTool.java`, and `Github.java` but left gaps:
- `Request.java` already holds generic helpers (`get`, `post`, `authGet`, `authUpload`, multipart builder) with correct imports, yet `MindustryTool.java` still constructs `HttpRequest` directly for `logout` and `chatStream`, uses fully-qualified names (`java.net.http.HttpRequest.Builder`, `java.net.URI`), and returns `CompletableFuture<String>` for most endpoints.
- DTOs are still referenced from `src/old` (`old.mindustrytool.dto.*`, `old.mindustrytool.features.*.dto`, `old.mindustrytool.Utils` for JSON). The rewrite must be self-contained under `mindustrytool.*`.
- No self-contained model layer exists; callers must parse JSON manually.
- Auth helpers in `Request` directly import `old.mindustrytool.features.auth.AuthService`, coupling new code to legacy.

Stack: Java 17, Gradle `sourceCompatibility 17`, Jackson `jackson-databind` + `jsr310` already in `build.gradle`, `java.net.http.HttpClient` for async. `Config.API_URL` normalized to `https://api.mindustry-tool.com/api/v4` (no trailing slash) in new `mindustrytool.Config`.

Stakeholders: mod runtime (async game thread), future new-code consumers of `MindustryTool` client.

## Goals / Non-Goals

**Goals:**
- `Request.java` is the single HTTP boundary; `MindustryTool.java`/`Github.java` only compose URLs and delegate to `Request`, returning typed DTOs.
- Typed async API: `MindustryTool` methods return `CompletableFuture<DTO>` / `List<DTO>` / `byte[]`, parsing via copied `JsonUtils` (Jackson) inside the service layer.
- Self-contained `mindustrytool.models` and `mindustrytool.utils` with no `import old.*`.
- All imports are explicit (no `java.net.http.HttpResponse.BodyHandlers` fully-qualified inline).

**Non-Goals:**
- Reimplementing auth flow fully (keep minimal token-refresh contract for now; full `AuthService` rewrite is separate change).
- Changing API semantics, paging, or endpoint contracts.
- Adding image caching or SSE reconnection logic beyond thin `chatStream` wrapper.
- Removing/rewriting `src/old` consumers.

## Decisions

**1. Request owns all HttpClient construction — MindustryTool only builds URLs.**
- Why: Enforces single HTTP boundary; prevents duplication. `MindustryTool.logout` and `chatStream` currently bypass `Request` — they will be refactored to helper overloads (e.g. `Request.send(HttpRequest)` or `authGet`/`postWithHeaders`).
- Alternative considered: Keep some direct `HttpClient` usage in `MindustryTool` for special headers/SSE. Rejected: leakiness; add narrow helper instead.

**2. Typed returns via internal JSON parsing, not caller parsing.**
- `Request` returns `HttpResponse<String>`; `MindustryTool` maps `thenApply(r -> JsonUtils.fromJson(...))`. Keeps parsing close to domain, lets `Request` stay generic.
- Alternative: `Request` with generic `<T> getJson(url, Class<T>)`. Rejected: couples `Request` to Jackson and to model package; keep `Request` DTO-agnostic.

**3. Copy DTOs into `mindustrytool.models`, adapt to new package.**
- Source inventory: `MapData`, `MapDetailData`, `SchematicData`, `SchematicDetailData`, `TagCategory`, `TagData`, `UserData`, `ModData`, `PlayerConnectRoom`, `PlayerConnectProvider`, `ServerDto` (was inner class), `ChannelDto`, `ChatMessage`, `ChatUser`, `UserSession`, plus Jackson annotations. Copy verbatim, change `package old...` → `package mindustrytool.models`, fix imports, add missing fields from JSON shape by inspecting legacy service usage and API samples.
- Alternative: Reuse `old` DTOs via dependency. Rejected: violates rewrite self-containment requirement.
- Copy `old.mindustrytool.Utils` JSON methods (`toJson`, `fromJson`, `fromJsonArray` with `ObjectMapper` + `JavaTimeModule`) into `mindustrytool.utils.JsonUtils`.

**4. Import hygiene.**
- Add explicit imports: `java.net.http.HttpClient`, `HttpRequest`, `HttpResponse`, `HttpResponse.BodyHandlers`, `HttpResponse.BodyHandler`, `java.time.Duration`, `java.net.URI`, `java.net.URLEncoder`, `java.nio.charset.StandardCharsets`, Jackson types. Remove all `java.net.http.*` fully-qualified inline usages in `MindustryTool.java`.
- Enforce via compile check/IDE; no behavior change.

**5. Auth decoupling (minimal).**
- Keep `Request.auth*` needing refresh + token. Introduce `mindustrytool.services.AuthTokenProvider` interface (`CompletableFuture<Void> refreshIfNeeded(); String getToken();`) with default impl delegating to `old AuthService` temporarily, or copy minimal `AuthService` token logic (JWT expiry check + refresh). This removes direct `old.*` import from `Request`.
- Alternative: Pass token explicitly per call. Rejected: verbose; retrofit later with provider.

**6. Keep Github.java scoped to non-`Config.API_URL` endpoints.**
- `Github` owns `Config.GITHUB_API_URL`, `Config.MOD_HJSON_URL`, and `Config.PROJECT_URL` calls. Already correct; ensure it also delegates to `Request` and returns typed DTOs where applicable (e.g. release list).

## Risks / Trade-offs

- **DTO drift vs API** → Mitigation: Copy exact field names from legacy DTOs; verify against live API sample JSON for one map/schematic; keep Jackson `FAIL_ON_UNKNOWN_PROPERTIES=false`.
- **Auth refresh divergence** → Mitigation: Initially copy legacy `refreshTokenIfNeeded` logic verbatim into new provider; add single integration test for 401 path.
- **Duplicate JSON logic transient** → Mitigation: Mark `old.Utils` JSON as deprecated; new `JsonUtils` is single source for new code; old callers untouched.
- **Compile break from removed storage/translation methods** → Already removed; verify no new-code callers reference them via grep before closing.
- **Large model copy amplifies maintenance** → Mitigation: Copy only DTOs actually used by retained endpoints (filter out storage/translation DTOs); list in spec.

## Migration Plan

1. Create `mindustrytool.models` and `mindustrytool.utils.JsonUtils`; copy/adapt DTOs and JSON helpers.
2. Refactor `Request.java` to add missing import variants and decouple auth via provider; ensure no `old.*` imports.
3. Refactor `MindustryTool.java`: replace fully-qualified usages with imports, replace direct `HttpRequest` construction (logout/chatStream) with `Request` helpers, change return types to DTOs with `thenApply` parsing, remove raw-String fallbacks.
4. Refactor `Github.java` similarly to typed returns via `JsonUtils`.
5. Verify `./gradlew compileJava` — only expected pre-existing `PagingRequest` error remains; run `grep -R "old\.mindustrytool" src/mindustrytool` to confirm no legacy imports.
6. Rollback: revert three service files + models folder (isolated change, no DB migration).

## Open Questions

- Exact shape of paged responses (API returns bare array vs `{data:[]}`)? Inspect `PagingRequest` and live `GET /maps?page=0&size=1` to decide whether to return `List<T>` or wrapper DTO. Default: `List<T>` from `fromJsonArray`, matching legacy.
- Should `downloadMap`/`downloadSchematic` stay `byte[]` or wrap in DTO with headers? Keep `byte[]` for binary data.
- Token provider final ownership: dedicated `AuthService` rewrite vs. lightweight provider — defer to next change; current minimal provider suffices.
