# time-control Specification

## Purpose

Standalone Solim-first game-speed control gated to hosting or single-player, with ephemeral speed, preset and slider interaction modes, and a settings dialog. Created by archiving change rewrite-time-control.
## Requirements

**Source: rewrite-time-control, time-control-refinements, time-control-scale**

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

The feature SHALL render a standalone Solim `hud()` overlay with a drag handle bound via `.draggable(xSignal, ySignal)`, portrait/landscape position persistence following the QuickAccess `ConfigGroup` pattern, and `keepInScreen` on `ResizeEvent`, and SHALL additionally be a QuickAccess entry. The HUD SHALL be visible only while the feature is enabled, `Vars.ui.hudfrag.shown` is true, `Vars.state.isGame()` is true, the net gate is valid, and the display mode is not popup-with-QuickAccess-on (in popup mode with QuickAccess on the standalone HUD SHALL be fully suppressed). The bar SHALL stay compact at scale 1.0: preset buttons roughly `13x10` units with `1`-unit gaps beside a `10`-unit drag handle (about `320x40px` total), and slider mode roughly `280x40px`, never shrinking interactive elements below mobile touch floors at default scale. A persisted UI scale (`ConfigValue<Float>` `scale`, 0.5–1.5, default 1.0) SHALL resize button sizes, icon sizes, and speed-label font scales via per-element reactive mappings (no `Hud.scale()` container zoom); slider track width, speed label width, gaps, and padding SHALL stay fixed while container row heights follow the scaled button size so content never clips. At scale 1.0 the HUD SHALL render pixel-identical to the fixed-size layout with identical proportions when scaled. Construction SHALL NOT unwrap signals with `.get()` in `build()`; bindings SHALL be automatic with ambient ownership.

#### Scenario: HUD shows during valid hosted game

- **WHEN** the feature is enabled during active hosted gameplay with the HUD fragment shown
- **THEN** the time-control bar is added to `Vars.ui.hudGroup` and visible

#### Scenario: HUD hides for clients

- **WHEN** the session is a client session
- **THEN** the HUD is not visible even while the feature row remains listed

#### Scenario: Drag persists per orientation

- **WHEN** the player drags the HUD anchor and rotates the screen
- **THEN** coordinates persist under separate portrait/landscape keys and the bar stays within screen bounds

#### Scenario: Bar stays compact

- **WHEN** either interaction mode renders at default scale
- **THEN** the bar fits within its compact footprint with a fixed-width speed label that does not resize as values change

#### Scenario: Scale resizes buttons icons and fonts proportionally

- **WHEN** the user changes the scale setting
- **THEN** drag-handle, preset, and reset button sizes plus all icons and speed-label fonts update reactively with identical proportions and no clipping

#### Scenario: Identical at default scale

- **WHEN** scale is 1.0
- **THEN** button sizes, icon sizes, font sizes, track width, label width, gaps, and padding equal the pre-scale fixed layout exactly

#### Scenario: Track and label widths stay fixed

- **WHEN** scale changes
- **THEN** slider track width, speed label width, gaps, and padding do not change

#### Scenario: HUD suppressed in popup mode

- **WHEN** display mode is popup and QuickAccess is on
- **THEN** the standalone bar is not added to `Vars.ui.hudGroup` even while the feature is enabled

#### Scenario: HUD returns on silent fallback

- **WHEN** display mode is popup and QuickAccess is off
- **THEN** the standalone bar renders per the remaining HUD rules with no notice shown

### Requirement: Preset interaction mode
In preset mode the HUD SHALL offer the fixed preset list `0.125, 0.5, 1, 2, 8` with tap-to-select; tapping the already-selected preset SHALL toggle the double-tap boost (`>=1 ? x2 : /2`) EXCEPT for the `1x` preset, which SHALL be boost-locked. Tapping `1x` from any state SHALL clear boost and reset to 1x, and double-tapping `1x` SHALL be a no-op. Presets and boosted values SHALL interleave across every power of two from `2^-4` to `2^4` with no two provenances yielding the same speed. The selected highlight SHALL update reactively without rebuilding the view.

