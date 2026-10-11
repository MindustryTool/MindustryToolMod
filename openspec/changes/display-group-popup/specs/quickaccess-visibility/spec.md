## ADDED Requirements

### Requirement: Display overlay delegation to DisplayGroup
The system SHALL configure `quickAccessByDefault=false` on `HealthBarFeature`, `RangeDisplayFeature`, `PathfindingFeature`, `ProgressDisplayFeature`, `TeamResourceFeature`, `VisualizerFeature`, `WavePreviewFeature`, and `ToggleRenderingFeature`, routing their default in-game quick access into `DisplayGroupFeature`. Users MAY still explicitly show any of these features directly on the QuickAccess HUD bar via `shownFeaturesConfig` in `QuickAccessFeature`.

#### Scenario: Display overlays hidden from default QuickAccess HUD
- **WHEN** QuickAccess HUD initializes with default configuration
- **THEN** individual display features (HealthBar, RangeDisplay, Pathfinding, ProgressDisplay, TeamResource, Visualizer, WavePreview, ToggleRendering) do not render separate buttons on the QuickAccess HUD bar

#### Scenario: User explicitly pins a display feature to QuickAccess HUD
- **WHEN** user enables a display feature in QuickAccess settings
- **THEN** that feature id is added to `QuickAccessFeature.shownFeaturesConfig` and appears directly on the QuickAccess HUD bar
