## 1. Refactor Joystick Mobile Input Gesture Handling

- [x] 1.1 Remove direct `pinch` and `pinchStop` method overrides from `JoystickMobileInput`
- [x] 1.2 Implement `add()` override in `JoystickMobileInput` with a dedicated `GestureListener` adapter delegating gesture events
- [x] 1.3 Verify compilation and checkstyle passes cleanly via Gradle
