# unified-ci-pipeline Specification

## Purpose
Unified CI/CD pipeline across all subprojects, packaging the desktop mod JAR, delivering pull request artifacts, and publishing GitHub Releases with beta branch tagging.
## Requirements
### Requirement: Staged CI Pipeline Architecture
The CI workflow SHALL execute in staged jobs (`quality-check`, `build-package`, and `publish-release`) with workflow-level concurrency cancellation enabled (`cancel-in-progress: true`).

#### Scenario: Workflow execution stages
- **WHEN** a push or pull request triggers the CI pipeline
- **THEN** the `quality-check` job runs first; upon success, `build-package` executes; and upon further success on eligible branches, `publish-release` executes.

#### Scenario: Concurrent commits cancel earlier runs
- **WHEN** a new commit is pushed to a branch or pull request while an earlier run is in progress
- **THEN** GitHub Actions cancels the previous in-progress run automatically.

### Requirement: Unified Subproject Verification
The CI workflow SHALL execute all unit tests, checkstyle validations, and the `checkBundles` localization verification task across all subprojects on every pull request and push.

#### Scenario: Pull request verification
- **WHEN** a pull request targeting `main` is opened or updated
- **THEN** the workflow runs `./gradlew check` using JDK 17 with Gradle build caching enabled and reports test and bundle verification results.

### Requirement: Universal Mod Jar Packaging
The CI workflow SHALL package the universal hybrid mod JAR containing both JVM bytecode and Android Dalvik dex classes (`classes.dex`).

#### Scenario: Building the desktop jar
- **WHEN** the verification job succeeds
- **THEN** the workflow executes `./gradlew deploy` with Android build-tools configured in PATH to produce `MindustryToolMod.jar` in `build/libs/`.

### Requirement: Pull Request Artifact Delivery
The CI workflow SHALL upload the compiled mod JAR as a downloadable GitHub Action run artifact on pull requests.

#### Scenario: PR artifact upload
- **WHEN** a pull request build completes successfully
- **THEN** the workflow uploads `build/libs/MindustryToolMod.jar` as an artifact using `actions/upload-artifact@v4`.

### Requirement: Automated GitHub Releases with Beta Branch Tagging
The CI workflow SHALL publish GitHub Releases only on pushes to `main` (as official releases) and pushes to `dev` or version tags matching `v*` (as beta pre-releases), SHALL NOT publish releases on feature branches, and SHALL include clean commit-based release notes formatted as bulleted lists extracted from git log rather than default pull request comparison notes.

#### Scenario: Push to main creates official release with commit changelog
- **WHEN** code is pushed to `main` and passes all checks
- **THEN** the workflow parses `mod.hjson` to extract the release version, creates a GitHub Release with that exact tag, marks `prerelease: false`, populates the release body with conventional commit summaries since the previous release tag (excluding merge commits), and attaches `build/libs/MindustryToolMod.jar`.

#### Scenario: Push to dev or tag creates beta prerelease with commit changelog
- **WHEN** code is pushed to `dev` or a tag matching `v*` and passes all checks
- **THEN** the workflow appends `-beta` to the version tag (or uses the tag name), creates or updates the GitHub Release with `prerelease: true`, populates the release body with conventional commit summaries since the previous release tag (excluding merge commits), and attaches `build/libs/MindustryToolMod.jar`.

#### Scenario: Push to feature branch does not publish release
- **WHEN** code is pushed to any feature or working branch other than `main` or `dev`
- **THEN** the workflow executes verification and packaging without creating or publishing any GitHub Release.

