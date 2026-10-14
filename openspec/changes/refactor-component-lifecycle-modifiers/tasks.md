## 1. Ownership Window Infrastructure

- [ ] 1.1 Define an ownership window as a span where a component is the active ambient owner: its `build()`, or a `children(Runnable)` block
- [ ] 1.2 Expose the window marker from `Component` so guards can read it without depending on `solim-runtime`
- [ ] 1.3 Set the marker in `element()` around `build()`, and in every `children(Runnable)` implementation — `Column`, `Row`, `Card`, `Button`, `Grid`, `Wrap`, `Scroll`, `SolimCollapser`, `SolimDialog`, `Hud` — clearing it in each `finally`
- [ ] 1.4 Add a test asserting the marker is set for the eager containers `Column`, `Row`, `Card`, `Grid`, `Wrap`, and `Spacer`, which have no `build()`

## 2. Props DSL

- [ ] 2.1 Add `Prop` in `solim-core/src/solim/modifier/Prop.java` with a `Kind` of `ELEMENT`, `TABLE`, `CELL`, a static value or `Readable` source, and a setter
- [ ] 2.2 Add `Props<T>` in `solim-core/src/solim/modifier/Props.java` holding the host and an ordered `List<Prop>`, exposing `children(Runnable)` and a terminal `apply()`
- [ ] 2.3 Make `Props` construction pure — no `element()` / `table()` call, no `Effect`, no ownership registration — so it is safe to invoke at any call site
- [ ] 2.4 Add `Configurable<SELF>` in `solim-core/src/solim/modifier/Configurable.java` supplying `props(Prop...)` for every component that currently implements `CellConfig` / `ElementConfig` / `TableConfig`
- [ ] 2.5 Implement `applyAll()` so static properties apply directly and `Readable` properties install through `Effect.of(...)` with the host as the active ambient owner
- [ ] 2.6 Route `Kind.CELL` properties into the host's `PendingCellConfig` rather than a live cell, relying on the existing ordering where `AttachmentStack.pop()` runs the configurator while the container is still the active ambient owner
- [ ] 2.7 Add `raw(...)` accepting an arbitrary mutation against a chosen `Kind` as the escape hatch for non-setter modifiers
- [ ] 2.8 Add a terminal `apply()` and auto-apply any unapplied `Props` at `element()` for components with no `children(Runnable)` (`Text`, `Spacer`, `Divider`, bare `image()`)
- [ ] 2.9 Port `ElementConfig` properties to `Kind.ELEMENT` props: `width`, `height`, `size`, `x`, `y`, `position`, `visible`, `opacity`, `alpha`, `name`, `onClick`, `stopClickPropagation`, `tooltip`, `draggable`, `update`, `origin`, `rotation`
- [ ] 2.10 Port `TableConfig` properties to `Kind.TABLE` props: `align`, `top`, `bottom`, `left`, `right`, `center`, `padding` and its directional variants, `gap`, `rounded`, `border`, `background`, `backgroundColor`
- [ ] 2.11 Port `CellConfig` properties to `Kind.CELL` props: `growX`, `growY`, `grow`, `margin` and its directional variants, `marginX`, `marginY`, `minWidth`, `maxWidth`, `minHeight`, `maxHeight`, `prefWidth`, `prefHeight`
- [ ] 2.12 Give bespoke props to the modifiers that are not a single setter: `Column.align()` (four coordinated actions), `Column.gap()` (field write plus whole-table `respace`), `Button.style(Cons<SolimButtonStyleBuilder>)` (conditionally allocates an effect)
- [ ] 2.13 Port the component-specific modifiers used by the mod through first-class props or `raw(...)`: `color`, `style`, `icon`, `wrap`, `checked`, `text`, `fontScale`, `enabled`, `disabled`, `ellipsis`, `image`
- [ ] 2.14 Make `Column`, `Row`, `Card`, `Grid`, `Wrap`, `Spacer` `Configurable` and apply the recorded props at the start of `children()`, before `AttachmentStack.push(...)`
- [ ] 2.15 Make `Button`, `Checkbox`, `SolimTextField`, `SolimSlider`, `SolimSelect`, `Switch`, `Scroll`, `SolimCollapser`, `Tabs` `Configurable`
- [ ] 2.16 Make `SolimDialog`, `Hud`, and `Popup` `Configurable`
- [ ] 2.17 Make `Text`, `SolimImage`, `NetworkImage`, `Badge`, `Divider`, `SolimVideo` `Configurable` on the auto-apply path
- [ ] 2.18 Update `solim/UI.java` factory methods to use the deferred path and stop calling `AttachmentStack.attachToParent(x.element())`
- [ ] 2.19 Verify each ported property against existing behaviour, including the `ElementConfig.width(Readable)` cross-write into `cellConfig().prefWidth`

## 3. Reconciler Attachment Routing

- [ ] 3.1 Change `ForEach.reconcile()`, `Dynamic.mount()`, `When`, `ReactiveGrid`, `QueryView`, and `Popup` to attach items through `AttachmentStack.attach(...)` instead of calling `container.add(el)` directly
- [ ] 3.2 Delete the six direct `PendingCellConfig.applyToCell(cell)` calls and their `comp.adopt(d)` repair loops, since `DEFAULT_CONFIGURATOR` now handles every cell uniformly
- [ ] 3.3 Delete the `itemBindings` / `currentBindings` disposal bookkeeping that the `applyToCell` repair loop required
- [ ] 3.4 Resolve the concurrent-adoption defect in `ForEach.reconcile()`, which currently registers each cell effect in both `itemBindings` and `comp`, by collapsing to single ownership
- [ ] 3.5 Verify `CollapseHelper` and the remaining direct callers still obtain their cell configuration through the attachment pipeline
- [ ] 3.6 Add a test asserting that a reactive cell constraint on a reconciled item is owned by exactly one component and disposed exactly once

