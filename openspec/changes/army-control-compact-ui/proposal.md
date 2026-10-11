## Why

Army Control popup and HUD are too packed for in-game use: three full-text action buttons (`Select Map / Select Screen / Deselect All`) overlap inside ~280px width, and stacked labels, full-name chips, and full-width group rows consume ~7 rows of height players cannot spare during RTS micro.

## What Changes

- Replace text action buttons with a single icon-only toolbar row: select-map, select-screen, deselect-all, add-group (`+`), and close (`x`), each `unit(11)` with tooltip.
- Compact Active Selection chips to unit icon + count only (name moves to tooltip); tap still excludes that unit type.
- Compact saved control groups to unit icons + count + trash button (drop `Group N (total)` names and per-type preview duplication); tap still recalls, trash still deletes.
- Strip non-interactive chrome: remove `Army Control` title header and `Active Selection` / `Control Groups` section labels.
- Collapse empty selection/groups sections entirely (no `No units selected` / `No groups saved` placeholder rows).
- Shrink popup/HUD container width accordingly (from `unit(70)` / `unit(65)` toward `unit(50-55)`) and share one compact content component between popup and HUD modes.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `army-control-group`: display requirements change — icon-only actions, icon+count chips and groups, no title/section labels, collapsed empty states, narrower container. Selection, recall, prune, and command-mode behavior unchanged.

## Impact

- Affected code: `mindustrytool.features.armycontrol.ArmyControlPopup`, `ArmyControlHudView`, `ActiveSelectionBar`, `ControlGroupList` (possible merge into one shared compact component).
- Localization: reuse existing `feature.army-control.action.*` and `feature.army-control.hint.deselect-chip` keys as tooltips; no new user-visible strings expected.
- Settings: display-mode (popup/HUD), scale, position configs unchanged.