#### Scenario: Tap selects preset
- **WHEN** the user taps a non-selected preset
- **THEN** speed applies that multiplier and the highlight moves to it with no full view rebuild

#### Scenario: Double-tap boosts selected preset
- **WHEN** the user taps the already-selected preset other than `1x`
- **THEN** the boosted value applies and the label reflects the effective multiplier

#### Scenario: No overlap between presets and boosts
- **WHEN** every preset is tapped twice
- **THEN** the nine reachable speeds (`0.0625` through `16`) each arise from exactly one preset-or-boost provenance

#### Scenario: Tapping 1x resets
- **WHEN** the user taps the `1x` preset while boosted at another speed
- **THEN** boost clears, speed returns to 1x, and the `1x` highlight shows

#### Scenario: Double-tap 1x is a no-op
- **WHEN** the user taps the already-selected `1x` preset
- **THEN** speed stays at 1x with no boost applied

### Requirement: Slider interaction mode
In slider mode the HUD SHALL offer a slider over normalized position `u in [-1, 1]` with a fixed `0.05` step, mapped two-sided-quadratically around 1x so that `speed(-u) = 1/speed(u)` with `u = 0` exactly 1x. A reset button after the label SHALL return straight to 1x. The widget SHALL bind to an intermediate position signal flowing single-directionally into the speed signal; resets SHALL zero the position signal. A reactive fixed-width label SHALL show the effective multiplier.

#### Scenario: Drag changes speed continuously
- **WHEN** the user drags the slider
- **THEN** the applied multiplier and the label update reactively within the clamped range

#### Scenario: Center detent lands on 1x
- **WHEN** the user drags to the exact center step
- **THEN** speed applies exactly 1x

#### Scenario: Reset button returns to 1x
- **WHEN** the user activates the slider-mode reset button at a non-1x speed
- **THEN** the slider returns to center and speed applies exactly 1x

### Requirement: Persisted mode setting with reset on switch
The interaction mode (presets or slider) SHALL persist via a `ConfigValue` while speed stays ephemeral; switching modes SHALL reset speed to 1x with the default provider restored.

#### Scenario: Mode survives restart
- **WHEN** the user selects slider mode and restarts the game
- **THEN** slider mode is still selected while speed starts at 1x

#### Scenario: Mode switch clears custom speed
- **WHEN** the user switches modes with a non-1x speed active
- **THEN** speed resets to 1x before the new mode renders

### Requirement: Settings dialog
The feature SHALL expose a `TimeControlSettingsDialog` (Solim dialog with mode selector, UI scale slider, reset-speed-to-1x action, reset-HUD-position action, and a hosting-only safety note) following the `QuickAccessSettingsDialog` structure. The scale slider SHALL range 0.5–1.5 with 0.1 step, bind directly to `scaleConfig.signal()`, and display a percent label. No reset action SHALL be provided for scale. All rows SHALL bind declaratively with no manual subscriptions for ordinary state.

#### Scenario: Reset speed action
- **WHEN** the user activates reset-speed in the settings dialog
- **THEN** speed returns to 1x with the default provider restored and the HUD reflects it

#### Scenario: Reset position action
- **WHEN** the user activates reset-position in the settings dialog
- **THEN** portrait and landscape positions return to screen center

#### Scenario: Adjusting scale slider
- **WHEN** the user drags the scale slider in the settings dialog
- **THEN** the scale configuration persists immediately and the HUD button, icon, and font sizes update reactively

#### Scenario: Scale survives restart
- **WHEN** the user sets a non-default scale and restarts the game
- **THEN** the scale value is restored while speed starts at 1x

### Requirement: Translated strings and rewritten help
All user-visible time-control text SHALL resolve from `assets/bundles/bundle.properties` under `feature.time-control.*` keys with per-key translator comments; dynamic labels SHALL use `Core.bundle.format`. The mode-hint text SHALL document the boost gesture and the `1x` reset exception, the slider reset button SHALL carry a translated tooltip, and the scale slider SHALL use `feature.time-control.settings.scale` with a comment documenting the 50%–150% range. No new hardcoded display strings SHALL be introduced.

