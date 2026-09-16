# Virtual Joystick

## Requirements

1. **Virtual Joystick Display:**
   - MUST render an on-screen joystick composed of an outer base and an inner movable knob.
   - Default position MUST be at the bottom-left of the screen.
   - Position coordinates (X and Y) MUST persist across game sessions.

2. **Reposition Handle:**
   - MUST provide a drag handle adjacent to the joystick base to reposition the entire widget on screen.
   - MUST provide a toggle in settings to show or hide the drag handle.
   - When the drag handle is hidden, touch interactions on the joystick MUST NOT reposition the widget.

3. **Decoupled Movement and Camera:**
   - Moving the joystick knob MUST move the player unit in the corresponding directional vector at the unit's speed.
   - When the joystick is released, the unit MUST stop moving.
   - Swiping the screen to pan the camera MUST move the camera freely across the map WITHOUT moving the player unit.
   - Double-tapping the joystick knob MUST snap \Core.camera.position\ directly onto the player unit.

4. **Input Handler Override:**
   - When the feature is enabled, the mod MUST override \Vars.control.input\ via \Vars.control.setInput(...)\.
   - On mobile platforms, it MUST use a custom \MobileInput\ subclass.
   - On desktop platforms, it MUST use a custom \DesktopInput\ subclass.
   - When the feature is disabled, the mod MUST restore the original \InputHandler\ instance.

5. **Customization & Settings:**
   - MUST include a scale slider to adjust joystick diameter.
   - MUST include an opacity slider to adjust joystick transparency.
   - MUST include a reset button to restore default bottom-left position.
   - Feature MUST be disabled by default (\enabledByDefault = false\).

6. **Internationalization:**
   - All settings labels, descriptions, and button text MUST be translatable in \undle.properties\.
