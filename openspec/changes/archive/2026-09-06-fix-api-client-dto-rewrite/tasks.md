## 1. Models and JSON utility

- [x] 1.1 Create `src/mindustrytool/models` and `src/mindustrytool/utils` packages
- [x] 1.2 Copy/adapt `JsonUtils` from `old.mindustrytool.Utils` (ObjectMapper with `FAIL_ON_UNKNOWN_PROPERTIES=false`, `JavaTimeModule`, `toJson`/`fromJson`/`fromJsonArray`) into `mindustrytool.utils.JsonUtils`
- [x] 1.3 Copy `MapData`, `MapDetailData`, `SchematicData`, `SchematicDetailData` into `mindustrytool.models` and fix package/imports
- [x] 1.4 Copy `TagCategory`, `TagData`, `UserData`, `ModData`, `Sort` into `mindustrytool.models`
- [x] 1.5 Copy `PlayerConnectRoom`, `PlayerConnectProvider` and extract `ServerData` from `ServerService.ServerDto` inner class into `mindustrytool.models`
- [x] 1.6 Copy `ChannelDto`, `ChatMessage`, `ChatUser`, `UserSession` (and related auth DTOs) into `mindustrytool.models`
- [x] 1.7 Copy `TaskData`, `TaskResponse` (for Github project tasks) into `mindustrytool.models` if typed Github returns are used
- [x] 1.8 Verify no copied model imports `old.*` and all Jackson annotations compile

## 2. Request.java — generic HTTP boundary

- [x] 2.1 Add explicit imports in `Request.java` for `HttpClient`, `HttpRequest`, `HttpResponse`, `HttpResponse.BodyHandlers`, `HttpResponse.BodyPublishers`, `Duration`, `URI`, `StandardCharsets` and remove any fully-qualified inline usages
- [x] 2.2 Introduce `mindustrytool.services.AuthTokenProvider` interface (`CompletableFuture<Void> refreshIfNeeded(); String getToken()`) and wire `Request.auth*` to it; provide default impl that delegates to legacy `AuthService` to remove direct `import old.mindustrytool.features.auth.AuthService`
- [x] 2.3 Ensure `Request` exposes `get`, `getBytes`, `post`, `put`, `delete`, `authGet`, `authPost`, `authPut`, `authDelete`, `authUpload`, `buildMultipartBody` as tested async helpers with shared `HttpClient` and `TIMEOUT_MS`/`LONG_TIMEOUT_MS`
- [x] 2.4 Verify `Request.java` compiles with no `old.*` imports and no fully-qualified `java.net.http.*` in method bodies

## 3. MindustryTool.java — typed API client

- [x] 3.1 Replace all fully-qualified `java.net.http.*`/`java.time.*` usages with proper imports (`HttpResponse`, `BodyHandlers`, `URI`, `Duration`, `URLEncoder`, `StandardCharsets`)
- [x] 3.2 Refactor `logout` and `chatStream` to delegate via `Request` helpers (add `Request.send` overload if needed) instead of constructing `HttpRequest` directly
- [x] 3.3 Change return types to DTOs: `findMap`→`CompletableFuture<MapDetailData>`, `searchMaps`→`CompletableFuture<List<MapData>>`, `findSchematic`→`SchematicDetailData`, `searchSchematics`→`List<SchematicData>`, `getTags`→`List<TagData>`, `getUserBatch`→`List<UserData>`, `getPlanets`→`List<ModData>`, `getServers`→`List<ServerData>`, `searchRooms`→`List<PlayerConnectRoom>`, `getProviders`→`List<PlayerConnectProvider>`, `getChatChannels`→`List<ChannelDto>`, `getChatMessages`→`List<ChatMessage>`, `getChatUsers`→`List<ChatUser>`, `getSession`→`UserSession`, etc., using `JsonUtils.fromJson`/`fromJsonArray` in `thenApply`
- [x] 3.4 Keep `downloadMap`/`downloadSchematic`/`downloadSchematicImage`/`downloadMapImage` as `CompletableFuture<byte[]>` via `Request.getBytes`
- [x] 3.5 Ensure storage and translation endpoints remain removed; `MindustryTool` contains only `Config.API_URL` URLs (no GitHub/PROJECT URLs)
- [x] 3.6 Remove any `import old.mindustrytool.Utils` and replace with `mindustrytool.utils.JsonUtils`
- [x] 3.7 Verify `MindustryTool.java` compiles with no `old.*` imports and no inline fully-qualified names

## 4. Github.java — external API client

- [x] 4.1 Add explicit imports in `Github.java` and ensure all calls delegate to `Request.get`
- [x] 4.2 Change `getReleases`/`getProjectTasks` to return typed DTOs via `JsonUtils` where models exist (`List<TaskData>`/`TaskResponse`), or document why raw string is kept (e.g. `mod.hjson`)
- [x] 4.3 Verify `Github.java` contains only `GITHUB_API_URL`, `MOD_HJSON_URL`, `PROJECT_URL` URLs and no `Config.API_URL` usage
- [x] 4.4 Remove any `old.*` imports from `Github.java`

## 5. Verification

- [x] 5.1 Run `grep -R "old\\.mindustrytool" src/mindustrytool` and confirm no hits
- [x] 5.2 Run `grep -R "java\\.net\\.http" src/mindustrytool` and confirm no fully-qualified inline usages remain in method bodies
- [x] 5.3 Run `./gradlew compileJava` and confirm only pre-existing `PagingRequest` error remains; new code compiles cleanly
- [x] 5.4 Smoke-test one typed call per group (maps, tags, chat) with sample JSON to ensure Jackson parsing succeeds
