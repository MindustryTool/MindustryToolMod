# browser-render-budget Specification

## Purpose
TBD - created by archiving change schematic-browser-perf-tracker. Update Purpose after archive.
## Requirements
### Requirement: Chunked page reveal under frame budget

The system SHALL reveal browser grid pages in per-frame chunks so that no single frame exceeds a 26ms render budget.

#### Scenario: First chunk paints immediately

- **WHEN** a new browser page arrives with more items than the render chunk size
- **THEN** the grid SHALL first mount at most one chunk of cards and reveal the remainder in subsequent frames.

#### Scenario: Later chunks reuse earlier cards

- **WHEN** a subsequent chunk is revealed
- **THEN** keyed reconciliation SHALL reuse already-mounted cards and only build new ones.

#### Scenario: Stale expansions are dropped

- **WHEN** the page changes or the dialog is disposed while chunks are pending
- **THEN** pending expansions SHALL NOT mount items from the previous page or touch disposed components.

### Requirement: Background image decode

The system SHALL decode browser preview images off the main thread and only upload GL textures on the main thread, and SHALL apply decode results deterministically so that the most recent `loadUrl` for an image instance wins regardless of worker completion order.

#### Scenario: Disk cache decode off main

- **WHEN** a preview image resolves from the disk cache
- **THEN** file read plus PNG decode SHALL run on a background worker and only texture creation plus drawable application SHALL run on the main thread

#### Scenario: Current generation wins over stale decodes

- **WHEN** an image instance calls `loadUrl` more than once (for example a constructor URL followed by `rounded(...)` or `size(...)`) and an older decode finishes after a newer one
- **THEN** only the result whose per-instance generation and URL still match SHALL be applied, and the superseded result SHALL be cached under its own key without being applied

#### Scenario: Captured decode parameters stay consistent

- **WHEN** a decode is scheduled and the corner radius or target size changes before it completes
- **THEN** the decode, its cache key, and its application SHALL all use the parameters captured at schedule time

#### Scenario: Corrupt or empty cache retries over network

- **WHEN** a cached file is empty, undecodable, or expired
- **THEN** the system SHALL delete the corrupt file and retry the load through the network loader instead of permanently showing the fallback drawable, and SHALL NOT crash

### Requirement: Lean browser cards

The system SHALL render schematic and map browser cards with consolidated action controls, separate lightweight likes and comments stat buttons for visual parity, and a single image load per card.

#### Scenario: Stats retain separate lightweight buttons

- **WHEN** a browser card renders its likes and comments stats
- **THEN** likes and comments SHALL each use a lightweight stat button opening details, while download/save, play, and copy actions keep their dedicated buttons.

#### Scenario: Single image load per card

- **WHEN** a browser card preview image is configured
- **THEN** image loading SHALL trigger exactly once per card instead of once per modifier pass.
