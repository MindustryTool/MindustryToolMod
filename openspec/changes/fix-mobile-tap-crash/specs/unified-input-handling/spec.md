## ADDED Requirements

### Requirement: Fault-Tolerant Mobile Gesture Processing
`ModMobileInput` SHALL intercept touch gestures and isolate downstream event dispatch and game logic errors from crashing the mobile render thread.

#### Scenario: Uncaught exception in mobile tap does not crash
- **WHEN** an uncaught exception or runtime error occurs during `super.tap()` execution or downstream `Call.tileTap` / `Events.fire`
- **THEN** `ModMobileInput.tap` catches the exception, logs the error, and returns `false` without terminating the game thread

#### Scenario: Normal tap behavior preserved
- **WHEN** a tap gesture is performed on a valid tile and no exception occurs
- **THEN** `ModMobileInput.tap` returns the result of vanilla `MobileInput.tap()`
