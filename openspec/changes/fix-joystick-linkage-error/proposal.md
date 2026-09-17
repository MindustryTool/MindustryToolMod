## Why

In Mindustry build 160.4+, Android builds enable R8 optimization and minification. Because `mindustry.input.InputHandler` implements `arc.input.GestureDetector.GestureListener` with desugared default interface methods and no class in vanilla Mindustry overrides `pinch` or `pinchStop`, R8 marks `InputHandler.pinch(...)` and `InputHandler.pinchStop()` as `final`.

When `mindustrytool.features.joystick.JoystickMobileInput` (a subclass of `MobileInput` -> `InputHandler`) directly overrides `pinch(...)` and `pinchStop()`, Android Dalvik/ART fails class verification on startup during `loadClass()` with `java.lang.LinkageError`, crashing Mindustry for all Android users even when the joystick feature is disabled.

## What Changes

- Refactor `JoystickMobileInput` to remove direct overrides of `pinch(...)` and `pinchStop()`, eliminating the Dalvik class verification conflict.
- Implement a dedicated `GestureListener` adapter inside `JoystickMobileInput` that wraps `detector` and intercepts `pinch` / `pinchStop` without inheriting from `InputHandler`.
- Preserve the existing mobile behavior where holding the joystick knob allows a second finger to pan the camera while suppressing pinch-to-zoom.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `virtual-joystick`: Clarify that mobile gesture delegation avoids directly overriding `InputHandler` methods to remain compatible with Android bytecode verification.

## Impact

- `mindustrytool.features.joystick.JoystickMobileInput`: Removes `pinch(...)` and `pinchStop()` methods from the class definition and installs a delegating `GestureDetector` with an adapter implementing `GestureListener`.
- Fixes startup crash on Mindustry 160.4+ on Android (Dalvik/ART).
- No API changes or user-facing setting changes.