## 4. Mod Migration & Mixin Deletion

- [ ] 4.1 Write a codemod that rewrites `expr.m1(a).m2(b).m3(c)` into `expr.props(m1(a), m2(b), m3(c))` and measure its coverage against the 3,031 in-chain sites
- [ ] 4.2 Migrate `features/chat` (715 sites), `features/browser` (583), `features/godmode` (385), `features/playerconnect` (353), `features/schematicgrid` (299)
- [ ] 4.3 Migrate `components` (235), `features/translation` (230), `features/autoplay` (203), `features/settings` (185), `features/teamresource` (151), `features/maprules` (144), `features/prettychat` (137), `features/armycontrol` (117), `features/callout` (115)
- [ ] 4.4 Migrate the remaining feature packages and `services` (65 sites)
- [ ] 4.5 Migrate the 12 mod test files using the DSL
- [ ] 4.6 Hand-review all 339 reactive call sites, verifying each binding is still owned by the same component and disposed at the same time
- [ ] 4.7 Hand-review the component-specific call sites routed through `raw(...)`
- [ ] 4.8 Delete `CellConfig`, `ElementConfig`, and `TableConfig` and let the compiler enumerate every remaining call site
- [ ] 4.9 Remove `implements` clauses for the deleted interfaces from every component
- [ ] 4.10 Delete the now-unused helper methods and `this instanceof` branches the mixins carried

## 5. Reactive Primitives Outside Components

- [ ] 5.1 Stop `Units.dvw()` and `Units.dvh()` from returning a `Computed`; re-express them as plain value helpers so no `Computed` is allocated at a call site
- [ ] 5.2 Migrate `Query.initEffect()` so the effect is created inside the owning component's window rather than in the `Query` constructor
- [ ] 5.3 Migrate the remaining `Effect` / `Computed` sites outside a window: `TwoWayBinding`, `ProgressBar`, `FeedbackBadge`, `VirtualList`, the `ReactiveGrid.resolvedColumnCount` field, and the `Signal.map()` / `Readable.map()` call sites in `Button`, `Badge.ofCount`, `Tabs`, `SolimVideo`, `Text`, `SolimImage`, `NetworkImage`
- [ ] 5.4 Move every non-reactive owned resource out of constructors into a window, including the child components that depend on construction-time ownership (`Divider.image`, `Badge.label`, `Tabs.headerBar`, `SolimDialog` content, `SolimVideo` controls, and the `TwoWayBinding`s in `Switch` / `Checkbox` / `SolimSlider` / `SolimSelect` / `SolimTextField`)

## 6. Ownership API Removal

- [ ] 6.1 Add the ownership-window guard to `Effect.of(...)` and the `Computed` constructor, throwing `IllegalStateException` outside a window — landed only once sections 2 through 5 leave no violation
- [ ] 6.2 Delete `Component.own()` outright, with no replacement method, no renamed alias, and no package-private escape hatch
- [ ] 6.3 Reimplement `Component.listen(...)` on `OwnershipContext.register(...)` instead of `own(...)`
- [ ] 6.4 Keep `adopt()` as the single internal adoption primitive on `DisposableOwner` and document it as framework-internal; it stays public because `Component`, `SessionRoot`, and `OwnershipScope` implement it across `solim-api` and `solim-runtime`
- [ ] 6.5 Enforce strict single ownership for all `Disposables`, preventing duplicate or concurrent adoption across owners
- [ ] 6.6 Remove `reparentTo()` from `Component.java` and `ElementResolver.java`

## 7. Verification & Testing

- [ ] 7.1 Add behavioural tests asserting that a `Props` applies all its properties to the Element, the Table, and the parent Cell
- [ ] 7.2 Add a test asserting that a reactive property's effect is disposed with the host component and not with the enclosing scope
- [ ] 7.3 Add a test asserting cell constraints recorded during `applyAll()` are honoured by the cell configurator at attachment
- [ ] 7.4 Add a test pinning the precedence rule when a chained modifier is written both before and after `.props(...)`
- [ ] 7.5 Add a visual regression check for vertical and horizontal `gap` with mixed child sizes, covering the deferred `respace()` timing
- [ ] 7.6 Add a guard test proving `Effect.of(...)` and `new Computed<>(...)` outside a window throw, and that the same calls inside `build()` and inside `children()` succeed
- [ ] 7.7 Add a regression test proving a component's window-created effects are disposed with that component and not with the ambient parent scope
- [ ] 7.8 Update tests that called `own(...)` or `reparentTo()` to rely on ambient registration inside a window
- [ ] 7.9 Extend `SolimEncapsulationGuardTest` to also reject `adopt(` and `reparentTo(` in mod sources, and assert that `own(` no longer exists on `Component`
- [ ] 7.10 Run test suite `./gradlew test` across all modules (`:solim-api`, `:solim-runtime`, `:solim-core`, `:solim`, `:mod`)
- [ ] 7.11 Verify no memory leaks, no leaked ownership-window frames, disposal order preservation, and clean single invocation of `build()` / `element()`
