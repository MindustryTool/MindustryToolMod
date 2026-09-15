## ADDED Requirements

### Requirement: Github service owns only external endpoints and delegates to Request

`Github` SHALL own only `Config.GITHUB_API_URL`, `Config.MOD_HJSON_URL`, and `Config.PROJECT_URL` endpoints, delegate every call to `Request`, and contain no direct `HttpClient` construction.

#### Scenario: Github delegates to Request
- **WHEN** `src/mindustrytool/services/Github.java` is inspected
- **THEN** each method calls `Request.get` (with import, not fully-qualified) and maps `HttpResponse<String>.body()` to a DTO or string, with no `HttpClient`/`HttpRequest` instantiation

#### Scenario: No MindustryTool API URLs in Github
- **WHEN** `Github.java` URLs are listed
- **THEN** none start with `Config.API_URL`

### Requirement: Typed returns for Github and project calls

`Github` methods SHALL return typed futures where applicable (e.g. release list parsed to DTO) instead of raw string where a model exists.

#### Scenario: getReleases returns typed list
- **WHEN** `Github.getReleases(page, perPage)` is called
- **THEN** it delegates to `Request.get(GITHUB_API_URL + "?page=...")` and returns parsed DTOs (or `CompletableFuture<String>` for raw mod.hjson if no DTO is warranted, with explicit rationale)

#### Scenario: getProjectTasks returns typed response
- **WHEN** `Github.getProjectTasks(status)` is called
- **THEN** it calls `Request.get(PROJECT_URL + "/api/v1/projects/.../tasks?status=" + status)` and parses into `TaskResponse` / `List<TaskData>` via `JsonUtils`

### Requirement: Import hygiene for Github

`Github` SHALL use explicit imports for `java.util.concurrent.CompletableFuture`, `mindustrytool.Config`, `mindustrytool.utils.JsonUtils`, and `mindustrytool.models.*` where used, and SHALL NOT use fully-qualified inline names.

#### Scenario: No fully-qualified names in Github
- **WHEN** `Github.java` is inspected
- **THEN** all types are imported at the top and referenced by simple name
