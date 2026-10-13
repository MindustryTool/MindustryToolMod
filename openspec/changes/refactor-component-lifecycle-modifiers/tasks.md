## 1. Component Lifecycle & Ownership Cleanup

- [ ] 1.1 Remove `reparentTo()` from `Component.java` and `ElementResolver.java`
- [ ] 1.2 Make `adopt()` and `own()` in `Component.java` and `DisposableOwner.java` package-private (internal to `solim.core`)
- [ ] 1.3 Enforce strict single ownership for all `Disposable`s (prevent duplicate or concurrent adoption across owners)
- [ ] 1.4 Verify `Effect.of()` auto-registration in ambient `OwnershipContext` and ensure no orphaned effects exist

## 2. Modifiers Cleanup & Deferred Configurations

- [ ] 2.1 Remove `Pending` prefix (rename/consolidate `PendingCellConfig` into clean `CellConfig` configuration model)
- [ ] 2.2 Introduce deferred configuration models for `ElementConfig` and `TableConfig`
- [ ] 2.3 Remove all eager `element()`, `table()`, and `((Component) this).own(e)` calls from modifier methods, purely recording configuration state instead
- [ ] 2.4 Apply element/table configs and install their reactive effects during component realization when `OwnershipContext.push(this)` has the component active as the single owner
- [ ] 2.5 Verify cell configurator correctly applies all cell properties when elements mount into parent cells

## 3. Declarative DSL Lazy Realization

- [ ] 3.1 Remove manual `AttachmentStack.attachToParent(x.element())` calls from factory methods in `solim.UI.java`
- [ ] 3.2 Ensure container components (`Button`, `Column`, `Row`, `Card`, etc.) rely solely on `AttachmentStack.attachPendingComponents()` upon scope `pop()`
- [ ] 3.3 Verify top-level/root element resolution for views and dialogs

## 4. Verification & Testing

- [ ] 4.1 Update tests that directly invoked `leaf.own()` or `reparentTo()` to use internal or ambient registration
- [ ] 4.2 Run test suite `./gradlew test` across all modules (`:solim-api`, `:solim-runtime`, `:solim-core`, `:solim`, `:mod`)
- [ ] 4.3 Verify no memory leaks, disposal order preservation, and clean single invocation of `build()` / `element()`
