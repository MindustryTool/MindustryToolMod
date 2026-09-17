## Why

The manual-override cooldown keeps autoplay paused for a configurable duration after any touch or key press, which routinely surprises players: brief, incidental input such as tapping a dialog button or typing in chat locks autoplay out for seconds with no visible explanation. Instant yield on input plus immediate resume when input stops is simpler to understand and removes a settings knob that adds confusion rather than control.

## What Changes

- **Remove the `overrideCooldown` setting**: delete the `override-cooldown` `ConfigValue` (default 2.0s) from `AutoplayFeature` and its slider + label from the settings `globalSection`.
- **Remove the timed lockout**: delete `resumeTime` state and the cooldown-wait block in `update()`; manual input still yields control instantly (controller reset + transient state cleanup), and autoplay resumes on the very next frame once input ceases.
- **Remove now-unused bundle keys**: `feature.autoplay.settings.override-cooldown` and `feature.autoplay.settings.override-cooldown.description`.
- **Update tests**: drop the `overrideCooldown` default assertion in `AutoplayFeatureTest.configDefaults`.
- Persisted `override-cooldown` values already stored on player devices become inert orphan data (never read again); no migration needed.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `autonomous-gameplay-ai`: Player Manual Override no longer pauses for a cooldown duration; autoplay yields on input and resumes immediately when input stops.

## Impact

- `mod/src/mindustrytool/features/autoplay/AutoplayFeature.java` (setting, `resumeTime`, `update()` blocks)
- `mod/src/mindustrytool/features/autoplay/AutoplaySettingsView.java` (`globalSection` slider + label)
- `assets/bundles/bundle.properties` (two keys removed)
- `mod/src/test/java/mindustrytool/features/autoplay/AutoplayFeatureTest.java` (default assertion removed)
- `openspec/specs/autonomous-gameplay-ai/spec.md` (Req 2 updated via delta spec)
