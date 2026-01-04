# Architecture Overview

## Project Structure

The project is organized by **Feature Vertical Slices**. All functional modules reside in `src/mindustrytool/features`.

### Directory Layout

- `src/mindustrytool/Main.java`: Mod entry point and plugin loader.
- `src/mindustrytool/Plugin.java`: Interface for all feature plugins.
- `src/mindustrytool/features/`: Core functional modules.
    - `auth/`: Authentication service and UI.
    - `browser/`: Map and schematic browser.
    - `autodrill/`: Automated drill placement logic.
    - `background/`: Custom menu background handling.
    - `controls/`: Touch control enhancements.
    - `multiplayer/`: Multiplayer connection tools.
    - `quickaccess/`: HUD quick access bar.
    - `visuals/`: Visual overlays (Unit ranges, paths, etc.)
    - `voicechat/`: Voice chat implementation (Proximity/Global).
- `src/mindustrytool/ui/`: Shared UI components and dialogs.
- `src/mindustrytool/events/`: Event bus definitions.
- `src/mindustrytool/utils/`: Utility classes.

## Key Design Patterns

### Feature Isolation
Each feature in `features/` should be self-contained. Communication between features should happen via the **Event Bus** (`mindustrytool.events`), not direct dependencies.

### Event Bus
We use Mindustry's `arc.Events` with custom event classes (e.g., `LoginStateChangeEvent`) to decouple components.
- **Publishers**: Fire events when state changes.
- **Subscribers**: Listen for events to update UI or logic.

### UI Composition
- **Dialogs**: Extend `BaseDialog` or `BaseBrowserDialog`.
- **Widgets**: Reusable components in `ui/components` or `ui/renderers`.
- **Reflection**: Used primarily for deep UI injections (e.g. `BrowserPlugin` injecting buttons into game menus) or performance optimizations.

## Data Flow
- **API Requests**: Handled by `ApiRequest` and service classes in `features`.
- **Caching**: Images and data are cached in `Core.settings` or local files (`imageDir`).
