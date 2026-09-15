## Why

The current autoplay feature implementation in the legacy codebase relies on raw imperative Arc UI dialogs, direct Core.settings manipulation, lacks reactive updates, and abruptly resumes autoplay the instant any player touch ceases. A complete rewrite is required to bring Autoplay into the modern Solim declarative architecture, introduce a configurable pause-cooldown after manual player input, make the follow-unit camera setting cross-platform (defaulting to off), and ensure all user-visible text is fully internationalized.

## What Changes

- Full rewrite of `AutoplayFeature` extending `Feature`, removing the `.development(true)` flag and keeping `enabledByDefault(false)`.
- Migrates all 8 autoplay tasks into the modern feature package:
  - `SelfHealTask` (heals unit when damaged)
  - `FleeTask` (evades combat when low on health)
  - `AttackTask` (targets and fires at nearby enemies)
  - `RepairTask` (repairs friendly damaged buildings)
  - `FollowAssistTask` (follows and assists nearby allied units)
  - `SelfBuildTask` (constructs player's current build plans)
  - `RebuildTask` (reconstructs destroyed structures)
  - `MiningTask` (mines required core resources like copper and lead)
- Adds a configurable player input override cooldown timer (slider in settings from 0.5s to 5s, default 2.0s): whenever the player touches the screen or presses a key, autoplay yields control and stays paused for the cooldown duration before resuming.
- Cross-platform "Follow Unit" camera toggle (available on both Mobile and Desktop, defaulting to false so camera does not follow unit when autoplay is running).
- Overhead visual indicators: renders the active task icon above the player unit and a dashed line to the target objective.
- Declarative `AutoplaySettingsDialog` and `AutoplaySettingsView` built with Solim UI, featuring reorderable priority list, per-task enable toggles, live status labels, and settings sliders.
- Comprehensive internationalization in `assets/bundles/bundle.properties`.

## Capabilities

### New Capabilities
- `autonomous-gameplay-ai`: Autonomous task-based decision engine that controls the player unit to automatically heal, defend, repair, construct, and mine, respecting player input overrides and configurable priority order.

### Modified Capabilities
- (None)

## Impact

- Rewrites `AutoplayFeature.java` and adds modern task classes under `mindustrytool.features.autoplay.tasks`.
- Creates `AutoplaySettingsDialog.java` and `AutoplaySettingsView.java` using Solim UI.
- Deprecates the old implementation in `old/mindustrytool/features/autoplay/`.
- Adds i18n keys for all tasks, statuses, settings labels, and descriptions to `bundle.properties`.
