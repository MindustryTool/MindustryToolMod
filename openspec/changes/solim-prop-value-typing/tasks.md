## 1. Root capability marker

- [ ] 1.1 Add `solim.modifier.HostCapability` (empty marker interface)
- [ ] 1.2 Make every Solim host implement `HostCapability` (the `Component` base in `solim-api` plus the container/leaf/overlay hosts that do not already reach it). No behaviour change

## 2. Prop becomes generic

- [ ] 2.1 Add the `Requires` and `V` type parameters to `Prop` and replace `Applier(Object, Object)` with a value-typed slot
- [ ] 2.2 Add the typed host view (`withElement`, `withTable`, `withCell`) and dispatch the target through it, removing the `(Element)`/`(Table)`/`(PendingCellConfig)` casts in the `element`/`table`/`cell`/`cellSource` factories
- [ ] 2.3 Add the valueless factory returning `Prop<Requires, Void>` and delete the `valueLess` field and its branches
- [ ] 2.4 Keep `label` as a diagnostic only; delete `Props.applyAlignCellRecord` and the `"align".equals(label)` ladder
- [ ] 2.5 Retype `Props<T>` to hold `List<Prop<? super T, ?>>` and call the typed slot on apply

## 3. Values factories retyped

- [ ] 3.1 Retype the numeric slot/value factories (`width`, `height`, `size`, `prefWidth`, `prefHeight`, `minWidth`, `minHeight`, `maxWidth`, `maxHeight`, `gap`, `padding*`, `margin*`, `fontScale`, `opacity`, `alpha`, `progress`, …) and delete `num(...)`
- [ ] 3.2 Retype the reactive factories and delete the `(Readable<Float>)`, `(Readable<Boolean>)`, `(Readable<Color>)`, `(Readable<?>)` narrowing casts
- [ ] 3.3 Retype the colour factories (`color`, `backgroundColor`, `border`, `rounded`) and delete the `(Color)` casts
- [ ] 3.4 Retype the align factories (`align`, `top`, `bottom`, `left`, `right`, `center`) to record their typed cell value directly
- [ ] 3.5 Retype `raw(...)` to `Prop<HostCapability, Void>` with the explicit value-typed mutation form (capability restriction lands in the next change)

## 4. Configurable signature

- [ ] 4.1 Change `Configurable.props` to `props(Prop<? super SELF, ?>... props)` and apply the single documented `@SuppressWarnings("unchecked")` naming the Java 8 varargs/`@SafeVarargs` rule as the reason
- [ ] 4.2 Confirm `applyCellRecords`, `applyAll`, and the window handling are unchanged in behaviour (cell props still record before attachment; reactive props still install an `Effect` inside the host window)

## 5. Tests

- [ ] 5.1 Rewrite `solim/core/PropsDslTest` to assert that a typed factory applies the value it declares, that a valueless property still applies, and that the label no longer drives behaviour
- [ ] 5.2 Add a test proving a reactive prop still updates the bound target and is disposed with the host
- [ ] 5.3 Add a test proving cell props are observable through `cellConfig()` immediately after `props(...)` on a not-yet-built host
- [ ] 5.4 Run the full `solim-core` suite and fix any test asserting the erased `Prop` shape

## 6. Documentation

- [ ] 6.1 Document `Prop<Requires, V>` and `HostCapability` where the prop types are declared
- [ ] 6.2 Update `docs/solim_internals.md`'s props section to describe the typed slot and the removal of the label dispatch
