## 1. Follow target resolution

- [x] 1.1 Split `FollowAssistTask.update()` into Any vs named paths and drop the any-idle-teammate fallback in Any mode
- [x] 1.2 Add active-builder predicate (`activelyBuilding()` OR queued plans) with alive, same-team, and not-self guards
- [x] 1.3 Clear `ai.following` and yield (`return false`) when no active builder exists; remove stale TODO

## 2. Status and localization

- [x] 2.1 Add `feature.autoplay.status.no-active-builders` with descriptive comment to `assets/bundles/bundle.properties`
- [x] 2.2 Wire new status into Any-mode and named-missing yield paths via `Core.bundle.get`

## 3. Verification

- [x] 3.1 Verify Any-mode idle teammates yield to lower tasks, named idle still follows, and queued-plans counts as active
- [x] 3.2 Run AGENTS.md checks: no hardcoded user text, Java 8 APIs only, `arc.util.Nullable`, no FQCNs, ternary for simple conditions
