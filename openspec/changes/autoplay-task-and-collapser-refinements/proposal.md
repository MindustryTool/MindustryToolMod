## Why

Units without healing weapons or repair field abilities were incorrectly attempting to repair damaged blocks and getting stuck because `unit.canBuild()` was treated as a repair capability. Additionally, the Autoplay settings view suffered from UI overlapping between the task skip reason and the toggle button, and expandable task settings left large blank spaces when hidden using `.visible(false)`.

## What Changes

- **RepairTask Preconditions**: Disallow units from attempting repairs if they lack heal weapons (`w.bullet.heals()`, `RepairBeamWeapon`) or repair field abilities (`RepairFieldAbility`). `canBuild()` is no longer treated as a healing capability.
- **RepairAI Movement and Aiming**: When a unit possesses `RepairFieldAbility` but lacks heal weapons, navigate within aura distance to heal passively while suppressing hostile weapon firing against allied blocks.
- **SolimCollapser Layout Component**: Introduce `SolimCollapser` wrapping Arc's `Collapser` in `solim-core` and expose it via `solim.UI`. It animates collapse/expansion (0.2s default) and collapses `prefHeight` and `minHeight` to 0, eliminating blank space leftover by `.visible(false)`.
- **Autoplay Settings View Card Presentation**: Wrap each task in a rounded `Card` with a subtle border and background (`WebStyles.Colors.SECTION_BORDER` and `WebStyles.Colors.SECTION_BG`).
- **Autoplay Settings View Layout Structure**: Place the task status / skip reason on a dedicated row beneath the task control buttons so long status strings never overlap or crowd the task toggle button.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `autonomous-gameplay-ai`: Update RepairTask to require heal weapons or repair fields and refine task card UI layout in settings.
- `solim-layout`: Add `SolimCollapser` container wrapping Arc `Collapser` with reactive expansion and 0-height collapse.

## Impact

- `solim-core`: Adds `solim.layout.SolimCollapser` and test `SolimCollapserTest`.
- `solim`: Adds `collapser(...)` factory methods in `solim.UI`.
- `mod`: Updates `RepairTask.java`, `AutoplaySettingsView.java`, and autoplay unit tests.
- No breaking changes to public APIs.
