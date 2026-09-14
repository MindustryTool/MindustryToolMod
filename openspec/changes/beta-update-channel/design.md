## Context

`UpdateService.checkForUpdate` currently gates entirely on the stable `mod.hjson` version pointer: it fetches `Config.MOD_HJSON_URL`, compares against the installed version, and only then fetches GitHub releases for changelog text. The beta flag (`ModSettings.betaParticipate`) only filters changelog lines after that stable-driven gate, so beta users are never offered prereleases — e.g. installed `v5.0.1-v8` with published `v5.0.3-v8-beta` stays silent because stable latest (`v4.59.1-v8`) is older than installed. Real release data confirms both channels coexist in one releases array (`prerelease: true/false` per entry, tags like `v5.0.3-v8-beta`).

## Goals / Non-Goals

**Goals:**
- When beta is ON, the update gate derives "latest" solely from the GitHub releases list (max tag over all entries), so prereleases can trigger the update dialog.
- The beta dialog identifies the prerelease honestly (raw tag) and its Update action installs that exact tag.
- Stable-channel behavior (beta OFF) is byte-for-byte unchanged.

**Non-Goals:**
- Do not change stable-channel orchestration, dialog text, or install behavior.
- Do not fix the suffix-strip version tie (`v5.0.3-v8` vs `v5.0.3-v8-beta` compare equal) — accepted as silent.
- Do not add network-dependent unit tests for version fetching.

## Decisions

### Decision 1 — Beta ON skips `mod.hjson` entirely
**Why**: The releases array already contains both channels, so one fetch is the single source of truth for beta users. Keeping a parallel `mod.hjson` check would add a second source that can disagree (observed live: `mod.hjson` says `4.59.1` while beta tags reach `5.0.3-beta`) with no benefit.

**Alternative considered**: Keep `mod.hjson` for the stable part and take max(stable latest, prerelease latest). Rejected — redundant fetch and two truths to reconcile for the same answer.

### Decision 2 — Latest = max tag via existing `VersionUtils` comparison
**Why**: Reuses the proven parse/strip/compare semantics; prerelease tags (`v5.0.3-v8-beta` → `[5,0,3]`) compare naturally against installed versions. The max-tag selection itself is pure and unit-testable.

**Alternative considered**: Trust release ordering from the API (first entry = latest). Rejected — API order is recency, not version order; explicit max is robust.

### Decision 3 — Beta dialog shows the raw release tag
**Why**: The raw tag (`v5.0.3-v8-beta`) is the exact GitHub identifier and needs no i18n keys or formatting decisions. The installed side keeps its current formatted display.

**Alternative considered**: Formatted number plus a "(beta)" bundle suffix. Rejected — extra keys and ambiguity about which suffix scheme to render.

### Decision 4 — Beta install uses the exact-tag `githubImportMod` overload
**Why**: `githubImportMod(repo, isJava, release, forceEnable)` with the winning tag installs precisely what the dialog offered; the default-branch overload cannot deliver a prerelease. `null` means latest, but the beta path always has a concrete tag so it passes it through uniformly.

### Decision 5 — Beta-path failures are ignored (log + silent finish)
**Why**: The beta check is opportunistic. Showing an error dialog on every launch for a network blip would punish users who opted into beta; the stable path keeps its existing error dialog.

### Decision 6 — Pure helper placement left to the implementer
**Why**: Max-tag selection fits naturally beside `ChangelogFormatter` (already parses releases JSON) or `VersionUtils` (version math). Either satisfies testability without new layers; no reason to prescribe.

## Risks / Trade-offs

- **Risk**: Beta users fetch the releases list on every launch → GitHub rate limiting.
  → **Mitigation**: Same fetch the stable path already performs when an update exists; accepted for opt-in beta users.
- **Risk**: Tag scheme drift (tags stop matching `parseVersion` expectations) silently yields "up to date".
  → **Mitigation**: Pure max-tag helper is unit-tested against real tag shapes (`v5.0.3-v8-beta`); unparseable entries are skipped, never crash.
- **Trade-off**: Same-number cross-channel tie stays silent (accepted, Decision: do nothing).
- **Risk**: `release` parameter semantics (tag name) assumed from signature.
  → **Mitigation**: User-confirmed `null = latest`; implementation passes the concrete winning tag, the least ambiguous use.

## Migration Plan

No migration. Flag defaults to `false`, so all existing users keep stable behavior. Beta users get the new gate on next launch. Rollback is the previous `UpdateService`/`UpdateDialog` code; no persisted state changes shape.
