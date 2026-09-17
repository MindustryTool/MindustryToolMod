## Context

Mindustry 160.4 on Android enabled R8 optimization (`minifyEnabled = true`). As a result, un-overridden desugared interface default methods on `InputHandler` (`pinch(Vec2, Vec2, Vec2, Vec2)` and `pinchStop()`) are devirtualized and marked `final`.

`JoystickMobileInput` directly subclasses `MobileInput` (which extends `InputHandler`) and overrides `pinch(...)` and `pinchStop()`. During Android startup, ART bytecode verification fails on `JoystickMobileInput` with `java.lang.LinkageError`, crashing the game.

## Goals / Non-Goals

**Goals:**
- Eliminate `pinch(...)` and `pinchStop()` from `JoystickMobileInput`'s class declaration so ART bytecode verification succeeds without error.
- Preserve identical mobile touch handling: when the joystick knob is held, dragging a second finger continues to pan the camera instead of zooming.
- Maintain full compatibility with Java 8 runtime constraints and Checkstyle.

**Non-Goals:**
- Modifying joystick HUD UI, layouts, settings, or desktop input behaviors.
- Modifying vanilla input handling outside the active joystick session.

## Decisions

### Decision 1: Use an Independent `GestureListener` Delegate in `JoystickMobileInput`

Instead of declaring `pinch(...)` directly on `JoystickMobileInput`, we create a private inner class `JoystickGestureListener implements GestureDetector.GestureListener`.

Because `JoystickGestureListener` does not inherit from `InputHandler`, its `pinch(...)` method is a pure interface implementation with no superclass finality restrictions.

In `JoystickMobileInput`:
- Override `add()`:
  ```java
  @Override
  public void add() {
      super.add();
      Core.input.removeProcessor(detector);
      detector = new GestureDetector(20, 0.5f, 2, 0.15f, new JoystickGestureListener());
      Core.input.addProcessor(detector);
  }
  ```
- In `JoystickGestureListener`:
  - `pinch(...)`: When `feature.isKnobHeld()`, convert the non-knob pointer drag into `JoystickMobileInput.this.pan(...)` and return `true`. Otherwise return `false`.
  - `pinchStop()`: Reset panning states.
  - Delegate `pan`, `panStop`, `zoom`, `touchDown`, `tap`, `longPress` to `JoystickMobileInput.this`.

### Alternatives Considered
- *Remove pinch handling completely*: Trivial fix, but breaks the user experience on mobile where holding the joystick knob and dragging another finger fails to pan the camera.
- *Handle raw touch events in `touchDragged`*: Bypasses `GestureDetector`, but risks desynchronizing with Arc's internal gesture state machine.

## Risks / Trade-offs

- **[Risk] Processor Leak in `add()` / `remove()`** → **Mitigation**: `super.add()` registers the base detector; we immediately remove it via `Core.input.removeProcessor(detector)` before creating and adding our delegating detector. `super.remove()` removes `detector` from `Core.input`.
