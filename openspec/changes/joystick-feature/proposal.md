## Why

In vanilla Mindustry mobile, unit movement is coupled to camera panning: swiping the screen to look around the map forces the player unit to walk toward the camera center. This makes strategic observation and precise unit navigation frustrating on touch devices. Introducing a virtual on-screen joystick with a decoupled camera allows players to move freely while independently panning and scouting across the map on both mobile and desktop.

## What Changes

- Add a new \JoystickFeature\ extending \Feature\, disabled by default (\enabledByDefault = false\).
- Implement an on-screen virtual joystick UI element positioned at bottom-left by default, with persisted coordinates (\joystickX\, \joystickY\).
- Add a reposition drag handle on the joystick with a settings toggle to hide/show the handle (preventing accidental dragging during gameplay).
- Add customization options in settings: scale slider, opacity/transparency slider, handle visibility toggle, and a reset position button.
- Override \Vars.control.input\ via \Vars.control.setInput(...)\ using custom input handlers:
  - On Mobile: Subclass \MobileInput\ (\JoystickMobileInput\) to decouple camera panning from unit movement (\unit.movePref\ driven purely by joystick vector, free camera panning without unit following).
  - On Desktop: Subclass \DesktopInput\ (\JoystickDesktopInput\) to support virtual joystick input alongside mouse/keyboard controls.
- Implement camera snap shortcut: double-tapping the joystick knob instantly snaps the camera back to the player unit.
- Provide full internationalization (i18n) for all settings, labels, and tooltips in \ssets/bundles/bundle.properties\.

## Capabilities

### New Capabilities
- \irtual-joystick\: Provides an on-screen virtual joystick to control player unit movement independently from camera panning on both mobile and desktop platforms.

### Modified Capabilities
- (None)

## Impact

- Core input handling: Replaces \Vars.control.input\ with modded input handlers when the feature is enabled; restores original handler when disabled.
- UI overlay: Adds a floating Solim widget on the HUD layer.
- Settings: Adds configuration keys to mod settings and translations to \undle.properties\.
