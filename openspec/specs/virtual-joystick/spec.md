# virtual-joystick Specification

## Purpose

Provide an on-screen virtual joystick that decouples player unit movement from camera panning, letting players move freely while independently panning and scouting across the map on mobile and desktop, with a repositionable, customizable widget.
## Requirements
### Requirement: Virtual Joystick Display

The system SHALL render an on-screen joystick composed of an outer base and a movable inner knob on the game HUD, positioned at the bottom-left of the screen by default, and SHALL persist its screen position across sessions.

#### Scenario: Base and knob rendered

- **WHEN** the feature is enabled during an active game
- **THEN** an outer base with a movable inner knob is rendered on the HUD layer

#### Scenario: Renders at its layout position

- **WHEN** the joystick widget is drawn
- **THEN** the base and knob are rendered at the widget's own layout position, aligned with where touch input is received

#### Scenario: Default bottom-left position

- **WHEN** the feature is enabled with no saved position
- **THEN** the joystick appears at the default bottom-left offset

#### Scenario: Position survives restart

- **WHEN** the joystick is repositioned and the game restarts
- **THEN** the joystick reappears at the saved position

### Requirement: Reposition Handle

The system SHALL provide a drag handle adjacent to the joystick base that repositions the entire widget, SHALL provide a settings toggle to show or hide the handle, and SHALL NOT reposition the widget through joystick touches while the handle is hidden.

#### Scenario: Dragging the handle repositions the widget

- **WHEN** the player drags the handle
- **THEN** the widget moves with the pointer and the new position is persisted

#### Scenario: Handle visibility toggled

- **WHEN** the player toggles the show-handle setting
- **THEN** the handle is shown or hidden accordingly

#### Scenario: Hidden handle prevents repositioning

- **WHEN** the handle is hidden and the player touches the joystick
- **THEN** the widget is not repositioned and the touch only drives movement

### Requirement: Decoupled Movement and Camera

The system SHALL move the player unit in the direction of the joystick knob while the knob is held at the unit's speed, SHALL stop the unit when the knob is released, SHALL allow camera panning without moving the unit, and SHALL snap the camera onto the player unit when the knob is double-tapped. When the free-camera setting is disabled, the camera SHALL smoothly follow the player unit during joystick movement, with temporary pan override support. When one finger holds the joystick knob and a second finger touches the screen, the system SHALL convert the second finger gesture into camera panning and SHALL suppress pinch-to-zoom.

#### Scenario: Knob moves the unit
- **WHEN** the player holds a joystick direction
- **THEN** the player unit moves in that direction at its speed

#### Scenario: Release stops the unit
- **WHEN** the player releases the knob
- **THEN** the unit stops moving

#### Scenario: Camera follows unit when free-camera disabled
- **WHEN** the player moves using the joystick and free-camera is disabled
- **THEN** the camera smoothly tracks the player unit position

#### Scenario: Temporary pan override
- **WHEN** the player drags the map while free-camera is disabled
- **THEN** the camera pans freely to inspect the world, and smoothly returns to the unit 0.5s after touch release

#### Scenario: Second finger pans instead of zooming while joystick is held
- **WHEN** the player holds the joystick knob with one finger and drags the screen with a second finger
- **THEN** the second finger movement pans the camera and pinch-to-zoom is suppressed

#### Scenario: Normal pinch zoom when joystick idle
- **WHEN** the player performs a two-finger pinch gesture while the joystick knob is not held
- **THEN** standard camera zoom scaling occurs

#### Scenario: Free camera mode allows independent panning
- **WHEN** free-camera is enabled
- **THEN** the camera does not follow the unit during joystick movement and only moves when panned

#### Scenario: Double-tap recenters the camera
- **WHEN** the player double-taps the knob within the tap interval
- **THEN** the camera position snaps directly onto the player unit

### Requirement: Input Handler Override

The system SHALL replace `Vars.control.input` via `Vars.control.setInput(...)` while the feature is enabled, using a `MobileInput` subclass on mobile and a `DesktopInput` subclass on desktop, and SHALL restore the original handler when the feature is disabled.

#### Scenario: Mobile uses the joystick mobile handler

- **WHEN** the feature is enabled on a mobile platform
- **THEN** `Vars.control.input` is a `JoystickMobileInput` instance

#### Scenario: Desktop uses the joystick desktop handler

- **WHEN** the feature is enabled on a desktop platform
- **THEN** `Vars.control.input` is a `JoystickDesktopInput` instance

#### Scenario: Disable restores the original handler

- **WHEN** the feature is disabled
- **THEN** the original `InputHandler` captured before the swap is restored

### Requirement: Customization and Settings

The system SHALL provide a scale slider for the joystick diameter, an opacity slider for its transparency, a free camera toggle, a reset button restoring the default position, and SHALL be disabled by default.

#### Scenario: Size slider changes diameter
- **WHEN** the player adjusts the size slider
- **THEN** the joystick diameter updates reactively

#### Scenario: Opacity slider changes transparency
- **WHEN** the player adjusts the opacity slider
- **THEN** the joystick transparency updates reactively

#### Scenario: Free camera toggle
- **WHEN** the player toggles the free camera setting in the joystick settings view
- **THEN** the free-camera configuration in ModSettings updates reactively

#### Scenario: Reset restores default position
- **WHEN** the player presses the reset button
- **THEN** the joystick returns to the default bottom-left position

#### Scenario: Disabled by default
- **WHEN** the mod is loaded for the first time
- **THEN** the joystick feature is disabled

### Requirement: Internationalization

The system SHALL resolve all user-visible joystick strings (settings labels, descriptions, buttons, and tooltips) from `bundle.properties`.

#### Scenario: Settings strings resolve from the bundle

- **WHEN** the joystick settings UI is displayed
- **THEN** its labels, descriptions, and buttons resolve from translation keys rather than hardcoded text

#### Scenario: New keys carry translator comments

- **WHEN** a new user-visible joystick string is introduced
- **THEN** a corresponding key with a descriptive translator comment is added to `bundle.properties`

