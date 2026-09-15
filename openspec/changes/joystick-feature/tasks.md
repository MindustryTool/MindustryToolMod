## 1. Feature & Configuration Setup

- [ ] 1.1 Create \JoystickFeature.java\ extending \Feature\, registered in mod features with \enabledByDefault = false\.
- [ ] 1.2 Define \ConfigValue<T>\ fields in \JoystickFeature\ for \posX\, \posY\, \size\, \opacity\, and \showHandle\.
- [ ] 1.3 Add all required i18n translation keys with descriptive comments directly above them to \ssets/bundles/bundle.properties\.
- [ ] 1.4 Create \JoystickSettingsDialog.java\ and \JoystickSettingsView.java\ using declarative Solim UI (sliders for size/opacity, toggle for handle, reset button).

## 2. InputHandler Implementations

- [ ] 2.1 Implement \JoystickMobileInput.java\ subclassing \MobileInput\, overriding \updateMovement\ to drive movement purely from the joystick vector and decoupling camera panning.
- [ ] 2.2 Implement \JoystickDesktopInput.java\ subclassing \DesktopInput\, reading joystick vector in \updateMovement\ alongside mouse/keyboard.
- [ ] 2.3 Implement input swapping lifecycle in \JoystickFeature.onEnable()\ and \onDisable()\, calling \Vars.control.setInput(...)\ and restoring original input on disable.

## 3. Joystick UI Widget & Interaction

- [ ] 3.1 Implement \JoystickWidget.java\ rendering the fixed outer base and movable inner knob with touch pointer tracking.
- [ ] 3.2 Implement the reposition drag handle on \JoystickWidget\ that updates persisted \posX\ and \posY\ coordinates when dragged, hidden when \showHandle\ is false.
- [ ] 3.3 Implement double-tap detection on the joystick knob to immediately center \Core.camera.position\ onto the player unit.
- [ ] 3.4 Mount and manage lifecycle of \JoystickWidget\ on the HUD layer when active in a game.
