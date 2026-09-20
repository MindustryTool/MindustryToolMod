## 1. Stock Detach Parity (Desktop)

- [ ] 1.1 Add vanilla detach-camera chase branch to `ModDesktopInput.updateMovement()` when mod freeCam is off
- [ ] 1.2 Verify mod-ON behavior (frozen unit, WASD pans camera) is unchanged when vanilla detach is also on
- [ ] 1.3 Verify joystick blending and autoplay yielding still apply when vanilla detach is off

## 2. Snap Cleanup

- [ ] 2.1 Clear vanilla `spectating` target in `FreeCameraFeature.snapToPlayer()` alongside the panning reset
- [ ] 2.2 Cover both input handler types (desktop `panning`/`spectating`, mobile pan-delay) with instanceof guards

## 3. Verification

- [ ] 3.1 Add headless tests: unit chases camera with mod off + vanilla detach on; camera stays on player after snap with spectating set
- [ ] 3.2 Run `:mod:test` for freecamera + input suites and confirm green
- [ ] 3.3 Review against AGENTS.md checklist (no Java 9+ APIs, `arc.util.Nullable`, no fully qualified names, no `signal.get()` in builders)
