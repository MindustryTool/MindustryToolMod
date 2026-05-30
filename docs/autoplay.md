# Autoplay Audit

## Findings

### Security

- **MEDIUM** `AutoplayFeature.java:62-72` — `onDisable` sets `unit.controller(Vars.player)` — may override player's current control state unexpectedly
- **LOW** `AutoplayFeature.java:97-99` — Draws icon overlay on player unit position even if game is paused or in menu (though `isEnabled` is checked)

### Concurrency

- **MEDIUM** `AutoplayFeature.java:40-45` — `updateTask` and `updateUnit` both run on `Trigger.update` event; `currentTask` shared mutable state — if `updateTask` changes `currentTask` and `updateUnit` reads it in same frame, behavior is consistent but fragile
- **MEDIUM** `AutoplayFeature.java:82-95` — `currentTask != null && currentTask.getAI().unit() != unit` check may race if unit dies/changes between updateTask and draw frame

### Anti-Patterns

- **MEDIUM** `AutoplayFeature.java:50-57` — Task order persistence: saved order is deserialized, then remaining tasks from current list are appended; newly added tasks always sort last
- **MEDIUM** `AutoplaySettingDialog.java:89-98` — Status label rebuilds on every frame when dialog is open; `getStatus()` could be expensive for some tasks
- **LOW** `BaseAutoplayAI.java:44` — `super.updateUnit()` may call `updateTargeting()` which is overridden as no-op; the AI's default targeting behavior is skipped

### Dead Code / Tech Debt

- **MEDIUM** `AutoplayTask.java:15` — `isEnabled()` defaults to `true` via settings check; no way to distinguish "never set" from "explicitly set to true"
- **LOW** `AutoplayFeature.java:37` — `isFollowUnit` field shadows settings-based persistence; two sources of truth
- **MEDIUM** `AutoplayFeature.java:41` — `isFollowUnit` initialized from settings but also has a setter that writes to settings — race condition between read in constructor and write from settings dialog

### Performance

- **LOW** `AutoplayFeature.java:62` — Every frame: checks `isEnabled`, `Vars.state.isPlaying`, `Vars.player.unit()`, `Core.input.isTouched()`, iterates all tasks — could be optimized with early exits
- **LOW** `AutoplayFeature.java:40` — All tasks are checked every frame even if none are applicable; tasks could maintain their own activation state
