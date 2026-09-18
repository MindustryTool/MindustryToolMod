## Context

Previously, Free Camera was merely a static boolean configuration (`ModSettings.freeCamera`) nested under `mindustrytool.settings`. It only affected `JoystickMobileInput`, which skipped its auto-tracking lerp when the flag was true. Desktop controls were completely unassisted—moving via WASD always reset `DesktopInput.panning = false` and forcibly centered the camera on the player unit. Furthermore, toggling the camera required navigating into mod settings menus, making it impossible to quickly lock or unlock the camera during fast-paced gameplay.

## Goals / Non-Goals

**Goals:**
- Promote Free Camera to a first-class `FreeCameraFeature` extending `Feature`.
- Enable by default (`enabledByDefault(true)`) and add to QuickAccess HUD bar by default (`quickAccessByDefault(true)`).
- Provide 1-tap toggling via QuickAccess HUD and instant recentering onto the player unit via long-press.
- Decouple WASD unit movement from camera snapping on Desktop when Free Camera is active.
- Seamlessly coordinate with `JoystickFeature`, `AutoplayFeature`, and `QuickAccessFeature`.
- Preserve the convenience toggle in `JoystickSettingsView` by binding directly to `FreeCameraFeature.get().enabled()`.
- Clean up `GeneralSettingsView` by removing the redundant checkbox.

**Non-Goals:**
- Modifying camera zoom limits (which remains strictly the responsibility of `CameraZoomFeature`).
- Modifying pathfinding, building, or targeting mechanics.
- Bypassing or reimplementing Arc's camera coordinate projection system.

## Decisions

### 1. Dedicated `FreeCameraFeature` & QuickAccess Actions

- **Decision**: Implement `mindustrytool.features.freecamera.FreeCameraFeature` with `FeatureMetadata`:
  - `id`: `"free-camera"`
  - `icon`: `FileIcon.of("camera.png")`
  - `order`: 6
  - `enabledByDefault`: `true`
  - `quickAccessByDefault`: `true`
- **QuickAccess Tap**: Toggles `setEnabled(!isEnabled())`. When disabled (switching to locked mode), camera resumes tracking the player unit.
- **QuickAccess Long-Press**: Executes `snapToPlayer()`, immediately setting `Core.camera.position.set(Vars.player.x, Vars.player.y)` and resetting pan timers.
- **Keybind Support**: Registers a bindable hotkey for `snapToPlayer()` (default key: `Space` or unassigned) and toggle.

### 2. Desktop Input Camera Decoupling

- **Decision**: On Desktop, ensure the active `DesktopInput` handler does not snap the camera to the player unit when WASD keys are pressed while Free Camera is enabled.
- **Implementation**:
  - Mindustry's `DesktopInput` sets `this.panning = false` whenever `axis(moveX)` or `axis(moveY)` is non-zero, triggering `Core.camera.position.lerpDelta(player, ...)`.
  - When Free Camera is active, ensure `panning` remains `true` or camera lerping is suppressed during WASD updates in our desktop input handler (`FreeCameraDesktopInput` / `JoystickDesktopInput`).
  - When Free Camera is disabled, standard vanilla behavior (WASD snapping camera back to player) is preserved.
- **Alternatives Considered**:
  - *Restoring camera position post-frame in an update event*: Causes noticeable 1-frame visual stutter/jitter.
  - *Toggling vanilla `detach-camera` setting*: Causes vanilla DesktopInput to steer unit towards mouse/camera rather than WASD movement, which breaks player control expectations.

### 3. Virtual Joystick Integration

- **Decision**:
  - `JoystickMobileInput.updateCamera()` checks `FreeCameraFeature.isFreeCam()` instead of reading `ModSettings.freeCamera`.
  - When Free Camera is enabled, `updateCamera()` returns immediately (free panning preserved).
  - When Free Camera is disabled, camera smoothly follows unit after a 500ms pan delay.
  - Double-tapping the joystick knob calls `snapToPlayer()` to instantly center camera onto unit.
  - `JoystickSettingsView` retains an inline checkbox labeled with bundle key `feature.free-camera.name` bound reactively to `FreeCameraFeature.get().enabled()`.

### 4. Autoplay Coordination

- **Decision**: `AutoplayFeature` checks `FreeCameraFeature.isFreeCam()` before running `Core.camera.position.lerp(unit.x, unit.y, 0.1f)`.
- **Rationale**: When players enable Free Camera, they want to inspect their base, check enemy waves, or monitor power grids without having their view dragged wherever the automated bot moves.

### 5. Settings & Backward Compatibility

- **Decision**:
  - `GeneralSettingsView`: Remove the `freeCamera` checkbox row from general mod settings.
  - `ModSettings.freeCamera`: Removed entirely, relying on `FreeCameraFeature.get().enabled()` as the single source of truth.

## Risks / Trade-offs

- **[Risk] Multiple features managing `Vars.control.input` on Desktop**: Both `JoystickFeature` and `FreeCameraFeature` may need custom input handling.
  - *Mitigation*: Unify or coordinate input handler replacement so `Vars.control.input` seamlessly handles both joystick and free-camera desktop input without clashing or nesting wrappers.
- **[Risk] Player gets disoriented with free camera enabled by default**: First-time players might pan far away and lose track of their unit.
  - *Mitigation*: Provide clear visual cues on QuickAccess, a prominent long-press shortcut to snap back to the player unit, and keep the joystick double-tap recenter mechanic.
