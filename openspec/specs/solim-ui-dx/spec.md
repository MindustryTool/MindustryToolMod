# solim-ui-dx Specification

## Purpose
TBD - created by archiving change solim-ui-dx. Update Purpose after archive.
## Requirements
### Requirement: Single public UI facade

The Solim library SHALL expose exactly one public declarative facade, `solim.UI`. The legacy `solim.core.Ui` facade SHALL be deleted. The attachment heuristic previously exposed as `Ui.isExpanding(Element)` SHALL move to `SolimToken.isExpandingChild(Element)`, while `SolimToken.isExpanding(Element)` SHALL retain its existing token-flag-only semantics. Consumers SHALL NOT be able to import or call any `solim.core.Ui` factory method.

#### Scenario: Legacy facade is absent
- **WHEN** the `solim-core` source tree is inspected
- **THEN** no `solim/core/Ui.java` exists and no public facade type named `Ui` is available to consumers

#### Scenario: Attachment heuristic preserved
- **WHEN** a layout attacher checks whether a child wants to expand its cell via `SolimToken.isExpandingChild(element)`
- **THEN** the result matches the former `Ui.isExpanding` behavior, including spacer names, `fillParent`, `ScrollPane`, and a table whose first child is a `ScrollPane`

#### Scenario: Unit helper consolidated
- **WHEN** core code needs UI unit scaling
- **THEN** it uses `solim.core.Units.unit(float)` / `Units.unit(int)`, and `solim.UI.unit(...)` delegates to the same source of truth

### Requirement: Reactive value acceptance follows the Readable rule

UI facade methods that accept reactive content SHALL declare a single `Readable<T>` overload rather than separate `Signal<T>` and `Computed<T>` overloads. Plain values keep their own overload where the plain type is not a `Readable` (for example `String`, `int`, `Drawable`). This SHALL apply to `text(...)` and to any future reactive property that would otherwise duplicate overloads per reactive subtype.

#### Scenario: Signal and Computed bind without dedicated overloads
- **WHEN** `text(signal)` is called with a `Signal<String>` and `text(computed)` is called with a `Computed<String>`
- **THEN** both resolve to `text(Readable<String>)` and update the label reactively without per-subtype facade methods

#### Scenario: Static content still resolves
- **WHEN** `text("Settings")` is called
- **THEN** it resolves to `text(String)` and renders static text

### Requirement: Button fluent content composition

`Button` SHALL provide fluent content methods `text(String)`, `text(Readable<String>)`, `icon(Drawable)`, and `icon(Readable<Drawable>)` that append the corresponding child content to the button. `icon(...)` SHALL apply the standard Solim icon treatment: scalable drawable, `Scaling.fit`, and `unit(6)` sizing. Content SHALL append in call order, and reactive content bindings SHALL be owned by and disposed with the button.

#### Scenario: Fluent text button
- **WHEN** `button().text("Save").onClick(this::save)` is declared
- **THEN** the button contains a text label and invokes `save()` on click

#### Scenario: Fluent icon button
- **WHEN** `button().style(WebStyles.ghost()).size(unit(11)).icon(Icon.add).onClick(r)` is declared
- **THEN** the button contains an icon child sized at `unit(6)` with no `children(() -> ...)` lambda required

#### Scenario: Reactive icon updates without recreation
- **WHEN** `button().icon(playIcon)` is declared with `Readable<Drawable> playIcon` and the icon value changes
- **THEN** the button's icon drawable updates in place

#### Scenario: Reactive content disposed with the button
- **WHEN** a button built with `text(readable)` is disposed
- **THEN** the reactive binding's effect is disposed and no subscription survives

### Requirement: Keyed reactive collections use reactiveGrid only

`solim.UI` SHALL expose keyed, structurally-reactive collections only through `reactiveGrid(...)`. The four-argument `grid(...)` overloads SHALL be removed. `grid(...)` SHALL remain for static layout grids (fixed or reactive column count without a keyed item collection).

#### Scenario: reactiveGrid renders a keyed collection
- **WHEN** `reactiveGrid(items).columns(cols).key(k).children(factory)` is declared
- **THEN** a `ReactiveGrid` reconciles items by key and reflows on column-count change

#### Scenario: Static grid remains
- **WHEN** `grid(3).children(() -> { /* static children */ })` is declared
- **THEN** a plain `Grid` lays out the children in three columns with no keyed reconciliation

### Requirement: Keyed collections use fluent configuration with terminal children

