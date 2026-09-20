## 1. Localization

- [x] 1.1 Add `status.free-camera.enabled` key plus translator comment to `assets/bundles/bundle.properties`
- [x] 1.2 Confirm no existing key already covers the "free camera enabled" concept (reuse check)

## 2. Indicator HUD View

- [x] 2.1 Create `FreeCameraHudView` Solim component with top-center `hud().position(screenWidth/2, screenHeight-50)` pill
- [x] 2.2 Bind pill `visible` reactively to enabled + `hudfrag.shown` + `state.isGame()` with no `signal.get()` in `build()`
- [x] 2.3 Style pill with `Styles.black8` background, `rounded(unit(2))`, `Pal.accent` label, pass-through touchable
- [x] 2.4 Handle `ResizeEvent` with `keepInScreen()` so the pill stays on screen after resize/rotation

## 3. Feature Lifecycle Wiring

- [x] 3.1 Mount HUD to `Vars.ui.hudGroup` via `Core.app.post` in `FreeCameraFeature.onEnable()`
- [x] 3.2 Unmount and dispose HUD in `FreeCameraFeature.onDisable()` without touching camera/input logic
- [x] 3.3 Verify no `:solim-runtime` references, no Java 9+ runtime APIs, and `@Nullable` usage where applicable

## 4. Verification

- [x] 4.1 Verify pill shows on enable, hides on disable, hides outside gameplay, and survives resize in-game
- [x] 4.2 Verify label resolves from bundle (no hardcoded text) and input still passes through the pill
