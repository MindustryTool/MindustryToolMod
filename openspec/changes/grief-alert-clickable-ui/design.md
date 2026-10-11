## Context

`GriefAlertFeature` detects when a player builds an explosive reactor within range of their allied core. Previously, it posted notifications to Mindustry's multiplayer chat (`Vars.ui.chatfrag.addMessage`) or fallback HUD toasts (`Vars.ui.hudfrag.showToast`). The user requested completely eliminating chat messaging and vanilla toasts in favor of a full UI-first experience: an interactive, clickable Solim HUD banner at the top of the screen that jumps the camera to the target tile when clicked, draws a pulsing highlight indicator around the target block, and provides an explicit dismiss button.

## Goals / Non-Goals

**Goals:**
- Replace chat messages and generic toasts with an interactive Solim HUD alert component (`GriefAlertHudView`).
- Top-center screen placement, displayed when an alert is active and auto-dismissed after 7 seconds.
- Clicking the banner moves the camera immediately to the world position of the offending block and sets desktop panning so the camera doesn't snap back.
- Render a pulsing target ring in the game world at the target tile's coordinates for 3 seconds upon camera jump.
- Provide a clean close `[✕]` button to dismiss the banner without moving the camera.

**Non-Goals:**
- Storing or displaying a permanent history log of all past grief alerts.
- Changing reactor detection triggers or distance thresholds.

## Decisions

### 1. Alert State Management via Signal
`GriefAlertFeature` will hold a reactive `Signal<@Nullable GriefAlertInfo> activeAlert = Signal.of(null)`.
`GriefAlertInfo` record/class contains:
- `String builderName`
- `String blockName`
- `int tileX, tileY`
- `float worldX, worldY`
- `long timestamp`

When a new grief alert triggers:
- `activeAlert.set(newAlert)`
- A 7-second timer auto-clears the alert if it hasn't been dismissed or replaced.

### 2. HUD Banner View Structure (`GriefAlertHudView`)
Using Solim declarative UI:
- Solim `hud()` container mounted to `Vars.ui.hudGroup` when `GriefAlertFeature` is enabled.
- Positioned top-center (`x = Units.screenWidth() / 2f`, `y = Units.screenHeight() - 60f`).
- Binds to `feature.activeAlertSignal()`, showing a styled red/warning card with:
  - Warning icon (`Icon.warning` / `triangle-alert.png`)
  - Warning message text
  - Click listener on card that triggers `feature.jumpToAlert(alert)`
  - Dedicated dismiss button (`[✕]`) using `WebStyles.ghost()` that clears `feature.dismissAlert()`
- Visibility bound to `Signal.computed(() -> feature.isEnabled() && feature.hasActiveAlert() && Vars.state.isGame() && Vars.ui.hudfrag.shown)`.

### 3. Camera Navigation & Panning
On click:
- `Core.camera.position.set(alert.worldX, alert.worldY)`
- To avoid immediate snapback to the player:
  - For `DesktopInput`: set `panning = true` and `spectating = null`.
  - For `ModMobileInput`: set `spectating = null`.
- Activate world highlight marker at `(alert.worldX, alert.worldY)` for 3 seconds.
- Dismiss the alert banner.

### 4. Target World Marker via Trigger.draw
`GriefAlertFeature` subscribes to `Events.run(Trigger.draw, this::drawHighlight)`.
- If a target highlight is active (`now - jumpTime < 3000ms`), calculate pulsing alpha and radius using `Time.millis()` or `Time.time`.
- Render a red/accent circle using `Lines.stroke(...)` and `Lines.circle(targetWorldX, targetWorldY, radius)`.
- Ensure culling/bounds check and layer consistency (e.g. `Layer.overlayUI` or `Layer.plans`).

## Risks / Trade-offs

- **[Desktop Camera Snapback]** → Mindustry's `DesktopInput` normally resets camera position if not in panning mode.
  *Mitigation*: Set `desktopInput.panning = true` upon camera jump so the camera stays on the target until the user navigates.
- **[Multiple Rapid Alerts]** → If another alert arrives while one is showing.
  *Mitigation*: Replace `activeAlert` with the newer alert and reset the auto-dismiss timer. Existing 3-second throttle prevents rapid spam.
