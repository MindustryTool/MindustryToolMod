# Project Structure  

## Root folder: `src/mindustrytool`

- `Config.java`: Static configuration class.
- `Main.java`: Mod entry point.
- `Utils.java`: Static utility methods.
- `dto/`: Shared class resources.
- `ui/`: Shared UI components and dialogs.
- `events/`: Custom event definitions.
- `features/`: Core functional modules.
    + `auth/`: Authentication.
    + `background/`: Custom background.
    + `browser/`: Map and schematic browser.
    + `player-connect/`: Player connect.
    + `settings/`: Settings.
    + `display/`: Display.
        + `hud/`: HUD.
        + `healthbar/`: Health bar.
        + `range/`: Anything that has range.
        + `pathfinding/`: Enemy pathfinding

## Code Style

- Use standard Java naming conventions.
- Prefer immutability
- No wildcard imports (`*`)
- Single Responsibility Principle
- Prefer composition over inheritance
- Clear exceptions & error handling
- Prefer Optional over null where appropriate
- Use java stream API, record and lambda expressions if applicable
- Limit to 4 method parameters

## Requirements

- Java 17
- Mindustry v8 (build 153)
