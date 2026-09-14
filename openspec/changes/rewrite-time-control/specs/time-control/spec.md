## ADDED Requirements

### Requirement: Host-or-single-player gating
The time-control feature SHALL apply game speed only when the session is single-player (`!Vars.net.active()`) or locally hosted (`Vars.net.server()`). When the client state (`Vars.net.client()`) holds, speed application SHALL be refused and any active speed SHALL reset to 1x.

#### Scenario: Speed applies while hosting
- **WHEN** `Vars.net.server()` is true and the user selects a speed
- **THEN** the delta provider applies the chosen multiplier

#### Scenario: Speed applies in single-player
- **WHEN** `Vars.net.active()` is false and the user selects a speed
- **THEN** the delta provider applies the chosen multiplier

#### Scenario: Client join forces reset
- **WHEN** the net state transitions to `Vars.net.client()` while a non-1x speed is active
- **THEN** speed resets to 1x, the default clamped provider is restored, and the HUD hides until validity returns

### Requirement: Ephemeral speed lifecycle
Speed SHALL live in a memory-only `Signal<Float>` defaulting to 1x and SHALL never persist across sessions. Reset to 1x (signal plus default clamped provider restore) SHALL occur on feature disable/dispose, world unload or return to menu (`ResetEvent`/`StateChangeEvent` to menu), VALID-to-INVALID net transitions, and interaction-mode switches.

#### Scenario: Disable restores default clock
- **WHEN** the feature is disabled or disposed while a non-1x speed is active
- **THEN** the delta provider is restored to the default clamped form and the signal returns to 1x

#### Scenario: Menu exit resets speed
- **WHEN** the game transitions to the menu or the world unloads with a non-1x speed active
- **THEN** speed resets to 1x with the default provider restored

#### Scenario: Speed does not survive restart
- **WHEN** the game restarts with a previously selected non-1x speed
- **THEN** the feature starts at 1x with no stored speed value read

### Requirement: Standalone reactive HUD
The feature SHALL render a standalone Solim `hud()` overlay (not a QuickAccess entry) with a drag handle bound via `.draggable(xSignal, ySignal)`, portrait/landscape position persistence following the QuickAccess `ConfigGroup` pattern, and `keepInScreen` on `ResizeEvent`. The HUD SHALL be visible only while the feature is enabled, `Vars.ui.hudfrag.shown` is true, `Vars.state.isGame()` is true, and the net gate is valid. Construction SHALL NOT unwrap signals with `.get()` in `build()`; bindings SHALL be automatic with ambient ownership.

#### Scenario: HUD shows during valid hosted game
- **WHEN** the feature is enabled during active hosted gameplay with the HUD fragment shown
- **THEN** the time-control bar is added to `Vars.ui.hudGroup` and visible

#### Scenario: HUD hides for clients
- **WHEN** the session is a client session
- **THEN** the HUD is not visible even while the feature row remains listed

#### Scenario: Drag persists per orientation
- **WHEN** the player drags the HUD anchor and rotates the screen
- **THEN** coordinates persist under separate portrait/landscape keys and the bar stays within screen bounds

### Requirement: Preset interaction mode
In preset mode the HUD SHALL offer the fixed preset list with tap-to-select; tapping the already-selected preset SHALL toggle the legacy double-tap boost (`>=1 ? x2 : /2`) and SHALL update the selected highlight reactively without rebuilding the view.

#### Scenario: Tap selects preset
- **WHEN** the user taps a non-selected preset
- **THEN** speed applies that multiplier and the highlight moves to it with no full view rebuild

#### Scenario: Double-tap boosts selected preset
- **WHEN** the user taps the already-selected preset
- **THEN** the boosted value applies and the label reflects the effective multiplier

### Requirement: Slider interaction mode
In slider mode the HUD SHALL offer a clamped slider bound directly to the speed signal within the fixed v1 range and step, with a reactive label showing the effective multiplier.

#### Scenario: Drag changes speed continuously
- **WHEN** the user drags the slider
- **THEN** the applied multiplier and the label update reactively within the clamped range

### Requirement: Persisted mode setting with reset on switch
The interaction mode (presets or slider) SHALL persist via a `ConfigValue` while speed stays ephemeral; switching modes SHALL reset speed to 1x with the default provider restored.

#### Scenario: Mode survives restart
- **WHEN** the user selects slider mode and restarts the game
- **THEN** slider mode is still selected while speed starts at 1x

#### Scenario: Mode switch clears custom speed
- **WHEN** the user switches modes with a non-1x speed active
- **THEN** speed resets to 1x before the new mode renders

### Requirement: Settings dialog
The feature SHALL expose a `TimeControlSettingsDialog` (Solim dialog with mode selector, reset-speed-to-1x action, reset-HUD-position action, and a hosting-only safety note) following the `QuickAccessSettingsDialog` structure. All rows SHALL bind declaratively with no manual subscriptions for ordinary state.

#### Scenario: Reset speed action
- **WHEN** the user activates reset-speed in the settings dialog
- **THEN** speed returns to 1x with the default provider restored and the HUD reflects it

#### Scenario: Reset position action
- **WHEN** the user activates reset-position in the settings dialog
- **THEN** portrait and landscape positions return to screen center

### Requirement: Translated strings and rewritten help
All user-visible time-control text SHALL resolve from `assets/bundles/bundle.properties` under `feature.time-control.*` keys with per-key translator comments; dynamic labels SHALL use `Core.bundle.format`. The stale "cannot be enabled yet" help text SHALL be replaced with text describing host/single-player gating, the two modes, and the auto-reset behavior. No new hardcoded display strings SHALL be introduced.

#### Scenario: Help describes gating and modes
- **WHEN** the help dialog renders for time-control
- **THEN** `feature.time-control.help` text explains hosting/single-player availability, preset vs slider modes, and auto-reset, with no hardcoded strings

### Requirement: Clamped provider apply and restore
Applying a multiplier SHALL use a provider that clamps the effective step after multiplication, and every reset path SHALL restore the default clamped provider form. The default form SHALL be documented at the restore call site since `Time` exposes no getter.

#### Scenario: Apply clamps extreme steps
- **WHEN** a high multiplier is applied during a hitch frame
- **THEN** the effective per-frame step is clamped instead of growing unbounded
