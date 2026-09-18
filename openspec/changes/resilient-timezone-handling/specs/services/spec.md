## MODIFIED Requirements

### Requirement: ChangelogFormatter is pure and bundle-aware

`ChangelogFormatter` SHALL format up to 20 releases into Mindustry markup: `[accent]tag[white]`, optional `yyyy-MM-dd HH:mm` date formatted using `TimeZones.systemDefaultOrUtc()` (falling back to `ZoneOffset.UTC` if system timezone rules are unavailable), `[gold]` download-count line from summed `assets[].download_count`, and `renderMarkdown(body)` conversion (links→`[sky]`, headers→`[accent]`, lists→`•`, bold/italic/code). Labels SHALL be bundle keys, not hard-coded English.

#### Scenario: Pure formatting without Arc runtime
- **WHEN** `ChangelogFormatter.format(releases)` is called with a parsed release list
- **THEN** it returns a `String` without touching `Core.app` or network, and caps output at 20 releases

#### Scenario: Markdown transforms preserved
- **WHEN** body contains `**bold**`, `*italic*`, `` `code` ``, `[text](url)`, `## header`, `- item`
- **THEN** output contains `[white]bold[white]`, `[lightgray]italic[white]`, `[cyan]code[white]`, `[sky]text[white]`, `[accent]header[white]`, `• item` respectively

#### Scenario: Resilient date formatting without tzdb
- **WHEN** `ChangelogFormatter` formats release dates in an environment lacking `tzdb.dat`
- **THEN** date formatting falls back to UTC without throwing `ExceptionInInitializerError` or `ZoneRulesException`

## ADDED Requirements

### Requirement: Resilient system timezone resolution

`TimeZones.systemDefaultOrUtc()` SHALL attempt to return `ZoneId.systemDefault()`. If resolving the system default zone throws any `Throwable` (such as `ZoneRulesException`, `FileNotFoundException`, or `ExceptionInInitializerError` due to missing `tzdb.dat`), it SHALL catch the error and return `ZoneOffset.UTC`.

#### Scenario: System timezone available
- **WHEN** the underlying JRE has valid timezone database rules
- **THEN** `TimeZones.systemDefaultOrUtc()` returns the host system's `ZoneId`

#### Scenario: Timezone database missing or corrupted
- **WHEN** resolving the system timezone throws a `Throwable`
- **THEN** `TimeZones.systemDefaultOrUtc()` catches the failure and returns `ZoneOffset.UTC`

### Requirement: Safe crash timestamp parsing

`CrashTimestampParser.parse(Fi file)` SHALL parse timestamps from crash report file names into epoch milliseconds using `ZoneOffset.UTC`. If parsing fails due to invalid format, missing file, or any unexpected `Throwable`, it SHALL catch the error, log a warning, and return `0`.

#### Scenario: Parse standard crash report filename
- **WHEN** a valid crash file named `crash-report-MM_dd_yyyy_HH_mm_ss.txt` is parsed
- **THEN** it returns the corresponding epoch milliseconds calculated at `ZoneOffset.UTC`

#### Scenario: Parse crash timestamp with missing timezone rules
- **WHEN** `CrashTimestampParser.parse(file)` is invoked in an environment where `tzdb.dat` is missing
- **THEN** parsing succeeds without throwing any timezone or initialization error

#### Scenario: Parse invalid crash report filename
- **WHEN** an invalid or unparseable crash file is passed to `CrashTimestampParser.parse(file)`
- **THEN** it catches all errors and returns `0`

### Requirement: Non-fatal startup crash inspection

`CrashReportService.checkForCrashes()` SHALL wrap its detection and dialog presentation in a defensive error boundary catching `Throwable`. Any unexpected failure encountered during crash detection SHALL be logged and SHALL NOT abort the client load sequence or crash the application.

#### Scenario: Crash detection succeeds or finds no crashes
- **WHEN** `checkForCrashes()` runs normally
- **THEN** it returns `true` if a new crash dialog was shown, or `false` otherwise

#### Scenario: Crash detection encounters unexpected runtime error
- **WHEN** scanning the crash directory or parsing crash logs raises an unexpected `Throwable`
- **THEN** the error is logged and `checkForCrashes()` returns `false` without terminating the game process
