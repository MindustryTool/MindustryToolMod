## MODIFIED Requirements

### Requirement: Decoupled Movement and Camera

The system SHALL move the player unit in the direction of the joystick knob while the knob is held at the unit's speed, SHALL stop the unit when the knob is released, SHALL allow camera panning without moving the unit, and SHALL snap the camera onto the player unit when the knob is double-tapped. When the `FreeCameraFeature` is disabled, the camera SHALL smoothly follow the player unit during joystick movement, with temporary pan override support. When one finger holds the joystick knob and a second finger touches the screen, the system SHALL convert the second finger gesture into camera panning and SHALL suppress pinch-to-zoom.

#### Scenario: Knob moves the unit
- **WHEN** the player holds a joystick direction
- **THEN** the player unit moves in that direction at its speed

#### Scenario: Release stops the unit
- **WHEN** the player releases the knob
- **THEN** the unit stops moving

#### Scenario: Camera follows unit when free-camera disabled
- **WHEN** the player moves using the joystick and `FreeCameraFeature` is disabled
- **THEN** the camera smoothly tracks the player unit position

#### Scenario: Temporary pan override
- **WHEN** the player drags the map while `FreeCameraFeature` is disabled
- **THEN** the camera pans freely to inspect the world, and smoothly returns to the unit 0.5s after touch release

#### Scenario: Second finger pans instead of zooming while joystick is held
- **WHEN** the player holds the joystick knob with one finger and drags the screen with a second finger
- **THEN** the second finger movement pans the camera and pinch-to-zoom is suppressed

#### Scenario: Normal pinch zoom when joystick idle
- **WHEN** the player performs a two-finger pinch gesture while the joystick knob is not held
- **THEN** standard camera zoom scaling occurs

#### Scenario: Free camera mode allows independent panning
- **WHEN** `FreeCameraFeature` is enabled
- **THEN** the camera does not follow the unit during joystick movement and only moves when panned

#### Scenario: Double-tap recenters the camera
- **WHEN** the player double-taps the knob within the tap interval
- **THEN** the camera position snaps directly onto the player unit

### Requirement: Customization and Settings

The system SHALL provide a scale slider for the joystick diameter, an opacity slider for its transparency, a free camera toggle bound to `FreeCameraFeature.get().enabled()`, a reset button restoring the default position, and SHALL be disabled by default.

#### Scenario: Size slider changes diameter
- **WHEN** the player adjusts the size slider
- **THEN** the joystick diameter updates reactively

#### Scenario: Opacity slider changes transparency
- **WHEN** the player adjusts the opacity slider
- **THEN** the joystick transparency updates reactively

#### Scenario: Free camera toggle
- **WHEN** the player toggles the free camera setting in the joystick settings view
- **THEN** the `FreeCameraFeature` enabled state updates reactively

#### Scenario: Reset restores default position
- **WHEN** the player presses the reset button
- **THEN** the joystick returns to the default bottom-left position

#### Scenario: Disabled by default
- **WHEN** the mod is loaded for the first time
- **THEN** the joystick feature is disabled
