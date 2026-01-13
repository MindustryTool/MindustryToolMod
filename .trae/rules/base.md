# Project Structure

## Root folder: `src/mindustrytool`

-  `Config.java`: Static configuration class.
-  `Main.java`: Mod entry point.
-  `Utils.java`: Static utility methods.
-  `dto/`: Shared class resources.
-  `ui/`: Shared UI components and dialogs.
-  `events/`: Custom event definitions.
-  `services/`: Service layer for business logic, data fetching.
-  `features/`: Core functional modules.
   -  `auth/`: Authentication.
   -  `background/`: Custom background.
   -  `browser/`: Map and schematic browser.
      -  `map/`: Map browser.
      -  `schematic/`: Schematic browser.
   -  `playerconnect/`: Player connect.
   -  `settings/`: Settings.
   -  `display/`: Display.
      -  `hud/`: HUD.
      -  `healthbar/`: Health bar.
      -  `range/`: Anything that has range.
      -  `pathfinding/`: Enemy pathfinding
   -  `chat/`: Chat.

**Group files into folders based on their functionality whenever possible.**

## Code Style

-  Use standard Java naming conventions, only use meaningful names and avoid abbreviations.
-  No magic number or string, use constant instead unless its for UI.
-  Boolean variable should use `is` or `has` prefix.
-  Apply DRY, KISS, and SOLID principles.
-  Prefer immutability
-  No wildcard imports (`*`)
-  Avoid callback hell
-  Single Responsibility Principle
-  Prefer composition over inheritance
-  Always use dependency injection (constructor injection)
-  Clear exceptions & error handling
-  Prefer Optional over null where appropriate
-  Use java stream API, record and lambda expressions if applicable
-  Limit to 4 method parameters
-  Use/define components for UI elements whenever possible
-  Avoid large nesting (3 levels or more), eary return whenever possible
-  Avoid singleton

## Requirements

-  Java 17
-  Mindustry v8 (build 153)
