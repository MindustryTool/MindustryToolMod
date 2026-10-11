## 1. Shared Compact Content

- [ ] 1.1 Extract shared compact content component (toolbar + selection + groups) reused by popup and HUD shells
- [ ] 1.2 Build icon-only toolbar row (select-map, select-screen, deselect-all, add-group, close) with `unit(11)` / `unit(6)` sizes and existing bundle-key tooltips

## 2. Selection & Groups Density

- [ ] 2.1 Compact `ActiveSelectionBar` chips to unit icon + count with name-plus-hint tooltip, keeping tap-to-exclude
- [ ] 2.2 Compact `ControlGroupList` rows to unit icons + count recall button plus trash delete, removing names and preview duplication
- [ ] 2.3 Move add-group creation into toolbar `+` action with current-selection-else-screen-else-map fallback

## 3. Chrome Removal & Wiring

- [ ] 3.1 Remove title header, section labels, and empty placeholder rows; collapse empty selection/groups sections
- [ ] 3.2 Rewire `ArmyControlPopup` and `ArmyControlHudView` shells onto shared content and narrow container toward `unit(50-55)`
- [ ] 3.3 Update affected Solim component tests for icon-count chips, toolbar actions, and collapsed empty states
- [ ] 3.4 Verify `./gradlew :mod:test` and bundle check pass with no new user-visible strings
