## 1. Resilient Timezone Utility

- [ ] 1.1 Implement `TimeZones.java` in `mindustrytool.utils` providing `systemDefaultOrUtc()` with automatic fallback to `ZoneOffset.UTC` on `Throwable`
- [ ] 1.2 Add unit tests for `TimeZones` verifying default zone resolution and graceful fallback to UTC

## 2. Safe Crash Detection and Startup Boundary

- [ ] 2.1 Update `CrashTimestampParser` to parse timestamps using `ZoneOffset.UTC` and catch `Throwable`
- [ ] 2.2 Add defensive `try-catch (Throwable)` error boundary in `CrashReportService.checkForCrashes()` to ensure failure never blocks startup
- [ ] 2.3 Add unit tests for `CrashTimestampParser` verifying UTC epoch calculation, chronological ordering, and invalid input resilience

## 3. Protect Changelog Formatting

- [ ] 3.1 Update `ChangelogFormatter` to use `TimeZones.systemDefaultOrUtc()` for date formatting
- [ ] 3.2 Update and verify `ChangelogFormatterTest` passes with resilient timezone handling

## 4. Verification & Testing

- [ ] 4.1 Run `./gradlew test` across all modules to verify unit tests pass
- [ ] 4.2 Run `./gradlew spotlessCheck` to verify code formatting compliance