`ReactiveGrid`, `ForEach`, and `VirtualList` SHALL follow the pattern **fundamental data on the factory → fluent configuration → terminal `.children(...)`**. The factory SHALL accept only fundamental data (`reactiveGrid(items)`, `forEach(items)`, `virtualList(items, heightProvider)`); static collections SHALL be accepted directly and wrapped internally. `ReactiveGrid` column count SHALL default to 1 with `.columns(int | Readable<Integer>)` overriding. The required item factory SHALL be supplied through a terminal `children(Function<T, Component>)` (and `children(BiFunction<T, GridItemContext, Component>)` on `ReactiveGrid`) that returns `void`, so configuration cannot continue after it. Component generics SHALL collapse from `<T, K>` to `<T>`; the key type SHALL remain internal. Omitting `.key(...)` SHALL default to identity keys with the reconciler's duplicate-key detection as the only guard. Optional configuration methods (`.key(...)`, `.columns(...)`, `.gap(...)`, `.empty(...)`, `.emptyView(...)`, `.overscan(...)`, `.onReachTop(...)`, `.onReachBottom(...)`, cell/table modifiers) SHALL live on the component, return the component, and be order-independent. Post-construction runtime configuration (e.g. `grid.columns(5)` on a retained reference) SHALL remain functional; terminality SHALL be construction-time API guidance only. No builder or config intermediate objects SHALL be introduced, and the fluent form SHALL add no allocations, subscriptions, effects, or reconciliation passes beyond field assignment.

#### Scenario: Grid with defaults and fluent configuration
- **WHEN** `reactiveGrid(feature.trackSignal(type)).key(track -> track.id).children(track -> new MusicTrackCard(feature, track))` is declared
- **THEN** a single-column reactive grid renders without any `Signal.of(1)` boilerplate and the item factory is the terminal call

#### Scenario: Terminal children ends the chain at compile time
- **WHEN** `reactiveGrid(items).children(item -> render(item)).gap(8)` is attempted
- **THEN** the code does not compile because `children(...)` returns `void`

#### Scenario: Configuration order is irrelevant
- **WHEN** `reactiveGrid(items).columns(3).gap(8).key(k).children(f)` and `reactiveGrid(items).key(k).gap(8).columns(3).children(f)` are declared
- **THEN** both produce identically configured grids

#### Scenario: Context-aware item factory
- **WHEN** `reactiveGrid(items).columns(cols).children((item, ctx) -> card(item).width(ctx.itemWidth()))` is declared
- **THEN** the item factory receives the `GridItemContext` and item width updates reactively

#### Scenario: Identity key default
- **WHEN** `forEach(items).children(factory)` is declared without `.key(...)`
- **THEN** items reconcile by identity (`equals`/`hashCode`) and duplicate keys throw `IllegalArgumentException` from the reconciler

#### Scenario: Reference captured before children
- **WHEN** a caller retains the component (`var list = virtualList(items, heights).key(k); list.children(factory); this.list = list;`)
- **THEN** the field reference works for later runtime configuration and scroll queries

#### Scenario: Missing item factory renders empty until supplied
- **WHEN** `reactiveGrid(items)` is built (its element forced by configuration or layout) without any `.children(...)` call
- **THEN** it renders empty with no reconcile, and calling `.children(...)` afterwards refreshes the already-built content exactly once

#### Scenario: Generics collapse
- **WHEN** a caller declares a field `VirtualList<MessageGroup> virtualList;`
- **THEN** the type compiles with a single type parameter and the key type never appears in any public signature

### Requirement: Conditional rendering sugar when()

`solim.UI` SHALL provide `when(Readable<Boolean> condition, Supplier<Component> supplier)` that mounts the supplied component when the condition is truthy and unmounts it when falsy, delegating to `Dynamic` so that existing lifecycle and disposal rules apply. `dynamic(...)` SHALL remain available for non-boolean value switching.

#### Scenario: Condition true mounts content
- **WHEN** `when(visible, () -> button("X", r))` is declared and `visible` becomes true
- **THEN** the button is mounted in the current parent

#### Scenario: Condition false unmounts and disposes
- **WHEN** the same `when(...)` condition becomes false
- **THEN** the mounted component is unmounted and disposed exactly once

#### Scenario: dynamic remains for value switching
- **WHEN** `dynamic(providerSignal, provider -> provider != null ? a() : b())` is declared
- **THEN** the value-switching behavior is unchanged

### Requirement: Chainable spacer

`solim.UI.spacer()` SHALL return the `Spacer` component (not a raw `Element`) so callers can chain configuration such as `.name(...)`, while continuing to attach the spacer element to the active parent.

#### Scenario: Spacer is chainable
- **WHEN** `spacer().name("gap")` is declared inside an active parent
- **THEN** a `Spacer` is created, its element attached to the parent, and its name applied

### Requirement: Dead facade methods removed

The UI facade SHALL NOT expose methods with no call sites that duplicate existing APIs. The following SHALL be removed: `badgeCount(...)`, `divider(String)`, `divider(char)`, `container()`, `container(Runnable)`, no-arg `icon()`, `buttonStyle()` (both variants), `hud(Cons<Hud>)`, `Hud.backgroundDrawable(...)`, `SolimDialog.backButton()`, `SolimDialog.content(Runnable)`, and `Button.sizedButton()`. Count badges SHALL be expressed through `badge(Readable<String>)` with caller-side formatting.

#### Scenario: Removed methods are unavailable
- **WHEN** the public API is inspected
- **THEN** none of the removed methods are declared on `UI`, `Hud`, `SolimDialog`, or `Button`

#### Scenario: Count badge via mapped Readable
- **WHEN** a reactive count badge is needed
- **THEN** it is declared as `badge(count.map(v -> v > 99 ? "99+" : String.valueOf(v)))` and updates reactively

