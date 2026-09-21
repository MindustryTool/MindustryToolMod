## Why

A Developer Experience audit of the Solim public UI API found systemic problems: `solim.core.Ui` is a diverged duplicate of the public `solim.UI` facade (already drifted: `wrap()` returns `Row` there, `icon()` is unsized), `Button` carries an 8-overload constructor matrix because it lacks fluent content methods, `text()` has 4 redundant overloads, `grid`/`reactiveGrid` name the same keyed-collection API, and roughly a dozen dead or broken aliases exist (including `Card.of`, which silently discards its argument). The mod module repeats a 6-line icon-button incantation ~50 times and guards nullable `enabled()` with 6 identical intermediate-variable blocks. These issues make the API hard to discover, inconsistent, and verbose for the common case. All decisions were confirmed with the user during the audit.

## What Changes

- **BREAKING** Delete the legacy `solim.core.Ui` facade entirely; move the one legitimate helper (`Ui.isExpanding`) into `SolimToken`; migrate ~10 solim-core test files off the legacy facade; replace the internal `Ui.unit(14)` call in `Tabs` with `Units`/local computation.
- **BREAKING** `Button` gains fluent content methods `.text(String)` / `.text(Readable<String>)` and `.icon(Drawable)` / `.icon(Readable<Drawable>)` (icon applies the standard `unit(6)` icon sizing). The constructor overload matrix collapses to `button()`, `button(Runnable)`, `button(String, Runnable)`, `button(Readable<String>, Runnable)`; removed: `button(Drawable)`, `button(Drawable, Runnable)`, `button(String, Drawable, Runnable)`, `button(text, onClick, Consumer<SolimButtonStyleBuilder>)` (both variants).
- **BREAKING** `text(...)` collapses to `text(String)` + `text(Readable<String>)` (Signal/Computed are `Readable`s, so all existing call sites compile unchanged); `Text.of(...)` likewise collapses to 2 overloads.
- **BREAKING** The 4-arg keyed `grid(...)` overloads are removed; `reactiveGrid(...)` becomes the only keyed-collection API (per the existing AGENTS.md mandate). Two mod call sites migrate.
- **BREAKING** Keyed-collection components migrate to fluent configuration with terminal `.children(...)`: `reactiveGrid(items)`, `forEach(items)`, `virtualList(items, heightProvider)` take only fundamental data; column count defaults to 1 (`.columns(...)` overrides); the required item factory is supplied via terminal `void` `.children(...)`; generics collapse from `<T, K>` to `<T>`; omitting `.key(...)` defaults to identity keys. Approximately 13 mod call sites plus field declarations and solim-core tests migrate.
- New `when(Readable<Boolean>, Supplier<Component>)` structural sugar for the dominant boolean-guard `dynamic()` pattern (67 sites); `dynamic()` remains for value-switching.
- `spacer()` returns the `Spacer` component instead of `Element`, enabling chaining.
- **BREAKING** Dead API removal (all zero- or test-only call sites, verified in audit): `UI.badgeCount(...)`, `divider(String)`, `divider(char)`, `container()`, `container(Runnable)`, `Card.of(...)`, no-arg `icon()`, `UI.buttonStyle()` (both), `hud(Cons<Hud>)`, `Hud.backgroundDrawable(...)`, `SolimDialog.backButton()`, `SolimDialog.content(...)`, `Button.sizedButton()`. Reactive count badges become `badge(count.map(...))` per the existing spec scenario.
- Mod-side: new `WebStyles.iconButton(Drawable icon, String tooltip, Runnable onClick)` helpers built on `Button.icon()`; migrate ~50 ghost-icon-button sites; delete GodModeHudView's 6 redundant `if (canEdit != null)` blocks (Button.enabled already tolerates null) and its duplicate ResizeEvent keep-in-screen listener (Hud already registers one).
- Test coverage for the fluent collection APIs: behavioral tests asserting configuration-before-children mounts and reconciles correctly, generated children actually render into the component tree (one element per item, keyed reuse after updates, correct removal), terminal `children(...)` cannot be chained past, missing factories fail fast, identity-key default reconciliation and duplicate-key rejection, column-count default of 1, reactive `.columns()`/`.gap()` updates, context-aware item factories, and disposal without leaks for `ReactiveGrid`, `ForEach`, and `VirtualList`.
- Attachment timing (pending-attach in constructors vs immediate factory attach) is explicitly **not** changed here; it is a separate future change.
- Query/Mutation APIs are untouched.

## Capabilities

### New Capabilities

- `solim-ui-dx`: Coherent public UI facade rules — single facade (`solim.UI`), Button fluent content API, collapsed reactive-value overload rule (`Readable` acceptance), `reactiveGrid`-only keyed collections, fluent configuration with terminal `.children(...)` for keyed collections (`ReactiveGrid`, `ForEach`, `VirtualList`), `when()` conditional rendering, chainable `spacer()`, and removal of dead facade methods.

### Modified Capabilities

- `solim-widgets`: Overload matrix requirements change — Button constructor overloads shrink and gain fluent content; text widget overload set narrows to String/Readable; keyed grid facade renamed to reactiveGrid-only and migrated to fluent configuration with terminal `.children(...)`; `ForEach`/`VirtualList` migrate to the same fluent pattern; badge count facade removed; divider string/char facades removed; container facade removed; spacer facade returns the component; new `when()` facade alongside `dynamic()`.
- `solim-lifecycle`: Widget facade list changes (`badgeCount` removed, `when` added, legacy `solim.core.Ui` facade deleted with `isExpanding` relocated to `SolimToken`).

## Impact

- **Solim modules**: `solim/src/solim/UI.java` (overload removal, `when`, `spacer` return type, fluent keyed-collection factories), `solim-core` (delete `solim/core/Ui.java`, `Button`, `Text`, `Badge` factory cleanups, `ReactiveGrid`/`ForEach`/`VirtualList` fluent migration, `SolimToken.isExpanding`, `Tabs` internal call), solim-core test files (~10 using `solim.core.Ui`, plus ReactiveGrid/ForEach/VirtualList test files), spec files under `openspec/specs/solim-*`.
- **Mod module**: `WebStyles.java` (new `iconButton` helpers), ~50 icon-button call sites, ~13 keyed-collection call sites (`reactiveGrid`/`forEach`/`virtualList`) migrated to the fluent form plus `<T>`-only field declarations, `GodModeHudView.java` cleanup.
- **Tests**: Button/Tabs/UI-facade tests updated for removed overloads; new tests for `Button.text()/icon()`, `when()`, `spacer()` chaining; migrated legacy-facade tests.
- **No runtime architecture changes**: ParentStack, ComponentContext, PendingCellConfig, StructuralReconciler, and Query/Mutation are untouched.
