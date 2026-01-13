The task was to fix `HealthBarVisualizer.java` to implement the `Feature` interface. This has been completed by refactoring the class and registering it in the main application logic.

### 1. Refactor `HealthBarVisualizer.java`
- Implemented `Feature` interface.
- Added `getMetadata()` returning "Health Bar" feature metadata.
- Implemented `init()` to load configuration and register the drawing loop via `Events.run(Trigger.draw, ...)`.
- Implemented `onEnable()` and `onDisable()` to toggle a local `enabled` flag, ensuring the draw logic only runs when enabled.
- Implemented `setting()` to return the settings dialog, refactoring the existing `showSettings()` logic into `initDialog()` and `rebuild()`.
- Used `HealthBarConfig` for configuration management.

### 2. Register Feature in `Main.java`
- Imported `HealthBarVisualizer`.
- Instantiated `HealthBarVisualizer` in `initFeatures()`.
- Registered the new feature instance with `FeatureManager`.

The Health Bar feature is now fully integrated into the feature management system, allowing it to be enabled/disabled and configured via the settings menu.