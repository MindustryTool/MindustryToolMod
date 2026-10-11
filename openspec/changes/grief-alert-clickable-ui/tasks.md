## 1. Feature State & Alert Model

- [ ] 1.1 Create `GriefAlertInfo` data model containing builder name, block name, coordinates, and timestamp
- [ ] 1.2 Add reactive `activeAlert` signal, `jumpToAlert()`, `dismissAlert()`, and auto-dismiss timer in `GriefAlertFeature`
- [ ] 1.3 Remove `Vars.ui.chatfrag.addMessage` and `Vars.ui.hudfrag.showToast` from `GriefAlertFeature.onBuildBegin()`

## 2. Interactive UI Component

- [ ] 2.1 Implement `GriefAlertHudView` Solim component top-centered with warning styling, click-to-jump, and dismiss button
- [ ] 2.2 Add HUD mounting and lifecycle cleanup in `GriefAlertFeature` (`onEnable`, `onDisable`, `WorldLoadEndEvent`)
- [ ] 2.3 Add necessary translation bundle strings for any UI actions/tooltips if applicable

## 3. World Highlight & Verification

- [ ] 3.1 Implement temporary pulsing target marker drawn at the target tile's world coordinates on `Trigger.draw`
- [ ] 3.2 Write/update unit tests for `GriefAlertFeature` verifying alert triggering, dismiss, jump behavior, and zero chat messages
- [ ] 3.3 Verify build passes with `./gradlew check` or test suite
