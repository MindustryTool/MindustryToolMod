## Context

QuickAccess HUD is an in-game draggable overlay toolbar that exposes quick-toggle buttons for enabled mod features. Currently, eight distinct visual/rendering features are configured with `quickAccess(true)`:
- `HealthBarFeature` (`health-bar`)
- `RangeDisplayFeature` (`range-display`)
- `PathfindingFeature` (`pathfinding`)
- `ProgressDisplayFeature` (`progress-display`)
- `TeamResourceFeature` (`team-resource`)
- `VisualizerFeature` (`visualizer`)
- `WavePreviewFeature` (`wave-preview`)
- `ToggleRenderingFeature` (`toggle-rendering`)

Because the QuickAccess HUD bar defaults to a maximum of 4 columns, these 8 features consume 2 to 3 rows of screen space during gameplay.
Following the pattern established by `AutoplayFeature` (where a single QuickAccess icon button triggers an anchored Solim `Popup` with task controls via `QuickAccessPopupHelper`), we aggregate all display overlays into a single dedicated `DisplayGroupFeature`.

## Goals / Non-Goals

**Goals:**
- Provide a single, compact QuickAccess HUD entry (`DisplayGroupFeature`) with icon `Icon.eye`.
- Tapping the icon toggles an anchored `DisplayGroupPopup` positioned via `QuickAccessPopupHelper`.
- Display all 8 display features in a configurable reactive grid (default 4 columns, matching QuickAccess HUD conventions).
- Allow independently toggling each display feature with a single tap, with active states reflected in real time via reactive bindings.
- Allow long-pressing (>= 300ms) any item button in the popup to open that feature's setting dialog.
- Provide a `DisplayGroupSettingsDialog` to configure column count (default 4), reorder display features, and hide/show individual features within the popup.
- Default `quickAccess(false)` on the 8 individual display features so they do not populate the main QuickAccess HUD bar.

**Non-Goals:**
- Merging non-display features like `FreeCameraFeature` or `CameraZoomFeature` into this popup (these remain independent).
- Introducing a global master switch that overrides individual display features (users requested independent toggles only).
- Altering the rendering logic or internal configs of the individual display features themselves.

## Decisions

### 1. Dedicated `DisplayGroupFeature` vs expanding `ToggleRenderingFeature`
- **Decision**: Create a new `DisplayGroupFeature` (id: `display-group`, icon: `Icon.eye`).
- **Rationale**: Keeps responsibilities cleanly separated (Single Responsibility Principle). `ToggleRenderingFeature` remains focused on unit/block layer rendering, while `DisplayGroupFeature` acts as the UI coordinator for all visual features.
- **Alternatives considered**:
  - *Expanding `ToggleRenderingFeature`*: Overloads its responsibility and confuses settings between layer toggling and general display aggregation.
  - *Hardcoding inside `QuickAccessFeature`*: Violates feature modularity and Solim architecture guidelines.

### 2. Solim `Popup` with `QuickAccessPopupHelper`
- **Decision**: Use Solim's `solim.overlay.Popup<DisplayGroupFeature>` and `QuickAccessPopupHelper.show(menu, feature, anchor)`.
- **Rationale**: Identical to `AutoplayPopup`, `TimeControlPopup`, and `QuickSchematicGridPopup`. Provides seamless repositioning, screen clamping, orientation adaptation, and click-outside dismissal.
- **Alternatives considered**:
  - *Standard `SolimDialog`*: Disruptive modal that covers the center of the screen, unsuitable for fast in-game overlay toggling.
  - *Persistent second HUD bar*: Takes up permanent screen real estate and clashes with existing HUDs.

### 3. Configurable Grid Layout & Order Persistence
- **Decision**: Persist column count (`colsConfig`, default 4), custom order (`displayOrderConfig`, Seq<String>), hidden set (`hiddenFeaturesConfig`, Set<String>), and shown set (`shownFeaturesConfig`, Set<String>) in `DisplayGroupFeature.config`.
- **Rationale**: Mirrors `QuickAccessFeature`'s proven normalized order resolution and dual-override visibility model (`computeVisibleItems`). Allows players to tailor which display buttons appear in the popup.
- **Alternatives considered**:
  - *Fixed hardcoded list*: Inflexible for players who only use 2 or 3 display overlays.

### 4. Click & Long-Click Interaction Contract
- **Decision**:
  - QuickAccess HUD button: single-click opens/closes `DisplayGroupPopup`; long-click opens `DisplayGroupSettingsDialog`.
  - Item button inside popup: single-click executes `f.setEnabled(!f.isEnabled())`; long-click (>= 300ms) executes `f.onQuickAccessLongClick(anchor)` (falling back to `f.getSettingDialog()`).
- **Rationale**: Consistent with standard MindustryTool QuickAccess muscle memory.

## Risks / Trade-offs

- **[Risk] Discovery of individual settings**: Players might not realize long-pressing an item in the popup opens its settings.
  → **Mitigation**: Add descriptive tooltip mentioning long-press for settings, plus provide shortcuts to individual setting dialogs in `DisplayGroupSettingsDialog`.
- **[Risk] User still wants a specific display feature directly on QuickAccess**:
  → **Mitigation**: QuickAccess settings (`QuickAccessSettingsDialog`) already supports manual inclusion via `shownFeaturesConfig`. Any display feature can still be pinned to QuickAccess HUD individually by the player.
