## Why

The current HTTP rewrite in `mindustrytool.services` is incomplete: `MindustryTool.java` contains inline HTTP logic alongside API methods, returns raw `String`/`byte[]` instead of typed DTOs, reuses DTOs and utilities from `src/old` via direct imports, and uses fully-qualified class names (e.g. `java.net.http.HttpResponse.BodyHandlers`) instead of imports. This couples the new rewrite to legacy code, leaks parsing to callers, and violates the rewrite goal of a self-contained, typed async client.

## What Changes

- Move all generic HTTP helper methods (`get`, `getBytes`, `post`, `put`, `delete`, `authGet`, `authPost`, `authPut`, `authDelete`, `authUpload`, multipart builder) exclusively into `Request.java`; `MindustryTool.java` and `Github.java` must delegate to `Request` and contain no direct `HttpClient`/`HttpRequest` construction
- Change `MindustryTool` and `Github` API methods to parse responses into DTOs and return `CompletableFuture<DTO>` / `CompletableFuture<List<DTO>>` / `CompletableFuture<byte[]>` instead of `CompletableFuture<String>`
- Copy all used DTOs from `src/old` into `src/mindustrytool/models` (no imports from `old.*`); update package to `mindustrytool.models` and make model classes self-contained
- Copy/adapt any required utility code (JSON serialization via Jackson) into the new codebase instead of importing `old.mindustrytool.Utils`
- Remove translation and storage endpoints from `MindustryTool.java` (already requested)
- Replace all fully-qualified usages with proper `import` statements (e.g. `import java.net.http.HttpResponse.BodyHandlers`, `import java.net.http.HttpRequest`, `import java.time.Duration`)
- Keep `Github.java` as the sole owner of GitHub and external project endpoints; `MindustryTool.java` must only call `Config.API_URL`

## Capabilities

### New Capabilities
- `http-client`: Generic async HTTP helpers in `Request.java` with unauthenticated and authenticated variants using Java 17 `HttpClient`
- `mindustrytool-api`: Typed MindustryTool API client returning DTOs from `mindustrytool.models` via `MindustryTool.java`
- `api-models`: Self-contained DTO/model set under `mindustrytool.models` copied from legacy code, decoupled from `old.*`
- `github-service`: Typed GitHub and external project API client via `Github.java`

### Modified Capabilities
<!-- No existing specs to modify; this is a new rewrite boundary. -->

## Impact

- Affected code: `src/mindustrytool/services/Request.java`, `src/mindustrytool/services/MindustryTool.java`, `src/mindustrytool/services/Github.java`, new `src/mindustrytool/models/**`, new `src/mindustrytool/utils/JsonUtils.java` (or similar)
- Breaking: API method signatures change from `CompletableFuture<String>` to `CompletableFuture<DTO>` — callers in `src/old` not affected, but future new-code callers must use typed returns
- Dependencies: Jackson `ObjectMapper` (already in `build.gradle`), Java 17 `java.net.http.HttpClient`; no new external dependencies
- Systems: MindustryTool API, GitHub API; auth token handling via `AuthService` will be decoupled (inject token provider or copy minimal auth contract)
