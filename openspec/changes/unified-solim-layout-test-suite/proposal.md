## Why

Solim provides rich alignment (`top`, `bottom`, `left`, `right`, `center`) and spacing (`gap`, `margin`, `padding`) modifier APIs across all its layout containers, reactive wrappers, interactive controls, and display primitives. However, there is no unified test suite verifying that all alignment directions and their bitmask compositions (`top().left()`, `bottom().right()`, `top().center()`, `center().top()`, etc.) operate consistently, nor that `gap`, `margin`, and `padding` coexist cleanly without stomping each other across both static and reactive signal updates. Adding this unified suite ensures strict layout guarantees, prevents layout regressions, and validates exact packing geometry.

## What Changes

- Add a unified component alignment test suite covering all Solim components across containers, reactive containers, interactive controls, and display primitives.
- Verify single and composite directional alignment bitmasks (`top()`, `bottom()`, `left()`, `right()`, `center()`, `top().center()`, `bottom().right()`, etc.) and ensure alignment replacement/override works correctly.
- Add a unified spacing composition test suite verifying the distinct responsibilities and combinations of `gap()` (sibling spacing), `margin()` (parent cell outer padding), and `padding()` (internal element insets).
- Add layout geometry and packing tests asserting that calculated child coordinates `(x, y, width, height)` match expected positions under all alignment and spacing combinations.
- Add reactive spacing and alignment tests verifying dynamic updates via `Signal<Float>` and `SignalDispatcher.flush()`, as well as disposal cleanup.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `solim-layout`: Add requirements specifying unified alignment composition bitmasks, gap/margin/padding spacing coexistence, and reactive geometry verification across all Solim components.

## Impact

- Test coverage across `solim-core` (`solim.layout`, `solim.reactive`, `solim.input`, `solim.display`).
- Zero breaking changes to production code or existing APIs.
- Tests run under `solim.test.SolimEnv` in `solim-core/src/test/java/solim/layout/`.
