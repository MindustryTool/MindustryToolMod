## Why

In `Any Active Builder` mode, `FollowAssistTask` follows any alive teammate even when nobody is building, gluing the player unit to idle players and starving lower-priority productive tasks (`SelfBuild`, `Rebuild`, `Mining`). This resolves the `TODO: Skip if there r no active builder` in `FollowAssistTask.java`.

## What Changes

- Remove the idle-teammate fallback in `Any` (empty target) mode: when no active builder exists, `FollowAssistTask.update()` yields (`return false`) instead of claiming an idle target.
- Broaden `active builder` to `activelyBuilding() OR has queued build plans` to cover pauses between placements.
- Keep named-player loyalty: when a specific target player is configured and alive, still follow even when idle; when the named player is missing, fall back to active-builder search then yield.
- Add distinct status `No active builders` for the new yield path so HUD feedback is not misleading.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `autonomous-gameplay-ai`: Task 5 Follow & Assist trigger condition in `Any` mode changes from "any alive teammate" to "active builder only", and yields otherwise.

## Impact

- Affected code: `mod/src/mindustrytool/features/autoplay/tasks/FollowAssistTask.java`, `assets/bundles/bundle.properties` (new status key).
- Behavior: `AutoplayFeature` priority arbitration unchanged, but `FollowAssist` no longer blocks `SelfBuild`/`Rebuild`/`Mining` when nobody builds.
- No API or config format changes; existing `follow-assist.target-player` values keep working.
