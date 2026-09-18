## Context

In certain runtime environments—particularly when Mindustry is launched from portable archives via 360zip or minimal JRE distributions—the Java Time Zone Database (`<jre>/lib/tzdb.dat`) is missing or inaccessible. Any call to `ZoneId.systemDefault()` or named timezone lookup causes `ZoneRulesProvider` to fail in a static initializer, raising an unchecked `ZoneRulesException` wrapped in `ExceptionInInitializerError`.

In MindustryToolMod, `CrashReportService.checkForCrashes()` runs immediately during client startup (`ClientLoadEvent`). It calls `CrashLocator` and `CrashTimestampParser`, which invokes `ZoneId.systemDefault()`. Because only `DateTimeParseException` was caught, this unhandled error crashes the entire Mindustry game process. In addition, `ChangelogFormatter` initializes a static `DateTimeFormatter` bound directly to `ZoneId.systemDefault()`, making the entire class unloadable in stripped JRE environments.

## Goals / Non-Goals

**Goals:**
- Provide a centralized resilient timezone resolution utility (`TimeZones.systemDefaultOrUtc()`) that safely falls back to `ZoneOffset.UTC` when JRE timezone rules cannot be loaded.
- Ensure `CrashTimestampParser` parses crash report filename timestamps deterministically using `ZoneOffset.UTC` without querying external timezone data, preserving exact file chronological ordering.
- Establish a top-level error boundary in `CrashReportService.checkForCrashes()` to ensure that diagnostic crash checking can never abort client launch.
- Protect `ChangelogFormatter` from class initialization failure in stripped JRE environments.

**Non-Goals:**
- Packaging or embedding a custom IANA timezone database into the mod.
- Modifying Mindustry core's crash writing or logging implementations.

## Decisions

### 1. Centralize resilient timezone resolution in `TimeZones`
- **Choice**: Implement `TimeZones.systemDefaultOrUtc()` in `mindustrytool.utils`. It attempts `ZoneId.systemDefault()` and catches `Throwable` (such as `ExceptionInInitializerError`, `ZoneRulesException`, `LinkageError`), falling back to `ZoneOffset.UTC`.
- **Rationale**: `ZoneOffset.UTC` is a fixed offset (+00:00) that does not load `tzdb.dat`. Having a single utility guarantees consistent fallback behavior across UI components, formatters, and services.
- **Alternatives considered**: Catching exceptions at every individual call site (duplicative and prone to omissions) or forcing UTC everywhere (which would sacrifice local time formatting for the majority of users).

### 2. Deterministic UTC parsing for `CrashTimestampParser`
- **Choice**: Parse `crash-report-MM_dd_yyyy_HH_mm_ss` using `ldt.toInstant(ZoneOffset.UTC).toEpochMilli()` directly.
- **Rationale**: Mindustry filename timestamps lack timezone offsets. Because relative chronological order is identical regardless of the chosen offset, using UTC guarantees zero file I/O, zero reliance on `tzdb.dat`, and exact alignment with `CrashLocator.CUTOFF_EPOCH` (which is already UTC).
- **Alternatives considered**: Using `TimeZones.systemDefaultOrUtc()` for crash filenames. While safe, using UTC is faster, deterministic, and completely isolates file parsing from timezone systems.

### 3. Catch `Throwable` in `CrashTimestampParser` and `CrashReportService`
- **Choice**: In `CrashTimestampParser.parse(Fi)`, catch `Throwable` and return `0`. In `CrashReportService.checkForCrashes()`, wrap the entire check in a `try ... catch (Throwable e)` block, logging the error and returning `false`.
- **Rationale**: The first rule of diagnostic/telemetry tooling is that it must never destabilize or crash the host application. Catching `Throwable` defends against corrupted files, strange JVM linkage issues, or stripped runtime components.
- **Alternatives considered**: Catching only `Exception`. This is insufficient because `ExceptionInInitializerError` extends `LinkageError` / `Error`, not `Exception`.

### 4. Resilient initialization in `ChangelogFormatter`
- **Choice**: Change `DATE_FORMATTER` in `ChangelogFormatter` to format dates using `TimeZones.systemDefaultOrUtc()`.
- **Rationale**: Prevents `ExceptionInInitializerError` during class loading when checking for updates in stripped runtime environments.

## Risks / Trade-offs

- **[Risk] Displayed times show in UTC for affected users** → **Mitigation**: Users on stripped JREs or running from 360zip will see release/log timestamps in UTC (`+00:00`) instead of their local timezone. This graceful degradation allows the game and mod to run completely normally rather than crashing.
- **[Risk] Catching `Throwable` hides underlying issues** → **Mitigation**: All caught errors are logged using `Log.err(...)` with actionable error context, ensuring developers can inspect logs without taking down the user's game.
