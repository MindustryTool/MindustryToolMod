## 1. Frame-Drawn Label

- [x] 1.1 Register `Trigger.draw` hook in `FreeCameraFeature` constructor with enabled + `hudfrag.shown` + `isGame()` guards
- [x] 1.2 Draw `status.free-camera.enabled` text with `Pal.accent`, `Align.center`, camera-relative top-center anchor and zoom-relative scale
- [x] 1.3 Draw dark translucent backdrop rect sized from the returned `GlyphLayout` at `Layer.overlayUI` with `Draw.reset()` after

## 2. Overlay Removal

- [x] 2.1 Delete `FreeCameraHudView` and remove `mountHud`/`unmountHud` wiring from `FreeCameraFeature`
- [x] 2.2 Confirm no remaining references to the HUD element name, resize listener, or `hudGroup` in the freecamera package

## 3. Verification

- [x] 3.1 Run `:mod:test` for freecamera + input suites and confirm green
- [x] 3.2 Review against AGENTS.md checklist (bundle key reused, no hardcoded text, no Java 9+ APIs, `arc.util.Nullable`, no fully qualified names)