#### Scenario: Help describes gating and modes
- **WHEN** the help dialog renders for time-control
- **THEN** `feature.time-control.help` text explains hosting/single-player availability, preset vs slider modes, and auto-reset, with no hardcoded strings

#### Scenario: Hint documents boost and reset
- **WHEN** the settings dialog renders the mode hint
- **THEN** the hint text explains tap-again-to-boost and that `1x` resets instead of boosting

#### Scenario: Scale label is translated
- **WHEN** the settings dialog renders the scale row
- **THEN** its label resolves from `feature.time-control.settings.scale` with no hardcoded fallback

### Requirement: Clamped provider apply and restore
Applying a multiplier SHALL use a provider that clamps the effective step after multiplication, and every reset path SHALL restore the default clamped provider form. The default form SHALL be documented at the restore call site since `Time` exposes no getter.

#### Scenario: Apply clamps extreme steps
- **WHEN** a high multiplier is applied during a hitch frame
- **THEN** the effective per-frame step is clamped instead of growing unbounded

### Requirement: Display-mode option

The feature SHALL expose a persisted per-feature display-mode option (`ConfigValue<String>`, HUD/popup values, default HUD) in its settings view, bound declaratively with no manual subscriptions for ordinary state.

#### Scenario: Display mode defaults to HUD

- **WHEN** the option has never been changed
- **THEN** behavior matches the pre-change HUD flow exactly

#### Scenario: Display mode persists

- **WHEN** the user selects popup mode and the game restarts
- **THEN** the option still reads popup

### Requirement: Shared row popup control surface

In popup mode with QuickAccess on, tapping the feature's QuickAccess button SHALL toggle popup visibility (opening if closed, hiding if already showing or clicked again). The popup SHALL display the shared horizontal control row (presets or slider) matching the HUD layout, rendered via a static layout function in `TimeControlHudView` (excluding the drag handle) with no title header, wrapped in a container with a black background, `rounded(unit(2))`, `padding(unit(1))`, and `gap(unit(1))`, anchored above or below the whole QuickAccess bar depending on the bar's screen position, sized per the feature UI scale setting, bound to the same speed, preset, boost, and mode signals as the HUD with automatic ownership. The popup controls SHALL support disabled state via a `canEdit` parameter (disabling controls when the feature is disabled or when the player is a net client). The popup SHALL NOT toggle enablement, SHALL contain no enable switch, and SHALL NOT propagate taps to any toggle path. Enable/disable SHALL remain available only via the settings dialog and FeatureCard.

#### Scenario: Tap opens popup without toggling

- **WHEN** the player taps the TimeControl QuickAccess button in popup mode with QuickAccess on while the feature is disabled and the popup is not showing
- **THEN** the popup opens and the feature remains disabled

#### Scenario: Clicking button again hides popup

- **WHEN** the player taps the TimeControl QuickAccess button in popup mode with QuickAccess on while the TimeControl popup is already showing
- **THEN** the popup closes and the feature enabled state is unchanged

#### Scenario: Popup drives speed while enabled

- **WHEN** the player uses the popup presets or slider while the feature is enabled as host
- **THEN** speed applies exactly as via the HUD controls

#### Scenario: Popup placement follows bar position

- **WHEN** the popup opens
- **THEN** it renders above or below the QuickAccess bar according to the bar's current screen position

#### Scenario: Popup follows scale

- **WHEN** the user changes the UI scale setting
- **THEN** popup content sizes update reactively per the same mappings as HUD elements

### Requirement: Disabled popup controls for clients

In popup mode, net clients opening the TimeControl popup SHALL see the normal controls visibly disabled rather than live, hidden, or replaced.

#### Scenario: Client opens popup

- **WHEN** a net client taps the TimeControl QuickAccess button in popup mode
- **THEN** the popup opens with its preset and slider controls disabled

