# unified-ci-pipeline Specification

## Purpose
Unified CI/CD pipeline across all subprojects, packaging the desktop mod JAR, delivering pull request artifacts, and publishing GitHub Releases with beta branch tagging.

## Requirements

### Requirement: Unified Subproject Verification
The CI workflow SHALL execute all unit tests and quality verification tasks across all 6 subprojects (`:mod`, `:solim`, `:solim-core`, `:solim-runtime`, `:solim-api`, and `:solim-mcp`) on every pull request and push.

#### Scenario: Pull request verification
- **WHEN** a pull request targeting `main` is opened or updated
- **THEN** the workflow runs `./gradlew check` using JDK 17 with Gradle build caching enabled and reports test results.

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
The CI workflow SHALL publish GitHub Releases on pushes, tagging non-main branch builds as beta pre-releases and `main` branch builds as official releases.

#### Scenario: Push to main creates official release
- **WHEN** code is pushed to `main` and passes all checks
- **THEN** the workflow parses `mod.hjson` to extract the release version, creates a GitHub Release with that exact tag, marks `prerelease: false`, and attaches `build/libs/MindustryToolMod.jar`.

#### Scenario: Push to non-main branch creates beta prerelease
- **WHEN** code is pushed to any branch other than `main` (such as `dev`) and passes all checks
- **THEN** the workflow appends `-beta` to the version tag (e.g. `v5.0.0-v8-beta`), creates or updates the GitHub Release with `prerelease: true`, and attaches `build/libs/MindustryToolMod.jar`.
