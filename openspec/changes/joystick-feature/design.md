## Context

Vanilla Mindustry on touch devices ties unit movement directly to camera position: \	argetPos.set(Core.camera.position)\. This makes independent camera exploration impossible because panning pulls the player unit along. This feature decouples unit movement and camera panning by introducing an on-screen virtual joystick and replacing \Vars.control.input\ with dedicated subclasses of \MobileInput\ and \DesktopInput\.

## Goals / Non-Goals

**Goals:**
- Provide a fixed-base virtual joystick on the screen with persisted coordinates (\posX\, \posY\).
- Provide an optional drag handle to reposition the joystick, with a settings toggle to hide/show it.
- Decouple camera panning from unit movement: panning moves the camera without moving the unit; moving the joystick moves the unit without moving the camera.
- Support double-tapping the joystick knob to snap the camera back to the player unit.
- Support both mobile and desktop platforms by subclassing \MobileInput\ on mobile and \DesktopInput\ on desktop.
- Expose settings for scale/size, opacity/transparency, handle visibility, and position reset.

**Non-Goals:**
- We do not alter building, mining, or shooting mechanics; touch taps on buildings, tiles, or enemies retain standard behavior.

## Decisions

1. **InputHandler Override Pattern:**
   - On feature enable: capture \originalInput = Vars.control.input\.
   - Instantiate \JoystickMobileInput\ if \Vars.mobile\, otherwise \JoystickDesktopInput\.
   - Call \Vars.control.setInput(customInput)\, which unbinds the old input, preserves the selected block, and binds the new input processors.
   - On feature disable: call \Vars.control.setInput(originalInput)\ to restore vanilla behavior cleanly.

2. **Decoupled Movement & Camera Mechanics:**
   - \JoystickMobileInput\ overrides \updateMovement(Unit unit)\:
     - When joystick is actively pressed: \movement.set(joystickVector).scl(unit.speed())\ and \unit.movePref(movement)\.
     - When joystick is idle: \movement.setZero()\ and \unit.movePref(movement)\ (unit holds position).
     - \	argetPos\ is not updated with \Core.camera.position\.
   - \pan(...)\ moves \Core.camera.position\ freely across the world, leaving the unit unaffected.

3. **Recenter Camera on Double-Tap:**
   - The joystick knob detects touch taps. If a tap occurs within 300ms of the previous tap inside the knob radius, \Core.camera.position.set(Vars.player.x, Vars.player.y)\ snaps the camera immediately onto the player.

4. **UI & Persistence:**
   - Joystick base is fixed at \ConfigValue<Float> posX, posY\ (default: bottom-left offset).
   - When \showHandle\ is enabled, a drag handle icon is displayed above/beside the base. Dragging the handle updates \posX\ and \posY\.
   - When \showHandle\ is disabled, the handle is hidden and touch events strictly control movement, preventing accidental repositioning.
   - Sliders for \size\ and \opacity\ update Solim reactive signals to resize and adjust alpha dynamically.

## Risks / Trade-offs

- **Risk**: Other mods or game updates replacing \Vars.control.input\.
  - **Mitigation**: We use Mindustry's standard \Vars.control.setInput(...)\ API and safely restore the original instance upon feature disabling.
- **Risk**: Units walking off-screen on decoupled camera.
  - **Mitigation**: Double-tapping the joystick knob instantly snaps the camera back to the player unit.
