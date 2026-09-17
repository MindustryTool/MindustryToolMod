## Context

Mindustry's rendering pipeline executes sequentially each frame:
1. World terrain, buildings, units, and effects are rendered into the main framebuffer (`Vars.renderer.draw()`).
2. Mindustry fires `EventType.Trigger.uiDrawBegin`.
3. The UI stage (`Core.scene.draw()`) renders all HUD fragments, dialogs, overlays, and tooltips on top of the world.
4. Mindustry fires `EventType.Trigger.uiDrawEnd` and `Trigger.postDraw`.

Currently, vanilla Mindustry only provides full-map rendering (`Vars.renderer.takeMapScreenshot()`), which renders the entire world grid into an enormous pixmap, or relies on OS shortcuts. Players have no in-game mechanism to capture their current screen view with or without UI, configure save folders, or customize file naming.

## Goals / Non-Goals

**Goals:**
- Provide in-game screen capture supporting both "With UI" and "Without UI" modes.
- Support customizable save locations, defaulting to `Vars.screenshotDirectory` with optional custom path configuration.
- Support customizable file naming formats (timestamp patterns) and an optional prompt to name files upon capture.
- Execute PNG encoding and disk writing asynchronously so gameplay remains smooth without stutter.
- Provide a clean Solim-based settings dialog and integrate with Quick Access for 1-click capture and long-click settings.
- Display a toast notification upon capture with a direct action to open the file/folder in the system file manager.

**Non-Goals:**
- Full-world map tile export (vanilla `takeMapScreenshot()` already covers this).
- In-game photo editing, cropping, markup, or filter tools.
- Direct social media or image-hosting uploads (kept within local file management).

## Decisions

### Decision 1: Capture Timing via `EventType.Trigger`
- **Choice**: Use Mindustry's render pipeline trigger events (`Trigger.uiDrawBegin` and `Trigger.postDraw`) to capture the framebuffer at exact points in the render sequence.
  - *Without UI*: Queue a one-shot action on `Trigger.uiDrawBegin`. The world is completely rendered, but no UI elements have drawn yet.
  - *With UI*: Queue a one-shot action on `Trigger.postDraw`. The full scene including HUD and overlays has completed rendering.
- **Alternatives Considered**:
  - *Toggling `Core.scene.root.setVisible(false)`*: Requires modifying scene visibility and waiting for an update tick, which can trigger layout recalculations, flicker, or corrupt modal state.
  - *Toggling `Vars.ui.hudfrag.shown`*: Only hides HUD fragments; does not hide mod overlays, chat, or open dialogs.

### Decision 2: Asynchronous PNG Encoding and Resource Cleanup
- **Choice**: Capture `Pixmap` synchronously on the render thread via `ScreenUtils.getFrameBufferPixmap(...)`, then immediately hand off the `Pixmap` to a background thread pool (`arc.util.async.Threads.daemon(...)`) for PNG compression and disk I/O via `PixmapIO.writePng(...)`. Call `pixmap.dispose()` on the background thread when complete.
- **Alternatives Considered**:
  - *Synchronous encoding*: Freezes the game for 50-200ms on desktop and up to several hundred milliseconds on mobile devices or 4K displays.

### Decision 3: File Naming and Collision Resolution
- **Choice**: Default to a timestamped format `screenshot-yyyy-MM-dd_HH-mm-ss.png`. If a collision occurs (e.g. multiple shots in the same second), append an incremental counter `_1`, `_2`. When the "Prompt for filename" option is enabled, display a lightweight text input dialog before capturing.
- **Alternatives Considered**:
  - *Overwriting existing files*: High risk of data loss.
  - *Random UUID names*: Unfriendly for human players sorting through saved files.

### Decision 4: Quick Access Integration and Solim UI
- **Choice**: Register `ScreenshotFeature` with `quickAccess(true)`. Clicking the quick access icon triggers an immediate capture using current settings. Long-clicking opens `ScreenshotSettingsDialog`. The settings dialog uses Solim declarative layouts (`column()`, `row()`, `text()`, `textField()`, `toggle()`) constrained to `maxWidth(500f)`.
- **Alternatives Considered**:
  - *Opening dialog on every click*: Slows down the user when trying to take quick action shots during intense gameplay.

## Risks / Trade-offs

- **[Risk] Rapid clicking causing memory pressure**: If a user clicks the screenshot button multiple times per second, multiple high-resolution uncompressed `Pixmap` objects could flood heap memory before background encoding finishes.
  - *Mitigation*: Introduce a capture debounce (e.g. 500ms cooldown) and track an active capture state.
- **[Risk] Invalid custom directory path**: A user may enter a non-existent or read-only directory path.
  - *Mitigation*: Check directory validity and write permissions. If custom directory creation fails, fall back to `Vars.screenshotDirectory` and notify the player with an error toast.
- **[Risk] Capturing without UI when not in game**: `Trigger.uiDrawBegin` outside of gameplay (e.g. in the main menu or custom game setup) would capture a blank background.
  - *Mitigation*: If `Vars.state == null || !Vars.state.isGame()`, automatically capture the full screen or notify the player that UI-less capture requires an active game.
