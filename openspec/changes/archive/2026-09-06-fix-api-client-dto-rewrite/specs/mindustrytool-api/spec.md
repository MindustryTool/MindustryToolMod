## ADDED Requirements

### Requirement: MindustryTool delegates all HTTP to Request and uses only Config.API_URL

`MindustryTool` SHALL contain only methods that target `Config.API_URL`; it MUST NOT construct `HttpClient`/`HttpRequest` directly and SHALL delegate every call to `Request.get`/`getBytes`/`post`/`authGet`/`authPost`/`authPut`/`authDelete` etc.

#### Scenario: No direct HttpClient construction in MindustryTool
- **WHEN** `src/mindustrytool/services/MindustryTool.java` is inspected
- **THEN** it contains no `HttpClient.newBuilder`, no `HttpRequest.newBuilder`, and no `java.net.http.HttpResponse.BodyHandlers` inline usage; all HTTP goes through `static import mindustrytool.services.Request.*` or `Request.CLIENT` for the single authenticated SSE case via helper overload

#### Scenario: Chat stream and logout use Request helpers
- **WHEN** `chatStream` or `logout` is needed
- **THEN** they use a `Request` helper (e.g. `Request.send` or `authGet` variant) rather than inline `HttpRequest.newBuilder` fully-qualified construction

#### Scenario: Only API_URL endpoints remain
- **WHEN** all methods in `MindustryTool` are listed
- **THEN** every URL starts with `Config.API_URL` and no GitHub or `PROJECT_URL` endpoints appear

### Requirement: Typed DTO returns via JSON parsing

`MindustryTool` methods SHALL return `CompletableFuture<DTO>` or `CompletableFuture<List<DTO>>` (or `CompletableFuture<byte[]>` for binary) by mapping `HttpResponse<String>` through a copied `JsonUtils` instead of returning `CompletableFuture<String>`.

#### Scenario: Find map returns MapDetailData
- **WHEN** `MindustryTool.findMap(id)` is called
- **THEN** it calls `Request.get(url("/maps/" + id))` and maps `response.body()` via `JsonUtils.fromJson(MapDetailData.class, body)` to `CompletableFuture<MapDetailData>`

#### Scenario: Search maps returns List<MapData>
- **WHEN** `MindustryTool.searchMaps(page,size,sort,query,tags)` is called
- **THEN** it builds a paged URL and returns `CompletableFuture<List<MapData>>` via `JsonUtils.fromJsonArray(MapData.class, body)`

#### Scenario: Tags and users return typed lists
- **WHEN** `getTags` or `getUserBatch` is called
- **THEN** they return `CompletableFuture<List<TagData>>` / `List<UserData>` parsed from JSON, not raw string

#### Scenario: Binary endpoints remain byte[]
- **WHEN** `downloadMap` or `downloadSchematic` is called
- **THEN** they use `Request.getBytes` and return `CompletableFuture<byte[]>` directly

### Requirement: Storage and translation endpoints removed

`MindustryTool` SHALL NOT contain storage (`/storage/*`) or translation (`/translations/translate`) endpoints.

#### Scenario: No storage methods
- **WHEN** `MindustryTool.java` is searched for `storageUrl`, `listStorageSlots`, `uploadFile`
- **THEN** no matches are found

#### Scenario: No translation methods
- **WHEN** `MindustryTool.java` is searched for `translate`
- **THEN** no method returns a translation endpoint

### Requirement: Import hygiene for MindustryTool

`MindustryTool` SHALL use explicit imports for `java.net.http.HttpResponse`, `HttpResponse.BodyHandlers`, `java.net.URI`, `java.time.Duration`, `java.net.URLEncoder`, `java.nio.charset.StandardCharsets`, and SHALL NOT use fully-qualified names like `java.net.http.HttpRequest.Builder` inside method bodies.

#### Scenario: No inline fully-qualified http types
- **WHEN** `src/mindustrytool/services/MindustryTool.java` is inspected
- **THEN** all `java.net.*` and `java.net.http.*` types are imported at the top and referenced by simple name

### Requirement: Retained endpoints stay complete

`MindustryTool` SHALL retain typed methods for ping, maps/schematics (find/download/search), tags, user batch, planets, servers, player-connect rooms/providers, chat (channels/messages/users/count/state/stream), auth (session/loginUri/pollLoginToken/logout/refreshToken), crash report, and images (schematic/map image).

#### Scenario: All prior MindustryTool API methods have typed equivalents
- **WHEN** the pre-rewrite `MindustryTool` method list is compared to the new one (excluding storage/translation)
- **THEN** each retained method exists with a DTO-typed return and delegates to `Request`
