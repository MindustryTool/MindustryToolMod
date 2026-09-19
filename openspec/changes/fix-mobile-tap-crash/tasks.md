## 1. Lifecycle-Safe Map Coordinate Picker

- [ ] 1.1 Update `MapPositionPicker` to track a single `currentListener` and provide `cancel()`
- [ ] 1.2 Implement deferred removal via `Core.app.post` upon tap capture in `MapPositionPicker`
- [ ] 1.3 Add unit tests in `MapPositionPickerTest` verifying listener deduplication, single execution, and non-crashing cancellation

## 2. Fault-Tolerant Mobile Gesture Handling

- [ ] 2.1 Override `tap` in `ModMobileInput` with exception handling wrapping `super.tap`
- [ ] 2.2 Add unit tests in `ModInputTest` verifying `ModMobileInput.tap` isolates exceptions and returns false

## 3. Verification

- [ ] 3.1 Run `:mod:test` to verify all mod tests pass
- [ ] 3.2 Run `:mod:check` to verify Checkstyle and formatting rules
