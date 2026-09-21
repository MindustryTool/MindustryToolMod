## 1. Core infrastructure

- [ ] 1.1 Add `SolimToken.isExpandingChild(Element)` combining the token flag with the existing heuristics (spacer names, `fillParent`, `ScrollPane`, first-child `ScrollPane`); keep `SolimToken.isExpanding(Element)` token-only
- [ ] 1.2 Add `Units.unit(float)` and `Units.unit(int)` to `solim-core/src/solim/core/Units.java`
- [ ] 1.3 Update the 13 layout call sites (`Card`, `Column`, `Grid`, `Row`, `Scroll`, `SolimCollapser`, `Wrap`, `Dynamic`, `ForEach`, `QueryView`) from `Ui.isExpanding` to `SolimToken.isExpandingChild`
- [ ] 1.4 Update `Tabs` to use `Units.unit(14)` instead of `Ui.unit(14)`

## 2. Button fluent content

- [ ] 2.1 Add `Button.text(String)`, `Button.text(Readable<String>)`, `Button.icon(Drawable)`, `Button.icon(Readable<Drawable>)` in `solim-core/src/solim/input/Button.java`, applying standard icon treatment (scalable, `Scaling.fit`, `Units.unit(6)`)
- [ ] 2.2 Verify `Button.enabled(Readable<Boolean>)` remains null-tolerant (no change expected)

## 3. Widget cleanups (solim-core)

- [ ] 3.1 Collapse `Text.of(...)` to `of(String)` + `of(Readable<String>)`; keep `Text.text(String)` + `text(Readable<String>)`
- [ ] 3.2 Remove `Card.of(Runnable)`
- [ ] 3.3 Remove `Hud.backgroundDrawable(...)`
- [ ] 3.4 Remove `SolimDialog.backButton()` and `SolimDialog.content(Runnable)` (keep `children`)
- [ ] 3.5 Remove `Button.sizedButton()`

## 4. Public facade (`solim/src/solim/UI.java`)

- [ ] 4.1 Collapse `text(...)` to `text(String)` + `text(Readable<String>)`; remove `Signal`/`Computed` overloads
- [ ] 4.2 Remove `button(Drawable)`, `button(Drawable, Runnable)`, `button(String, Drawable, Runnable)`, and both `button(..., Consumer<SolimButtonStyleBuilder>)` variants; keep `button()`, `button(Runnable)`, `button(String, Runnable)`, `button(Readable<String>, Runnable)`
- [ ] 4.3 Remove the two four-argument `grid(...)` overloads; make `reactiveGrid(items)` accept only the fundamental data and configure the rest fluently (section 5)
- [ ] 4.4 Add `when(Readable<Boolean>, Supplier<Component>)` delegating to `Dynamic`
- [ ] 4.5 Change `spacer()` to return `Spacer`
- [ ] 4.6 Remove `badgeCount(...)`, `divider(String)`, `divider(char)`, `container()`, `container(Runnable)`, no-arg `icon()`, and both `buttonStyle()` methods
- [ ] 4.7 Delegate `UI.unit(...)` to `Units.unit(...)` and remove the local `BASE_UNIT` duplicate

## 5. Fluent keyed collections (D10)

- [ ] 5.1 Migrate `ReactiveGrid` to `<T>`-only generics with internal `StructuralReconciler<Object, Component>`; replace the 4-arg constructor with `ReactiveGrid(Readable<? extends Iterable<T>> items)` + fluent `.columns(int)`, `.columns(Readable<Integer>)`, `.key(Function<T, ?>)` (identity default), keeping `.gap(...)`, `.empty(...)`, `.emptyView(...)`; add terminal `void children(Function<T, Component>)` and `void children(BiFunction<T, GridItemContext, Component>)`; throw `IllegalStateException` naming `.children(...)` if built without a factory
- [ ] 5.2 Migrate `ForEach` to `<T>`-only generics with `ForEach(Readable<? extends Iterable<T>> items)` + fluent `.key(...)` (identity default) and terminal `void children(Function<T, Component>)`
- [ ] 5.3 Migrate `VirtualList` to `<T>`-only generics with `VirtualList(Readable<? extends List<T>> items, ItemHeightProvider<T> heightProvider)` + fluent `.key(...)` (identity default), keeping `.overscan(...)`, `.gap(...)`, `.onReachTop(...)`, `.onReachBottom(...)`; add terminal `void children(Function<T, Component>)`
- [ ] 5.4 Accept static `Iterable<T>`/`List<T>` directly on the facade factories, wrapping in `Readable.of(...)` internally
- [ ] 5.5 Update `UI.reactiveGrid(...)`/`UI.forEach(...)`/`UI.virtualList(...)` factories to the fluent signatures and attach via the existing pending-attach path

## 6. Delete legacy facade

- [ ] 6.1 Delete `solim-core/src/solim/core/Ui.java`
- [ ] 6.2 Migrate solim-core tests importing `solim.core.Ui` (~10 files) to `solim.UI` or direct component constructors (`new Column()`, `new Divider(Direction.X)`, `new Spacer()` + `ParentStack.attachToParent`)
- [ ] 6.3 Migrate `Button`/`Tabs` tests off `sizedButton()` and removed facade methods

## 7. Mod migration

