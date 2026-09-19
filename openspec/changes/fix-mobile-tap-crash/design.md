## Context

On mobile Android devices, user interactions such as tapping tiles in Mindustry dispatch through Arc's `InputMultiplexer` -> `GestureDetector` -> `ModGestureListener` -> `MobileInput.tap` -> `Call.tileTap` -> `InputHandler.tileTap` -> `Events.fire(TapEvent)`.

In Arc's `Events.fire(Class<?> ctype, T type)`, event listeners are dispatched via a direct array iteration:
```java
Seq<Cons<?>> listeners = events.get(ctype);
if (listeners != null) {
    int len = listeners.size;
    Cons[] items = listeners.items;
    for (int i = 0; i < len; i++) {
        items[i].get(type);
    }
}
```
If a listener removes itself or another listener during `fire`, `Seq.remove` decrements `size`, shifts elements down, and sets `items[size] = null`. Because `len` was pre-cached before the loop, the loop proceeds to the old length and attempts to invoke `items[i].get(type)` on the `null` entry, throwing `NullPointerException`.

In `MapPositionPicker`:
1. One-shot listeners called `Events.remove(TapEvent.class, holder[0])` directly inside the event listener.
2. Multiple invocations of `MapPositionPicker.pick()` without tapping left duplicate listeners in the `Seq`.

Additionally, `ModMobileInput` delegated gestures to vanilla `MobileInput` without exception handling, allowing any uncaught runtime exception in tile tap logic or event subscribers to crash the Android OpenGL render thread.

## Goals / Non-Goals

**Goals:**
- Eliminate the `NullPointerException` during `TapEvent` dispatch by preventing concurrent mutation of Arc's `Seq` during `Events.fire`.
- Ensure `MapPositionPicker` maintains at most one active listener, cleanly cancelling any prior listeners before adding a new one.
- Guard `ModMobileInput.tap` with exception handling so that any unexpected error in downstream tile tapping or event processing logs a warning and returns `false` instead of crashing the game.
- Maintain 100% backward compatibility with existing tap and picker behavior.

**Non-Goals:**
- Rewriting Arc's internal `Events` engine (which is part of the external engine library).
- Modifying `solim-core` or other modules unrelated to the crash.

## Decisions

### Decision 1: Single-Tracked Listener with Deferred Removal in `MapPositionPicker`
- **Choice**: Store the currently active `Cons<TapEvent>` in a private static field in `MapPositionPicker`. When `pick()` is called, immediately invoke `cancel()` to detach any previous listener. Inside the listener, mark `handled = true`, call `cancel()`, and use `Core.app.post(() -> Events.remove(TapEvent.class, listener))` (with fallback to immediate removal when `Core.app == null` in unit tests).
- **Rationale**: `Core.app.post` schedules the removal on the next main loop turn after the current event dispatch completes, ensuring `Events.fire` never encounters an altered array during iteration. The `handled` boolean flag prevents double-execution if another event fires in the same frame.
- **Alternatives Considered**:
  - *Immediate removal*: Causes the exact `NullPointerException` when multiple listeners exist.
  - *No removal / permanent listener*: Leaks memory and triggers picker on unintended future taps.

### Decision 2: Guarded Delegation in `ModMobileInput.tap`
- **Choice**: Override `tap(float x, float y, int count, KeyCode button)` in `ModMobileInput` to wrap `super.tap(x, y, count, button)` in `try-catch (Throwable t)`. Log errors with `Log.err` and return `false`.
- **Rationale**: Mobile input gestures run directly on the UI/render thread. An unhandled exception crashes the process. By catching errors at the input boundary, the game recovers gracefully even if external game or mod code throws an exception during tap handling.
- **Alternatives Considered**:
  - *Uncaught delegation*: Allows unexpected exceptions to crash the entire application.

## Risks / Trade-offs

- [Deferred removal delay] → The listener might theoretically receive another event between trigger and `Core.app.post` execution.
  *Mitigation*: The `handled` boolean flag ensures subsequent events in the same frame are ignored immediately.
- [Hidden errors in tap] → Swallowing exceptions might conceal bugs in custom tap handling.
  *Mitigation*: Log the complete stack trace via `Log.err("Error in ModMobileInput.tap", t)` so any unexpected issues remain visible in logs without crashing the user session.
