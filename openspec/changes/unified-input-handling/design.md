## Context

Multiple features in MindustryTool require non-vanilla input and camera handling:
1. `FreeCameraFeature`: Decouples camera panning from player unit WASD movement on Desktop, and prevents automated camera tracking on Mobile.
2. `JoystickFeature`: Injects virtual joystick vectors into movement on Desktop and drives unit movement towards joystick direction on Mobile.

Previously, each feature attempted to dynamically swap `Vars.control.input` with custom classes (`JoystickDesktopInput` and `JoystickMobileInput`) when enabled, and restore the captured original input when disabled. This created tight coupling between `FreeCameraFeature` and `JoystickFeature`, brittle state tracking across world loads, and input interruptions (dropped touches/keys) during gameplay.

## Goals / Non-Goals

**Goals:**
- Provide a unified, persistent input layer that is installed once on `ClientLoadEvent` and verified on `WorldLoadEvent`.
- Eliminate all dynamic `Vars.control.setInput()` swaps and restore routines from individual features.
- Ensure 100% faithful vanilla fallback when features are disabled (e.g. mobile unit follows camera; desktop WASD resets pan).
- Completely decouple `FreeCameraFeature` from `JoystickFeature`.

**Non-Goals:**
- Replacing Mindustry's input bindings system (`Core.keybinds`).
- Introducing gamepads or external hardware controller layers beyond what Mindustry natively supports.

## Decisions

### Decision 1: Dedicated Package and Unified Class Names
- **Choice**: Place classes in `mindustrytool.input` named `ModDesktopInput` and `ModMobileInput`.
- **Rationale**: Keeps input handling clean, modular, and feature-agnostic. Features do not own the input system; rather, the input system queries features.
- **Alternatives Considered**: Keeping them in `mindustrytool.features.joystick` (misleading and keeps joystick-camera coupling).

### Decision 2: Lifecycle Management via `ModInputManager`
- **Choice**: Register `ModInputManager` that hooks `ClientLoadEvent` and `WorldLoadEvent`.
- **Rationale**: Wrapping on `ClientLoadEvent` ensures the mod input is ready before gameplay starts. Re-checking on `WorldLoadEvent` guarantees that if the player switched between Mouse and Touch in vanilla settings (which calls `Vars.control.setInput(...)`), the custom input is re-wrapped cleanly without per-frame polling.
- **Alternatives Considered**: Hooking per-frame in `Trigger.update` (unnecessary overhead); hooking only once at startup (leaves game unhooked if user switches Touch/Mouse in vanilla settings).

### Decision 3: Direct Loose Feature Queries via `FeatureManager`
- **Choice**: `ModDesktopInput` and `ModMobileInput` query features directly using `FeatureManager.getFeature(...)` or static accessors (`FreeCameraFeature.isFreeCam()`, `JoystickFeature.get()`).
- **Rationale**: Features are lightweight singletons managed by `FeatureManager`. Loose queries avoid building an over-engineered input pipeline/modifier architecture.
- **Alternatives Considered**: Event bus / input pipeline with modifier registries (unneeded complexity for 2 features).

### Decision 4: Faithful Vanilla Fallback on Inactivity
- **Choice**:
  - In `ModDesktopInput`: if `!FreeCameraFeature.isFreeCam()`, WASD resets pan and centers on player (vanilla). If joystick vector is zero, standard keyboard/mouse movement executes.
  - In `ModMobileInput`: if `JoystickFeature` is disabled, `targetPos` targets `Core.camera.position` (vanilla). If `FreeCameraFeature` is disabled, camera lerps to player unit.
- **Rationale**: Ensures that disabling features yields identical behavior to vanilla Mindustry.

## Risks / Trade-offs

- **[Vanilla Settings Changes]** If the player switches Touch/Desktop mode in vanilla settings while in-game, `control.input` resets to vanilla.
  → *Mitigation*: `ModInputManager.ensureCustomInput()` is triggered on `WorldLoadEvent` and can be invoked whenever settings change or game enters world.
- **[Inter-mod input conflicts]** Another mod might also replace `control.input`.
  → *Mitigation*: By subclassing `DesktopInput` and `MobileInput`, we remain standard input handlers. Checking `!(Vars.control.input instanceof ModDesktopInput)` avoids wrapping our own instances repeatedly.
