# Contributing Guide

## Code Style

- **Indentation**: 4 spaces.
- **Braces**: End of line (`{` on the same line).
- **Imports**: No wildcard imports (`*`). Explicit imports only.
- **Naming**: `CamelCase` for classes, `camelCase` for methods/vars, `UPPER_CASE` for constants.
- **Package Structure**: `mindustrytool.features.[feature_name]`.

## New Features

1. **Create Package**: Create a new package in `src/mindustrytool/features/[feature-name]`.
2. **Implement Plugin**: Create a main class implementing `mindustrytool.Plugin`.
3. **Register**: Register your plugin in `src/mindustrytool/Main.java`.

## Modifying Existing Features

- Respect the `features` directory structure.
- **Decoupling**: DO NOT add hard cross-feature dependencies. Use the Event Bus (`mindustrytool.events`) for communication.
- **UI**: Ensure UI changes work on both Desktop and Mobile (check `Core.app.isMobile()`).

## Building

- Use `gradlew build` to compile the mod.
- The output jar will be located in `build/libs`.
- Run `gradlew deploy` (if configured) to copy the jar to your Mindustry mods folder.
