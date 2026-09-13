## 1. Native Popup Core

- [ ] 1.1 Convert `solim.overlay.Popup` to generic `Popup<T>` holding a private data signal, a provider function field, and a scene-hosted menu `Table`; `build()` returns a zero-footprint spacer and creates the render effect
- [ ] 1.2 Implement `children(provider)` storing the content provider and returning the instance for chaining
- [ ] 1.3 Implement `show(data, x, y)` (set internal signal, position, add to scene root, attach listeners) and `hide()` (remove from scene, detach listeners), both returning the instance and no-op when no scene exists
- [ ] 1.4 Verify generic inference through the chained `popup().children(...)` facade call; fall back to an explicit type witness if the compiler requires it

## 2. Positioning

- [ ] 2.1 Extract prefer-above/flip-below/clamp-XY placement into a scene-free pure function
- [ ] 2.2 Wire placement into `show()`: pack menu, compute clamped position from anchor, set position before scene add

## 3. Dismissal Trio

- [ ] 3.1 Implement tap-outside dismissal via scene-root capture listener with `hit()` re-test; swallow outside touches, pass inside touches through
- [ ] 3.2 Implement Back + Esc dismissal via scene key listener attached only while visible; consume the key event
- [ ] 3.3 Dismiss on `ResizeEvent` while visible and verify listener attach/detach lifecycle has no leaks across show/hide cycles

## 4. Chrome and Facade

- [ ] 4.1 Ship a default dark rounded menu background; verify existing `rounded()`/`border()` calls override it
- [ ] 4.2 Change the `popup()` facade to the generic scene-hosted form and update the placeholder `Popup` tests to the new behavior

## 5. Native Tests

- [ ] 5.1 Add unit tests for the pure clamp function (above fit, flip on overflow, edge clamping on both axes)
- [ ] 5.2 Add unit tests for provider rebuild per `show()` and headless no-ops with no scene
- [ ] 5.3 Add a test pinning fluent chaining (single instance through the full configuration chain)

## 6. Chat Adoption

- [ ] 6.1 Slim `ChatActionPopup` to the shared-instance shell: static instance, `PopupRequest` data, Copy/Reply/Translate provider rows, translation logic with height-cache invalidation
- [ ] 6.2 Rewire `openActions` to `menu.show(message, stageX, stageY)` and remove the bespoke dialog machinery (transparent `SolimDialog`, mod-side catcher and clamp)

## 7. Verification

- [ ] 7.1 Verify zero compilation errors across `mod` and `solim-core`
- [ ] 7.2 Run `./gradlew test` to verify the complete test suite passes
- [ ] 7.3 Eyeball-check in-game: menu placement near card, tap-outside dismissal, Esc/Back dismissal, full Copy/Reply/Translate flow
