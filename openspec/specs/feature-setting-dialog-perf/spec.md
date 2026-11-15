# feature-setting-dialog-perf Specification

## Purpose
Ensure `FeatureSettingDialog` opens instantaneously without blocking the main render loop or causing frame freezes on mobile devices by progressively mounting cards, stripping mobile-irrelevant overheads, and validating latency under mobile viewport simulation.

## Requirements

### Requirement: Progressive Card Mounting
The `FeatureSettingsView` SHALL mount an initial batch of feature cards on first render and progressively load remaining cards across subsequent frames.

#### Scenario: Initial batch mounted immediately
- **WHEN** `FeatureSettingDialog` is opened on mobile
- **THEN** the initial batch of feature cards (up to 6 cards) SHALL be mounted and rendered on the first frame without blocking the main game thread

#### Scenario: Full card list loaded across subsequent frames
- **WHEN** the initial batch is mounted and additional registered features remain unrendered
- **THEN** subsequent batches of cards SHALL be appended over consecutive frames until all features are rendered

#### Scenario: Filtered search mounts matches immediately
- **WHEN** the player enters a search query in the filter input
- **THEN** matching features SHALL be computed and displayed immediately without waiting for background batch cycles

### Requirement: Mobile Overhead Elimination
`FeatureCard` and `FeatureSettingDialog` SHALL strip mobile-irrelevant overheads when running under `Vars.mobile`.

#### Scenario: Tooltips skipped on mobile
- **WHEN** running in mobile mode (`Vars.mobile == true`)
- **THEN** button tooltips on `FeatureCard` SHALL NOT attach `Tooltip` event listeners or child popup tables

#### Scenario: Flattened table hierarchy
- **WHEN** a `FeatureCard` is built
- **THEN** the card hierarchy SHALL use flattened table layouts to minimize recursive layout depth in Arc

#### Scenario: QuickAccess HUD dialog instance reuse
- **WHEN** the user opens `FeatureSettingDialog` multiple times from the QuickAccess HUD
- **THEN** the existing dialog instance SHALL be reused without recreating the entire scene graph from scratch

### Requirement: Mobile Layout Latency Verification
The project SHALL include an automated performance test verifying that `FeatureSettingDialog` builds and performs initial layout within a fast frame budget under simulated mobile conditions.

#### Scenario: Initial mobile build and layout latency
- **WHEN** `FeatureSettingDialog` is constructed and laid out under simulated mobile viewport dimensions
- **THEN** the initial layout pass SHALL execute within the frame budget threshold and mount the initial progressive slice of cards
