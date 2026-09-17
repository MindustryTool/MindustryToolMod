## Why

Players frequently want to capture high-quality screenshots of their bases, defense layouts, schematics, or gameplay moments without HUD elements, buttons, and mod overlays obscuring the view. Conversely, players creating tutorials or reporting bugs need complete screenshots that include all UI elements. Currently, Mindustry offers only a full-map tile capture or OS-level captures without in-game control over UI visibility, custom save destinations, or structured file naming. Adding a dedicated screenshot feature provides flexible in-game screen capture with toggleable UI presence, customizable save paths, and configurable file naming.

## What Changes

- Introduce a new `ScreenshotFeature` in `mindustrytool.features.screenshot`, registered into `FeatureManager` and `Main.java`.
- Support two capture modes:
  - **With UI**: Captures the complete rendered viewport including all HUD widgets, menus, and mod overlays.
  - **Without UI**: Captures a clean frame rendered before the UI stage (`Trigger.uiDrawBegin`), omitting HUD elements, overlays, and dialogs.
- Allow customizable file destinations:
  - Default to standard Mindustry screenshots directory (`Vars.screenshotDirectory`).
  - Allow selecting or entering a custom folder location.
- Allow customizable file naming:
  - Configurable filename prefix and timestamp template (e.g. `screenshot-{timestamp}.png`).
  - Optional setting to prompt the user for a custom filename before saving.
- Integrate with Quick Access bar and provide a dedicated Solim settings dialog to configure capture mode, folder location, naming format, and notification options.
- Provide immediate visual and toast feedback upon capture, displaying the saved file path with a button to open the containing directory.

## Capabilities

### New Capabilities
- `screenshot`: Screen capture functionality enabling in-game captures with or without UI, custom file naming and formatting, configurable output directories, and Quick Access integration.

### Modified Capabilities
*(None)*

## Impact

- **Feature Registry**: Adds `ScreenshotFeature` to `Main.java` and `FeatureManager`.
- **UI & Solim Components**: Creates `ScreenshotSettingsDialog` and `ScreenshotSettingsView` following Solim conventions.
- **Storage & I/O**: Interacts with `Vars.screenshotDirectory`, `arc.files.Fi`, and `arc.graphics.PixmapIO` on the render/background threads.
- **Internationalization**: Adds new user-facing translation keys to `assets/bundles/bundle.properties`.
