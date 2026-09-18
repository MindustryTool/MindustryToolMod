# feature-icons Specification

## Purpose
TBD - created by archiving change update-feature-icons. Update Purpose after archive.
## Requirements
### Requirement: Standardized Feature Icons
The mod SHALL provide clear, recognizable Lucide vector icon assets in `assets/icons/` for features that previously lacked dedicated icons or used misleading icons.

#### Scenario: Icon asset availability
- **WHEN** the mod is initialized or a feature queries its icon asset
- **THEN** each configured icon file (`pickaxe.png`, `signal.png`, `message-circle.png`, `music.png`, `hourglass.png`, `swords.png`, `network.png`, `cloud-upload.png`, `wand-sparkles.png`, `sparkles.png`, `chevrons-up.png`, `grid-2x2.png`) exists in `assets/icons/` as a 24x24 white RGBA PNG.

### Requirement: Feature Metadata Icon Assignment
Each target feature SHALL reference its designated icon in its `FeatureMetadata` configuration via `FileIcon.of("<name>.png")`.

#### Scenario: Feature metadata loads custom file icon
- **WHEN** `Feature.getMetadata().getIcon()` is called on SmartDrillFeature, PlayerConnectFeature, ChatFeature, MusicFeature, ProgressDisplayFeature, WavePreviewFeature, BridgeVisualizerFeature, SaveSyncFeature, GodModeFeature, PrettyChatFeature, SmartUpgradeFeature, or QuickAccessFeature
- **THEN** it returns a non-null `Drawable` representing the corresponding custom icon asset.

#### Scenario: Fallback behavior on missing asset
- **WHEN** an icon file fails to load or does not exist
- **THEN** `FileIcon.of` gracefully falls back to `Icon.book` or a default drawable without throwing an uncaught exception.

### Requirement: Emoji feature icon asset

The mod SHALL provide a Lucide `smile.png` icon asset in `assets/icons/` for the Emoji feature, and the Emoji feature metadata SHALL reference it via `FileIcon.of("smile.png")`.

#### Scenario: Emoji icon asset availability

- **WHEN** the mod is initialized or the Emoji feature queries its icon asset
- **THEN** `assets/icons/smile.png` exists as a 24x24 white RGBA PNG.

#### Scenario: Emoji metadata loads file icon

- **WHEN** `Feature.getMetadata().getIcon()` is called on the Emoji feature
- **THEN** it returns the `smile.png` drawable, falling back gracefully without throwing when the asset cannot load.

