## Why

When Mindustry is launched from environments with stripped or missing JRE timezone data (such as portable distributions, minimal runtimes, or when executed directly from compressed archives like 360zip into temporary folders), standard library calls to `ZoneId.systemDefault()` throw an unhandled `ZoneRulesException` wrapped in `ExceptionInInitializerError` due to a missing `tzdb.dat`. 

During game startup, `CrashReportService` attempts to inspect past crash logs to protect the user, but calls `CrashTimestampParser`, which invokes `ZoneId.systemDefault()`. Because this initialization error is unchecked, the crash detection routine itself causes a fatal game crash on startup. Furthermore, static initializers in classes such as `ChangelogFormatter` also directly bind to `ZoneId.systemDefault()`, making them vulnerable to class-loading failures.

## What Changes

- Introduce a resilient timezone helper `TimeZones.systemDefaultOrUtc()` that attempts to resolve `ZoneId.systemDefault()` and gracefully falls back to `ZoneOffset.UTC` if timezone data is missing or corrupted.
- Update `CrashTimestampParser` to parse filename timestamps deterministically using `ZoneOffset.UTC` (matching `CrashLocator.CUTOFF_EPOCH`) and guard against any unexpected parsing exceptions by returning a safe fallback.
- Wrap `CrashReportService.checkForCrashes()` in a top-level defensive error boundary so that failure during crash detection can never abort the client startup process.
- Update `ChangelogFormatter` to utilize the resilient timezone fallback instead of calling `ZoneId.systemDefault()` directly in static initializers.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `services`: Update crash detection and changelog formatting requirements to use resilient timezone resolution (`TimeZones.systemDefaultOrUtc()` / `ZoneOffset.UTC`) and enforce non-fatal error boundaries during startup crash checks.

## Impact

- **Affected Code**: `CrashTimestampParser`, `CrashReportService`, `ChangelogFormatter`, and new utility `TimeZones` in `mindustrytool.utils`.
- **APIs**: Internal utility methods and parser behavior. No breaking public API changes.
- **Dependencies**: No external dependencies added; uses standard Java 8 compatible `java.time` APIs.
