## Why

Free Camera can be enabled without any on-screen indication beyond the QuickAccess icon state, so players pan away and lose track of whether the camera is detached from their unit. A visible status banner makes the mode discoverable and prevents confusion during scouting.

## What Changes

- Add a persistent, non-interactive "Free camera enabled" pill visible while `FreeCameraFeature` is enabled during gameplay.
- Anchor the pill top-center of the screen (`screenWidth / 2`, near top edge), following the `JoinApprovalHudView` pattern.
- Render the label in Mindustry native accent (`Pal.accent`) on a dark translucent background.
- Show/hide reactively with gameplay visibility (`hudfrag.shown`, `state.isGame()`); hide in menus, editor, and when the feature is disabled.
- Add a translatable bundle key for the label; no hardcoded user-visible text.
- Snapback reliability work is explicitly out of scope and tracked separately.

## Capabilities

### New Capabilities

- None — no standalone capability is introduced.

### Modified Capabilities

- `free-camera`: add an enabled-state status indicator requirement (persistent top-center pill, accent color, reactive visibility, i18n label).

## Impact

- Affects `FreeCameraFeature` (HUD mount/unmount lifecycle) and one new Solim HUD view; no changes to input handlers (`ModDesktopInput`, `ModMobileInput`), camera logic, QuickAccess behavior, or existing specs beyond the `free-camera` delta.
- Snapback behavior, keybinds, and settings are untouched.
