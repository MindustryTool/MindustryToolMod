## Why

The project maintains localization bundles (`bundle.properties` as base, plus `bundle_vi`, `bundle_ru`, `bundle_zh_CN`), but currently lacks an automated validation mechanism to detect missing, obsolete (extra), duplicate, or malformed translation keys. As a result, locales have drifted significantly (e.g. Russian and Chinese currently miss >570 keys, while Vietnamese retains obsolete dead keys). Furthermore, GitHub CI runs as an un-staged monolithic job that lacks concurrency control, sets up Android build tools unnecessarily early, and attempts to publish GitHub releases on every single push across all branches.

## What Changes

- Introduce a new Gradle verification task `checkBundles` that verifies all locale bundles against `bundle.properties` (base English).
  - Fails the build on missing keys, obsolete/extra keys, duplicate keys within a file, and format parameter count mismatches (`{0}`, `{1}`).
  - Warns on untranslated keys (where value matches English base, filtering out format tokens like `{0}` and known brand names like `DevX`, `Gemini`, `DeepL`).
  - Integrates `checkBundles` into `./gradlew check`.
- Clean up and synchronize existing locale bundles (`bundle_vi.properties`, `bundle_ru.properties`, `bundle_zh_CN.properties`) by removing orphaned keys and filling in all missing keys to achieve 100% key coverage.
- Optimize GitHub CI (`.github/workflows/ci.yml`):
  - Add concurrency cancellation (`cancel-in-progress: true`) to avoid wasted runner time.
  - Split the pipeline into staged jobs: fast `quality-check` (JDK 17 + Gradle cache + `./gradlew check`), `build-package` (Android build tools + `./gradlew deploy`), and conditional `publish-release`.
  - Restrict GitHub release publication to `main` (stable releases) and `dev` or `v*` tags (beta pre-releases), preventing release attempts on feature branches.

## Capabilities

### New Capabilities
- `bundle-verification`: Automated verification task that enforces bundle completeness, hygiene (no obsolete or duplicate keys), placeholder syntax integrity, and untranslated string monitoring.

### Modified Capabilities
- `unified-ci-pipeline`: Staged multi-job CI workflow with concurrency cancellation, fast fail-first verification including bundle checks, and restricted release publication rules.

## Impact

- Build system: Root `build.gradle` gains the `checkBundles` task wired into `check`.
- Localization files: `assets/bundles/bundle_*.properties` updated to remove obsolete keys and populate missing keys.
- CI/CD: `.github/workflows/ci.yml` restructured into staged jobs with improved concurrency and release filters.
