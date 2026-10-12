## 1. Lift the ambient stack

- [x] 1.1 Move `OwnershipContext` from `solim-runtime` to `solim-api` under `solim.core`
- [x] 1.2 Move `SolimAssert` alongside it; confirm no remaining dependency on `solim-runtime`
- [x] 1.3 Update all 166 `OwnershipContext` and 32 `SolimAssert` references; no behavioural change
- [x] 1.4 Verify `solim-runtime` still compiles without depending on `solim-core`
- [x] 1.5 Test: `solim-runtime` and `solim-core` suites green after the move

## 2. Component becomes the base class

- [x] 2.1 Move the parent link, child list, leaf list and `onDispose -> children -> leaves` disposal order from `BaseComponent` into `Component`
- [x] 2.2 Make `Component` abstract; delete the `default dispose()` and `default isDisposed()` no-ops
- [x] 2.3 Add `adopt(Disposable)` routing to children vs leaves, plus `owner()`, `children()`, `isAncestorOf(Component)`
- [x] 2.4 Add both constructors: `Component()` and `Component(Element prebuilt)`
- [x] 2.5 Make `element()` final; implement the lazy build path with the ownership push, the null check and `applyDefaultName`
- [x] 2.6 Reduce `BaseComponent` to a shim that delegates to `Component` and keeps `build()`
- [x] 2.7 Reduce `LeafComponent` to a shim over `Component`, keeping `E element()` until its four subclasses migrate
- [x] 2.8 Verify no behaviour change for components that already owned correctly

## 3. Closure-free ownership

- [x] 3.1 Change the ownership stack from `Deque<Cons<Disposable>>` to `Deque<Component>`
- [x] 3.2 Delete `BaseComponent.registrar` and the `this::own` method reference in `element()`
- [x] 3.3 Make `OwnershipScope` and `Fragment` adopt through the shared method instead of implementing `Cons`
- [x] 3.4 Confirm `arc.func` no longer appears in the ownership path
- [x] 3.5 Test: pushing an owner allocates no callback

## 4. Migrate eager components

One class per step; each independently reviewable and independently revertible.

- [x] 4.1 `Button` - `extends Component`, delete `bindings`/`constraints`/`disposed`, delete the `dispose()` override, delete the 7 `bindings.add` sites, add the `children()` owner push
- [x] 4.2 `Text` - same shape
- [x] 4.3 `Hud` - replace the `resizeListener` closure with an owned subscription
- [x] 4.4 `Tabs` - migrate; keep `tabButtons`/`tabContents` as component-specific state
- [x] 4.5 `SolimStack` - migrate; `ownedLayers` becomes ordinary owned children
- [x] 4.6 `SolimDialog` - migrate; decide the fate of the public `registerDisposable`
- [x] 4.7 `Checkbox`, `Switch`, `SolimSelect`, `SolimSlider`, `SolimTextField`, `Divider`, `Spacer`
- [x] 4.8 `Row`, `Column`, `Card`, `Wrap`, `Scroll`, `Grid`, `SolimCollapser` - migrate with the `children()` owner push
- [x] 4.9 `SolimImage`, `NetworkImage`, `Badge`, `SolimVideo` - migrate off `LeafComponent`, taking the concrete `E` return with them
- [x] 4.10 Verify no class still implements `Component` directly

## 5. Delete the manual ownership sites

- [x] 5.1 Delete the `bindings.add` sites in `RoundedDrawable` (5)
- [x] 5.2 Delete the `owned.add` sites in `SolimVideo` (8)
- [x] 5.3 Collapse `SolimDialog.registerDisposable` into inherited ownership
- [x] 5.4 Store `Badge`'s and `NetworkImage`'s `binding` fields through ownership instead
- [x] 5.5 Fix `SolimImage.color(Readable)` and `SolimImage.drawable(Readable)`, which today store nothing
- [x] 5.6 Delete `ModifierOwnership`'s `instanceof DisposableOwner` guard; it is always true now
- [x] 5.7 Confirm zero manual ownership sites remain outside the base class

## 6. Retire the alias

- [x] 6.1 Migrate the 91 `mod/` files from `extends BaseComponent` to `extends Component`
- [x] 6.2 Delete `BaseComponent`
- [x] 6.3 Confirm `mod/` compiles and its suite is green

## 7. Verify

- [x] 7.1 Test: the reported regression - a `reactiveGrid` factory returning a `Button` updates the icon colour when the signal changes
- [x] 7.2 Test: a `forEach` factory returning each component type keeps its reactive bindings alive
- [x] 7.3 Test: a component that forgets to implement disposal does not compile (documented, not asserted at runtime)
- [x] 7.4 Test: disposal order is `onDispose -> children -> leaves` for both eager and lazy components
- [x] 7.5 Test: `children()` output is owned by the component whose block created it
- [x] 7.6 Confirm `element.name` is now populated for every component and note the change for MCP tooling
- [x] 7.7 Run `:checkBundles` and the full test suite
- [x] 7.8 Measure construction allocation before and after; confirm one closure per component is gone

## 8. Hand back to the parent change

Deferred by decision: a session root retains every top-level component for the process lifetime,
and the blocker it was meant to clear no longer exists. See `implementation-notes.md`.

- [ ] 8.1 Revisit `solim-reactivity-hardening` task 7.3: install the session root now that nothing falls through for want of an owner _(deferred to a follow-up change)_
- [ ] 8.2 Revisit `solim-reactivity-hardening` task 7.9: assert every registered node resolves to an owner _(deferred to a follow-up change)_
- [x] 8.3 Re-check `audit.md` section 8 gaps 1-8, which the session root depends on
