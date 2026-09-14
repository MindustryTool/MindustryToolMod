# update-service Delta Specification

## MODIFIED Requirements

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
