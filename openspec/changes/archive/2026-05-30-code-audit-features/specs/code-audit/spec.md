## ADDED Requirements

### Requirement: Feature inventory document
The system SHALL produce a `docs/FEATURE.md` file that lists every feature group with descriptions and file mappings.

#### Scenario: Feature inventory covers all files
- **WHEN** all files in `src/mindustrytool/` are enumerated
- **THEN** `docs/FEATURE.md` contains entries for: core (Config, Main, Utils, etc.), auth, autoplay, background, browser-map, browser-schematic, chat-global, chat-pretty, chat-translation, display-healthbar, display-item-visualizer, display-pathfinding, display-progress, display-quickaccess, display-range, display-teamresource, display-togglerendering, display-wavepreview, godmode, music, playerconnect, savesync, settings, smartdrill, smartupgrade, time, plus services, dto, ui, utils packages

### Requirement: Per-feature audit documents
The system SHALL produce one `docs/<feature>.md` file per feature group containing categorized audit findings.

#### Scenario: Each feature has its own audit file
- **WHEN** the audit is complete
- **THEN** each feature group has a corresponding `docs/<feature>.md` file

### Requirement: Finding categories
Each finding SHALL be categorized as: security, data-loss, concurrency, anti-pattern, dead-code, tech-debt, performance, or architectural-violation.

#### Scenario: Findings are categorized
- **WHEN** an issue is identified
- **THEN** it is tagged with one of the required category labels

### Requirement: Finding severity
Each finding SHALL be assigned a severity level: critical, high, medium, or low.

#### Scenario: Severity is assigned
- **WHEN** a finding is documented
- **THEN** it includes a severity rating

### Requirement: File path references
Each finding SHALL include the exact source file path and relevant line numbers.

#### Scenario: Finding references source location
- **WHEN** a finding is reported
- **THEN** it includes `src/mindustrytool/<path>.java:<line>` format

### Requirement: Priority ordering
Findings SHALL be prioritized: security first, then data-loss, concurrency, anti-patterns, dead-code, tech-debt, performance, architectural-violation.

#### Scenario: Security issues are listed first
- **WHEN** findings are listed in a feature doc
- **THEN** security issues appear before other categories
