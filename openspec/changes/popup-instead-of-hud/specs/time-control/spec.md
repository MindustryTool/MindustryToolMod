## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Display-mode option

The feature SHALL expose a persisted per-feature display-mode option (`ConfigValue<String>`, HUD/popup values, default HUD) in its settings view, bound declaratively with no manual subscriptions for ordinary state.

#### Scenario: Display mode defaults to HUD

- **WHEN** the option has never been changed
- **THEN** behavior matches the pre-change HUD flow exactly

#### Scenario: Display mode persists

- **WHEN** the user selects popup mode and the game restarts
- **THEN** the option still reads popup

### Requirement: Stacked popup control surface

In popup mode with QuickAccess on, tapping the feature's QuickAccess button SHALL open a stacked vertical mini-panel popup (presets and slider content arranged for the popup idiom) anchored above or below the whole QuickAccess bar depending on the bar's screen position, sized per the feature UI scale setting, bound to the same speed, preset, boost, and mode signals as the HUD with automatic ownership. The popup SHALL NOT toggle enablement, SHALL contain no enable switch, and SHALL NOT propagate taps to any toggle path. Enable/disable SHALL remain available only via the settings dialog and FeatureCard.

#### Scenario: Tap opens popup without toggling

- **WHEN** the player taps the TimeControl QuickAccess button in popup mode with QuickAccess on while the feature is disabled
- **THEN** the popup opens and the feature remains disabled

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
