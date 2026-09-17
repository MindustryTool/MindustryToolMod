## 1. Vanilla Zoom Spike

- [x] 1.1 Confirm the renderer scale setter, vanilla limit behavior, frame ordering for the clamp hook, and desktop plus mobile (pinch) coverage.
- [x] 1.2 Record vanilla defaults and safe beyond-vanilla slider bounds from spike observations.

## 2. Assets and Localization

- [x] 2.1 Add or assign the Lucide zoom/camera feature icon via `assets/icons/` conventions.
- [x] 2.2 Add `feature.camera-zoom.*` bundle keys (title, min/max labels, value format, reset, feature name/description/help) with translator comments.

## 3. Feature and Enforcement

- [x] 3.1 Create `CameraZoomFeature` metadata (id, icon, order, defaults, QuickAccess) with `min-zoom`/`max-zoom` `ConfigValue`s defaulting to vanilla limits.
- [x] 3.2 Implement the gated per-frame `Trigger.update` clamp with untracked reads, write-only-when-outside, and min<=max push behavior.

## 4. Settings UI and Verification

- [x] 4.1 Create the Solim settings dialog with min/max slider rows, reactive value labels, and reset-to-defaults.
- [x] 4.2 Verify Java 8 runtime compatibility, `arc.util.Nullable` usage, imports, ternary style, no `signal.get()` in `build()`, and in-game clamp behavior on desktop and mobile widths.
