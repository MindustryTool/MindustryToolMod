## MODIFIED Requirements

### Requirement: Input Handler Override

The system SHALL replace `Vars.control.input` via `Vars.control.setInput(...)` while the feature is enabled, using a `MobileInput` subclass on mobile and a `DesktopInput` subclass on desktop, and SHALL restore the original handler when the feature is disabled. The `MobileInput` subclass SHALL NOT directly override any method marked final in `InputHandler` on Android (such as `pinch` or `pinchStop`), delegating gesture interception to an independent `GestureListener` instead.

#### Scenario: Mobile uses the joystick mobile handler

- **WHEN** the feature is enabled on a mobile platform
- **THEN** `Vars.control.input` is a `JoystickMobileInput` instance

#### Scenario: Desktop uses the joystick desktop handler

- **WHEN** the feature is enabled on a desktop platform
- **THEN** `Vars.control.input` is a `JoystickDesktopInput` instance

#### Scenario: Disable restores the original handler

- **WHEN** the feature is disabled
- **THEN** the original `InputHandler` captured before the swap is restored

#### Scenario: Android bytecode verification passes

- **WHEN** the mod is loaded on Android Mindustry 160.4+
- **THEN** `JoystickMobileInput` loads without `LinkageError`
