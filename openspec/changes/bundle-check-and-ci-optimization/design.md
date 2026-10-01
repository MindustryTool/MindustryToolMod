## Context

MindustryToolMod utilizes Java properties bundles (`assets/bundles/bundle.properties` as default English) for user-visible i18n text, alongside localized variants `bundle_vi.properties`, `bundle_ru.properties`, and `bundle_zh_CN.properties`.

Over time, several issues have accumulated:
1. Significant drift: Russian and Chinese bundles currently lack 573 keys (~67% missing), while Vietnamese is missing 226 keys and retains 9 dead/orphaned keys.
2. No automated build-time or CI verification exists to alert contributors when new strings are introduced in code without corresponding entries across all bundles.
3. The GitHub CI workflow ([`.github/workflows/ci.yml`](file:///e:/Codes/MindustryTool/MindustryToolMod/.github/workflows/ci.yml)) runs as an un-staged single job. It configures Android SDK tools even for checks, lacks concurrency cancellation on rapid commits, and attempts to publish GitHub releases on all branch pushes.

## Goals / Non-Goals

**Goals:**
- Provide a Gradle verification task `checkBundles` that performs strict checks on missing, extra, duplicate, and malformed keys against `bundle.properties`.
- Wire `checkBundles` directly into `./gradlew check` so it runs during local developer verification and CI.
- Warn on untranslated keys (locale value identical to English), filtering out format placeholders (e.g. `{0}`) and common proper nouns (`DevX`, `Gemini`, `DeepL`).
- Clean up extra/dead keys in `bundle_vi.properties`, `bundle_ru.properties`, and `bundle_zh_CN.properties`, and synchronize all missing keys from base to satisfy 100% key completeness.
- Restructure GitHub CI into staged jobs (`quality-check`, `build-package`, `publish-release`) with concurrency cancellation and restricted release publishing.

**Non-Goals:**
- Machine translation generation inside the build task (keys are manually translated or synced).
- Runtime bundle hot-reloading.
- Changing runtime bundle resolution logic inside Mindustry.

## Decisions

### Decision 1: Custom Gradle Task vs Java JUnit Test
- **Choice**: Custom Gradle Task `checkBundles` registered in root `build.gradle` (or `buildSrc` / helper script) and linked via `check.dependsOn checkBundles`.
- **Rationale**: Keeps bundle verification as a standard Gradle quality check alongside Checkstyle and tests. Developers and CI can execute `./gradlew checkBundles` standalone.
- **Alternatives Considered**: 
  - Unit test in `src/test/java`: Would run under JUnit, but bundle files belong to assets, not JVM test resources, and failure reporting is cleaner as a dedicated Gradle task with `$GITHUB_STEP_SUMMARY` markdown output.

### Decision 2: Parser Strategy (Custom Line Reader vs `java.util.Properties`)
- **Choice**: Custom line-by-line parser.
- **Rationale**: `java.util.Properties.load()` silently overwrites duplicate keys within the same file and discards line number context. A line-by-line parser allows detecting duplicates, validating placeholder tokens (`{0}`, `{1}`), and pinpointing exact offending line numbers.
- **Alternatives Considered**: 
  - `java.util.Properties`: Simpler, but cannot detect duplicate keys in a single file.

### Decision 3: Strict Failure Policy for Missing Keys
- **Choice**: Missing keys in any locale bundle fail the build.
- **Rationale**: Based on user requirements, 100% key parity is enforced so no locale displays unmapped missing keys.
- **Mitigation / Pre-requisite**: Synchronize and update all existing locale bundles so all 853 keys are present before enforcing the check.

### Decision 4: Staged CI Pipeline with Concurrency
- **Choice**: Split `ci.yml` into 3 sequential jobs:
  1. `quality-check` (JDK 17 + Gradle caching, runs `./gradlew check` which includes Checkstyle, JUnit tests, and `checkBundles`).
  2. `build-package` (JDK 17 + Android SDK build tools, runs `./gradlew deploy` to generate Desktop and Android hybrid JARs).
  3. `publish-release` (runs only on push to `main` for stable release, or `dev` / `v*` tags for prerelease).
- **Concurrency**: Set `concurrency: group: ${{ github.workflow }}-${{ github.ref }}, cancel-in-progress: true`.
- **Alternatives Considered**: Single monolithic job: Slower fail feedback and requires setting up Android SDK build-tools even when Checkstyle or bundle checks fail.

## Risks / Trade-offs

- **[Risk] Syncing 570+ missing keys into RU and ZH_CN**: Immediate full human translation for 570 keys in RU and ZH_CN might not be instantly available.
  - *Mitigation*: Populate missing keys with English base text or accurate translations where available, enabling the strict key check to pass while flagging them as untranslated warnings.
- **[Risk] False positives on untranslated keys**: Proper nouns and symbol strings (e.g. `{0}`) match English identically.
  - *Mitigation*: Filter out tokens containing only format specifiers, punctuation, numbers, or known brand names (`DevX`, `Gemini`, `DeepL`).
- **[Risk] GitHub Release collision on concurrent pushes**: Multiple pushes to `dev` attempting to write the same tag simultaneously.
  - *Mitigation*: Concurrency cancellation ensures only the latest commit builds and publishes.
