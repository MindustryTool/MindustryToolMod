## Why

On Android mobile builds, tapping tiles can trigger an unhandled NullPointerException inside arc.Events.fire(TapEvent) from ModMobileInput.tap -> MobileInput.tap -> Call.tileTap -> InputHandler.tileTap.

Arc's Events.fire loops over listeners using a cached size (int len = listeners.size;) against a raw backing array (items). If a listener removes itself synchronously during event dispatch, Arc's Seq.remove shifts subsequent items down and sets items[size] to null. As the loop continues to len, it encounters items[i] == null and throws a NullPointerException. In MapPositionPicker, one-shot listeners removed themselves directly inside TapEvent callbacks and duplicate listeners accumulated if the picker was opened multiple times without a tap. Furthermore, ModMobileInput.tap lacked exception handling, allowing any downstream event or game exception to crash the Android OpenGL render thread.

## What Changes

- **Lifecycle-safe Map Coordinate Picker**:
  - Track active picker listener to prevent accumulation of multiple concurrent listeners.
  - Automatically cancel and safely detach any existing listener before registering a new one.
  - Defer listener removal using Core.app.post so Events.remove is never executed synchronously while Events.fire is iterating over listeners.
  - Add explicit cancel() API to allow callers or dialog closures to safely detach listeners.
- **Fault-tolerant Mobile Gesture Handling**:
  - In ModMobileInput.tap, wrap delegation to super.tap(x, y, count, button) in a try-catch block that logs errors and returns false, preventing render loop crashes.
  - Add defensive exception isolation to longPress, pan, and zoom handlers.

## Capabilities

### New Capabilities
*(None)*

### Modified Capabilities
- unified-input-handling: Add fault-tolerant gesture handling requirements to ModMobileInput so exceptions during gesture dispatch do not crash the game.
- god-mode: Update Interactive Map Coordinate Picker requirements to guarantee single-listener tracking, prior listener cancellation, and deferred unregistration.

## Impact

- mindustrytool.features.godmode.MapPositionPicker
- mindustrytool.input.ModMobileInput
- mindustrytool.features.godmode.MapPositionPickerTest
- mindustrytool.input.ModInputTest