- [ ] 7.1 Add `WebStyles.iconButton(Drawable icon, Runnable onClick)` and `WebStyles.iconButton(Drawable icon, String tooltip, Runnable onClick)` using `button(onClick).style(GHOST_STYLE).size(unit(11)).icon(icon)`
- [ ] 7.2 Migrate exact-shape ghost icon-button call sites (~50) to `WebStyles.iconButton(...)`; leave scaled/colored/checked variants fluent
- [ ] 7.3 Migrate keyed-collection call sites to the fluent form: `SchematicBrowserDialog`, `MapBrowserDialog`, `MusicSettingsView`, `AutoplaySettingsView`, `QuickAccessSettingsView`, `QuickAccessHudView`, `QuickSchematicGridHudView` (3 sites), `SchematicPickerDialog`, `QuickSchematicGridSettingsView`, `RoomBrowserView` for `reactiveGrid`; `ChatUserListView`, `ChatChannelListView` for `forEach`; `ChatMessageListView` for `virtualList` (capture the reference before `.children(...)`); drop the six `Signal.of(1)` column boilerplate args and update `<T>`-only field declarations
- [ ] 7.4 Migrate `button(Icon.x, Runnable)` call sites to `button(Runnable).icon(Icon.x)`
- [ ] 7.5 Clean `GodModeHudView`: replace the six `if (canEdit != null)` guard blocks with inline `.enabled(canEdit)`; remove the redundant `ResizeEvent` keep-in-screen listener

## 8. Tests

- [ ] 8.1 Add/extend tests for `Button.text()`, `Button.icon()` (static and reactive), including disposal of reactive content
- [ ] 8.2 Add tests for `UI.when(...)` mount/unmount/dispose
- [ ] 8.3 Add a test for chainable `spacer()` return type
- [ ] 8.4 Add a test asserting `SolimToken.isExpandingChild` heuristic behavior matches the former `Ui.isExpanding`
- [ ] 8.5 Migrate `ReactiveGridTest`, `ForEachComponentTest`, `VirtualListTest`, `StructuralReactivityTest`, `DisposalSweepTest`, and `LayoutTest` grid/forEach sites to the fluent API
- [ ] 8.6 `ReactiveGrid` fluent behavior tests: `reactiveGrid(items).key(k).columns(3).gap(8).children(factory)` mounts, reconciles item add/remove/update, and reflows when `columns` signal changes; columns default to 1 when `.columns(...)` is omitted; both `children(Function)` and `children(BiFunction)` forms receive a usable `GridItemContext` (non-null `itemWidth`, correct `columnCount`); static `Iterable` input wraps internally and re-renders when a wrapping `Readable` changes
- [ ] 8.6a Child rendering tests: after `SignalDispatcher.flush()`, every item's factory-produced element is actually present in the component tree (`table()` children contain one element per item, in item order); items removed from the collection disappear from the tree; items kept across updates keep their original element instance (keyed reuse, not recreation); `empty(...)`/`emptyView(...)` content renders when the collection is empty and is replaced by item children when non-empty; same rendering assertions for `ForEach` and mounted `VirtualList` visible-window items
- [ ] 8.7 Terminal `children(...)` tests: `children(...)` returns `void` (compile-time; asserted by call-site shape); config declared after `.children(...)` in a chain does not compile (asserted by grep in 9.1 since javac enforces it); building `reactiveGrid(items)` without `.children(...)` throws `IllegalStateException` naming `.children(...)`, and no reconcile ran
- [ ] 8.8 Key default tests: `forEach(items).children(factory)` reconciles by identity (`equals`/`hashCode`) preserving components for equal keys; duplicate keys throw `IllegalArgumentException` from the reconciler for both the identity default and an explicit `.key(...)`; `.key(...)` override restores keyed reuse (existing component instance retained across item-list order change)
- [ ] 8.9 `ForEach`/`VirtualList` fluent behavior tests: `forEach(items).key(k).children(factory)` mounts and preserves components across collection updates; `virtualList(items, heightProvider).key(k).overscan(2).onReachTop(t, cb).children(factory)` mounts only visible+overscan items, fires `onReachTop` at threshold, and updates when the collection signal changes; reference captured before `.children(...)` supports post-construction runtime calls (e.g. `.gap(...)`, pane queries)
- [ ] 8.10 Disposal tests: disposing a fluent-built `ReactiveGrid`/`ForEach`/`VirtualList` (including inside a parent `build()`) disposes the reconciler, all item components, reactive config bindings (`.columns(Readable)`, `.gap(Readable)`), and empty-view components exactly once with no surviving subscriptions

## 9. Verification

- [ ] 9.1 Grep the repository for residual references to removed APIs (`badgeCount`, `container(`, `divider('`, `divider("`, `Card.of(`, `buttonStyle(`, `backgroundDrawable`, `backButton(`, `sizedButton(`, `solim.core.Ui`, `icon()`, four-arg `grid(`, 4-arg `reactiveGrid(`, 3-arg `forEach(`, 4-arg `virtualList(`, `<T, K>` collection declarations)
- [ ] 9.2 Run `gradlew :solim-core:test`, `:solim:test`, `:solim-runtime:test`, and `:mod:test`
- [ ] 9.3 Run `gradlew :solim-core:checkstyleMain :solim:checkstyleMain :mod:checkstyleMain` (must pass with `ignoreFailures = false`)
- [ ] 9.4 Run `gradlew :solim:build :mod:build`
- [ ] 9.5 Run `openspec validate solim-ui-dx` and confirm the change is apply-ready