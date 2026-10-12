## 1. Remove Profiling & Tracing from Core Seams

- [x] 1.1 Remove `isTracing()` and `traceLeaf(...)` from `Component.element()` and `AttachmentBridge.java`
- [x] 1.2 Remove profiling hooks in `AttachmentBridge.Hook` and clean up `AttachmentStack` registration

## 2. Fail-Fast & Fail-Loud Error Handling

- [x] 2.1 Update `Component.dispose()` to rethrow exceptions immediately without catching and logging
- [x] 2.2 Update `SessionRoot.dispose()` to fail loud on errors without silent ignores
- [x] 2.3 Update `QueryView` to eliminate catch-and-swallow error handling during view construction

## 3. Widget Lazy build() Refactoring & Manual Ownership Removal

- [x] 3.1 Convert `Button`, `SolimSlider`, `SolimCollapser` to lazy `build()` execution
- [x] 3.2 Convert `Row`, `Column`, `Card`, `Grid`, `Tabs`, `SolimStack` to lazy `build()` execution
- [x] 3.3 Delete `ModifierOwnership.java` and remove manual `own(...)` calls from `ElementConfig`, `CellConfig`, and `TableConfig`
- [x] 3.4 Clean up redundant constructor overloads across all modified widgets

## 4. API Cleanup & Test Adjustments

- [x] 4.1 Remove `listenerCount()` and `observerCount()` from `Signal.java`
- [x] 4.2 Remove `SolimAssert.assertOwned()` from `SolimAssert.java`
- [x] 4.3 Update unit tests (`SignalTest`, `DisposalPrimitivesTest`, `SessionRootTest`, `ReflectAccessTest`) to test observable behavior
- [x] 4.4 Strip narrative changelog comments across modified files
- [x] 4.5 Run full test suite (`./gradlew test`) to verify all assertions pass
