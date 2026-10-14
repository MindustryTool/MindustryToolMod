## 1. Component Lifecycle & Ownership Cleanup

- [ ] 1.1 Remove `reparentTo()` from `Component.java` and `ElementResolver.java`
- [ ] 1.2 Delete `Component.own()` outright — no replacement method, no renamed alias, no package-private escape hatch — so manual self-registration is no longer expressible
- [ ] 1.3 Restrict `Effect` and `Computed` construction to `Component.build()`, enforced by a build-phase guard in `Effect.of(...)` and the `Computed` constructor that throws `IllegalStateException` when either is created outside an active `build()` frame
- [ ] 1.4 Expose the build-phase state from `Component` (a frame flag set around `build()` in `element()`) so the guard in 1.3 can distinguish `build()` from every other creation site without depending on `solim-runtime`
- [ ] 1.5 Move every `Effect`/`Computed` currently created outside `build()` into `build()`: `TwoWayBinding`, `ProgressBar`, `FeedbackBadge`, `Query.initEffect()`, `VirtualList`, the `ReactiveGrid.resolvedColumnCount` field, and the `Signal.map()` / `Readable.map()` / `Units.dvw()` / `Units.dvh()` call sites (`Button`, `Badge.ofCount`, `Tabs`, `SolimVideo`, `Text`, `SolimImage`, `NetworkImage`)
- [ ] 1.6 Move every non-reactive owned resource out of constructors into `build()`, including the child components that today depend on construction-time `own()` for their owner (`Divider.image`, `Badge.label`, `Tabs.headerBar`, `SolimDialog` content, `SolimVideo` controls, and the `TwoWayBinding`s in `Switch` / `Checkbox` / `SolimSlider` / `SolimSelect` / `SolimTextField`)
- [ ] 1.7 Delete the redundant `((Component) this).own(e)` calls in `CellConfig`, `ElementConfig`, and `TableConfig`; the owning component is the active ambient owner while those modifiers run
- [ ] 1.8 Reimplement `Component.listen(...)` on `OwnershipContext.register(...)` instead of `own(...)`
- [ ] 1.9 Keep `adopt()` as the single internal adoption primitive on `DisposableOwner` and document it as framework-internal; it stays public because `Component`, `SessionRoot`, and `OwnershipScope` implement it across `solim-api` and `solim-runtime`
- [ ] 1.10 Enforce strict single ownership for all `Disposables` (prevent duplicate or concurrent adoption across owners)
- [ ] 1.11 Verify `Effect.of()` auto-registration in ambient `OwnershipContext` and ensure no orphaned effects exist

## 2. Declarative DSL Lazy Realization

- [ ] 2.1 Remove manual `AttachmentStack.attachToParent(x.element())` calls from factory methods in `solim.UI.java`
- [ ] 2.2 Ensure container components (`Button`, `Column`, `Row`, `Card`, etc.) rely solely on `AttachmentStack.attachPendingComponents()` upon scope `pop()`
- [ ] 2.3 Verify top-level/root element resolution for views and dialogs

## 3. Verification & Testing

- [ ] 3.1 Update tests that called `own(...)` or `reparentTo()` to rely on ambient registration inside a `build()` scope
- [ ] 3.2 Extend `SolimEncapsulationGuardTest` to also reject `adopt(` and `reparentTo(` in mod sources, and assert that `own(` no longer exists on `Component`
- [ ] 3.3 Add a guard test proving `Effect.of(...)` and `new Computed<>(...)` outside `build()` throw, and that the same calls inside `build()` succeed
- [ ] 3.4 Add a regression test proving a component's `build()`-created effects are disposed with that component and not with the ambient parent scope
- [ ] 3.5 Run test suite `./gradlew test` across all modules (`:solim-api`, `:solim-runtime`, `:solim-core`, `:solim`, `:mod`)
- [ ] 3.6 Verify no memory leaks, no leaked ambient ownership frames after `build()`, disposal order preservation, and clean single invocation of `build()` / `element()`
- [ ] 3.7 Record the deferred modifier migration as a follow-up change: `CellConfig` / `ElementConfig` / `TableConfig` and `Button.enabled(...)` / `checked(...)` / `style(...)` still create `Effect.of(...)` inside modifier methods that are chained outside `build()`, and must be migrated before the 1.3 guard can cover them
