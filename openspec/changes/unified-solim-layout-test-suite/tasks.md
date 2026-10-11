## 1. Test Harness & Alignment Matrix Tests

- [x] 1.1 Implement component supplier fixtures and helper assertions in `solim-core/src/test/java/solim/layout/`
- [x] 1.2 Implement `UnifiedComponentAlignmentTest` testing individual alignment bitmasks (`top`, `bottom`, `left`, `right`, `center`) across all components
- [x] 1.3 Test alignment bitmask compositions (`top().left()`, `top().right()`, `bottom().left()`, `bottom().right()`, `top().center()`, `bottom().center()`, `left().center()`, `right().center()`) and order independence
- [x] 1.4 Test directional replacement/override semantics (e.g. `top()` then `bottom()` clears top and sets bottom) and child defaults propagation

## 2. Spacing Interplay & Composition Tests

- [x] 2.1 Implement `UnifiedSpacingCompositionTest` testing `padding()` inner container insets on `TableConfig` components
- [x] 2.2 Test `margin()` outer parent cell padding on `CellConfig` components and verify it does not bleed into inner container padding
- [x] 2.3 Test `gap()` calculation along primary container axes, verifying leading visible element gets zero gap and trailing elements receive `gap + margin`
- [x] 2.4 Test dynamic visibility toggling (`visible = false`) shifting gap offsets to subsequent visible siblings upon `respace()`

## 3. Layout Geometry & Packing Tests

- [x] 3.1 Implement `UnifiedLayoutGeometryTest` with mock Scene environment
- [x] 3.2 Verify computed coordinates `(x, y)` for centered components inside fixed-size parent tables
- [x] 3.3 Verify computed coordinates for all 4 corners (`top-left`, `top-right`, `bottom-left`, `bottom-right`) and edge-centered compositions
- [x] 3.4 Verify geometry under combined insets (`padding` + `margin` + `gap`)

## 4. Reactive Spacing & Alignment Tests

- [x] 4.1 Implement `ReactiveSpacingAlignmentTest` testing reactive `margin` and `padding` signals with `SignalDispatcher.flush()`
- [x] 4.2 Test reactive gap updates and verify parent cell re-spacing
- [x] 4.3 Verify lifecycle disposal: assert that disposing the component unbinds all effects and cleans up reactive subscriptions

## 5. Verification & Test Execution

- [x] 5.1 Run all newly created test suites via Gradle (`./gradlew :solim-core:test`) and ensure 100% pass rate
- [x] 5.2 Verify absence of regressions across existing Solim tests
