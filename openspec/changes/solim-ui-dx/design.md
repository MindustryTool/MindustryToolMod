## Context

`solim.UI` is the public declarative facade for the UI library; `solim-core` holds the components and mixins. A DX audit (see proposal) confirmed all decisions with the user. This design records the technical choices that make the migration safe and coherent, including the rejected alternatives.

Current relevant facts verified in the audit:

- `solim.core.Ui` is a 450-line legacy facade duplicating `solim.UI` and has already diverged (`wrap()` returns `Row`; `icon(Drawable)` is unsized). It is referenced only by ~10 solim-core test files plus one internal call (`Tabs` → `Ui.unit(14)`). Its only production helper is `Ui.isExpanding(Element)`, used by 13 layout call sites.
- `SolimToken.isExpanding(Element)` is a pure token-flag check used by `Ui.isExpanding`, `SpacerTest`, `SolimTokenTest`.
- `Button.enabled(@Nullable Readable<Boolean>)` already null-tolerates; `visible`, `checked`, `style`, `text(Readable)` do too.
- `PendingCellConfig.DEFAULT_CONFIGURATOR` applies deferred cell constraints at attach time; `BaseComponent`/`LeafComponent` already self-register as pending children. `Text`, `Button`, `Divider`, `Spacer`, `SolimStack`, `Hud`, `Tabs` rely on factory-level immediate attach.
- `Text.of(...)` and `UI.text(...)` each carry 4 overloads (`String`, `Signal`, `Computed`, `Readable`).
- `grid(...)` exposes 6 static-grid plus 2 four-arg keyed-collection overloads; `reactiveGrid(...)` is a pure alias delegating to the 4-arg `grid`.
- `ReactiveGrid<T, K>` requires 4 positional args (`columnCount`, `items`, `keyExtractor`, `itemFactory`); `ForEach<T, K>` requires 3 (`collection`, `keyExtractor`, `itemFactory`); `VirtualList<T, K>` requires 4 (`collection`, `keyExtractor`, `heightProvider`, `itemFactory`). Six mod call sites pass `Signal.of(1)` solely to satisfy the required `columnCount`. The `K` type parameter never escapes any component's public API — it is used only by the internal `StructuralReconciler` — yet callers must write it explicitly in field declarations (e.g. `VirtualList<MessageGroup, String>`).
- Java target: Java 17 language level, Java 8 runtime (`options.release = 8`). Checkstyle runs with `ignoreFailures = false`.

## Goals / Non-Goals

**Goals:**

- One coherent public facade: `solim.UI` only; no diverged legacy duplicate.
- Common cases stay one-liners; advanced cases use the existing fluent mixins.
- Reactive value handling follows a single `Readable<T>` rule.
- Keyed collections have exactly one name (`reactiveGrid`), conditional rendering has a typed boolean sugar (`when`).
- Keyed-collection components (`ReactiveGrid`, `ForEach`, `VirtualList`) follow the fluent configuration + terminal `.children(...)` pattern: fundamental data on the factory, optional configuration as chainable methods, required item factory as a `void` terminal `.children(...)`.
- Dead/broken APIs removed rather than aliased; all usages migrated (no `@Deprecated` shims).
- Mod-level icon-button boilerplate collapses to one call.

**Non-Goals:**

- No change to attachment timing (pending vs immediate) — deferred to a later change.
- No change to `Component`/`BaseComponent`/`LeafComponent` lifecycle contracts, `ParentStack`, `ComponentContext`, `PendingCellConfig`, `StructuralReconciler`.
- No `build()` signature change; no second component model; no builder interfaces per component.
- No Query/Mutation changes.
- No change to component constructor signatures of non-facade classes beyond those needed to delete facade overloads.

## Decisions

### D1. Delete `solim.core.Ui`; relocate `isExpanding` to `SolimToken`

`SolimToken` gains `isExpandingChild(Element)` which combines the pure token flag with the existing heuristics (spacer names, `fillParent`, `ScrollPane`, table whose first child is a `ScrollPane`). `SolimToken.isExpanding(Element)` keeps its exact current token-only semantics so `SpacerTest` and `SolimTokenTest` stay valid. The 13 layout call sites (`Card`, `Column`, `Grid`, `Row`, `Scroll`, `SolimCollapser`, `Wrap`, `Dynamic`, `ForEach`, `QueryView`) switch to `isExpandingChild`.

`Tabs` uses `Ui.unit(14)`; consolidate the duplicated `BASE_UNIT` (currently in both `solim.UI` and `solim.core.Ui`) into `solim.core.Units` as `unit(float)`/`unit(int)`, with `UI.unit(...)` delegating. This removes the last production dependency on the legacy facade.

The ~10 solim-core tests migrate to `solim.UI` (for facade behavior) or direct component constructors (`new Column()`, `new Divider(Direction.X)`, `new Spacer()` + `ParentStack.attachToParent`). Tests that specifically exercise attach semantics should construct components directly, since they assert on the same mechanism the factories use.

*Alternative considered:* keep `Ui` as a slim internal helper. Rejected — it preserves a second public type with a near-identical name, which is the core hazard. Relocating one method is cheaper.

### D2. Button fluent content, overload collapse

`Button` gains:

- `text(String)` / `text(Readable<String>)` — appends a `Text` child inside the button scope (equivalent to the current `children(() -> text(...))` body).
- `icon(Drawable)` / `icon(Readable<Drawable>)` — appends an icon child using the standard Solim icon treatment (`Drawables.scalable`, `Scaling.fit`, `size(unit(6))`), matching `UI.icon(...)`.

Content is appended in call order; calling `text` twice adds two labels (documented, same as two `children` calls). Reactive bindings created by the child `Text` register with the button's component context and are disposed with it.

Implementation detail: `Button` lives in `solim-core` and cannot call `solim.UI.icon`. It reuses `SolimImage` + `Drawables.scalable` + `Scaling.fit` + `Units.unit(6)`, which are all in `solim-core`. Introduce a tiny shared helper (e.g. `solim.display.Icons.append(parentTable, drawable)`) only if duplication with `UI.icon` becomes non-trivial; otherwise inline the three calls.

Kept factories: `button()`, `button(Runnable)`, `button(String, Runnable)`, `button(Readable<String>, Runnable)`.
Removed factories: `button(Drawable)`, `button(Drawable, Runnable)`, `button(String, Drawable, Runnable)`, `button(String, Runnable, Consumer<SolimButtonStyleBuilder>)`, `button(Readable<String>, Runnable, Consumer<SolimButtonStyleBuilder>)`.
The 3 mod sites using `button(Icon.edit, () -> ...)` migrate to `button(() -> ...).icon(Icon.edit)`.

*Alternative considered:* keep `button(Drawable, Runnable)` as convenience. Rejected — with `.icon()` it is redundant, and every retained overload must be maintained consistently.

### D3. `text(...)` collapses to String + Readable

`Signal<T>` and `Computed<T>` both implement `Readable<T>`, so `text(Readable<String>)` resolves every existing call site. `Text.of(...)` collapses identically. `String` is not a `Readable`, so no ambiguity is introduced. Lambdas passed to `text` do not exist in the codebase, so functional-interface overload ambiguity is not a concern.

*Alternative considered:* keep `Signal`/`Computed` overloads for javadoc clarity. Rejected — they are type-hierarchy redundancy with zero behavioral difference.

### D4. `reactiveGrid` is the only keyed-collection facade

Delete the two 4-arg `grid(...)` overloads. Per D10, `reactiveGrid(items)` takes only the fundamental data and configures the rest fluently. Static layout grids keep `grid()`, `grid(int)`, `grid(Runnable)`, `grid(int, Runnable)`, `grid(Readable<Integer>)`, `grid(Readable<Integer>, Runnable)`. Two mod call sites (`QuickAccessHudView`, `FeatureSettingsView`) migrate to the fluent form. This matches the existing AGENTS.md rule ("prefer `reactiveGrid(...)`; never use the static `grid(...)` for dynamic content").

### D5. `when(Readable<Boolean>, Supplier<Component>)`

Implemented as a thin wrapper over `Dynamic`: `when(cond, supplier)` creates `Dynamic<T>` with a factory that returns `supplier.get()` when the condition is truthy and `null` otherwise. `dynamic` is retained for value-switching (e.g. `provider != null ? a : b`). The supplier-created component is mounted/unmounted by `Dynamic`'s existing structural reconciliation, so disposal follows existing lifecycle rules — no new ownership path.

*Alternative considered:* deprecate `dynamic` in favor of `when`. Rejected — `dynamic` handles non-boolean value switches that `when` cannot express without an awkward `.map`.

### D6. `spacer()` returns `Spacer`

No call site assigns the result to `Element` (verified), so tightening the return type is source-compatible and enables `spacer().name(...)`. `UI.spacer()` continues to attach the spacer element to the current parent as today.

### D7. Dead API removal (breaking, migrated in the same change)

Remove `UI.badgeCount`, `divider(String)`, `divider(char)`, `container()`, `container(Runnable)`, `Card.of(Runnable)`, no-arg `icon()`, `UI.buttonStyle()` and `UI.buttonStyle(Consumer)`, `hud(Cons<Hud>)`, `Hud.backgroundDrawable`, `SolimDialog.backButton()`, `SolimDialog.content(Runnable)`, `Button.sizedButton()`.

- `badgeCount(Readable<Integer>)` had no call sites; count badges are expressed as `badge(count.map(v -> ...))`, which also supports "99+" formatting (already the documented scenario). `badge(int)` stays.
- `SolimDialog.content()` had no mod call sites; `children()` is the consistent name and stays.
- `Card.of(Runnable)` is removed rather than fixed: it silently ignored its argument, had no callers, and `UI.card(Runnable)` already does the right thing.
- `Button.sizedButton()` is tested (`ButtonComponentTest`, etc.); those tests migrate to `button()`.

Reactive count badge note: `badge(Readable<String>)` and a hypothetical `badge(Readable<Integer>)` cannot coexist (same erasure), which is why a separate count facade existed. Removing it is deliberate; formatting belongs to the caller.

### D8. Mod-side `WebStyles.iconButton`

Add:

```java
public static Button iconButton(Drawable icon, Runnable onClick)
public static Button iconButton(Drawable icon, String tooltip, Runnable onClick)
```

Building `button(onClick).style(GHOST_STYLE).size(unit(11)).icon(icon)` (plus tooltip). Reactive/scaled icon cases (e.g. GodMode's `buttonSize`/`iconSize`) and colored icons stay fluent/`children()` because the helper intentionally fixes the standard `unit(11)`/`unit(6)` convention. Migrate the ~50 exact-shape ghost icon-button sites. Also delete `GodModeHudView`'s redundant `if (canEdit != null)` guards (append `.enabled(canEdit)` directly) and its duplicate `ResizeEvent` listener (`Hud` already calls `keepInScreen` on resize).

### D9. Rejected: attachment-timing unification

Moving all components to constructor-time pending registration and dropping factory `attachToParent` calls is desirable (it fixes the bare `column()`/`card()` orphan trap) but changes observable attach timing across the component set. The user deferred it to a separate change. This change does **not** touch it, so the orphan-trap fix and the `Card.of` class of confusion is only partially addressed (the broken `Card.of` itself is removed).

### D10. Fluent keyed collections with terminal `.children(...)`

`ReactiveGrid`, `ForEach`, and `VirtualList` migrate to the pattern **fundamental data on the factory → fluent configuration → terminal `.children(...)`**. Decisions confirmed with the user:

- **Fundamental data only on the factory.** `reactiveGrid(items)`, `forEach(items)`, `virtualList(items, heightProvider)`. Static `Iterable`/`List` inputs wrap in `Readable.of(...)` at the facade. `VirtualList` keeps `heightProvider` on the factory (fundamental to virtualization math; a default would guess heights and jank the scroll). Column count defaults to 1 (already the internal fallback); `.columns(int | Readable<Integer>)` overrides.
- **Terminal `.children(...)` supplies the required item factory and returns `void`.** Signatures: `children(Function<T, Component>)` on all three; `children(BiFunction<T, GridItemContext, Component>)` on `ReactiveGrid` (context-aware form). Configuring after `.children(...)` does not compile, giving the natural lifecycle `configure → define children → finished`. The component already exists at factory time, so this is not a `build()` — it supplies how children are created. Config-before-children ordering (`.gap()`, `.empty()` before the factory lambda) fixes the current inverted readability where options dangle after a multiline lambda.
- **Generics collapse from `<T, K>` to `<T>`.** `K` never escapes any component's public API; `StructuralReconciler<Object, Component>` holds it internally and the key function is stored as `Function<T, ?>`. Field declarations shrink (`VirtualList<MessageGroup, String>` → `VirtualList<MessageGroup>`), and Java infers `T` from the items argument.
- **Key defaults to identity, no guard.** Omitting `.key(...)` uses `Function.identity()`; the reconciler's existing `IllegalArgumentException` on duplicate keys is the only safety net (user decision). `.key(Function<T, ?>)` overrides. `equals`/`hashCode`-based reconciliation remains the caller's responsibility when overriding.
- **The component itself is the fluent config object.** No `ReactiveGridBuilder`/`Config` intermediates, no allocations. Existing config methods (`gap`, `empty`, `emptyView`, `overscan`, `onReachTop`, `onReachBottom`, cell/table modifiers) stay on the component and return `this`. Configuration is order-independent; `.children(...)` is the only terminal op.
- **Runtime mutability preserved.** `gap(Readable<Float>)`-style reactive config and post-construction runtime calls (e.g. `grid.columns(5)` on a retained reference) keep working; terminality is construction-time API guidance only. Reference capture pattern for post-config use: assign to a variable *before* `.children(...)` (e.g. ChatMessageListView's retained `virtualList` field).
- **Lifecycle preserved.** `BaseComponent` constructor registers the pending child at factory time; item factory is required at first `build()`/reconcile — a missing factory throws `IllegalStateException` with guidance. No new subscriptions, effects, allocations, or reconciliation passes; the fluent form is field assignment only.

Facade shape:

```java
// before
reactiveGrid(Signal.of(1), feature.trackSignal(type), track -> track.id,
        track -> new MusicTrackCard(feature, track));

// after
reactiveGrid(feature.trackSignal(type))
        .key(track -> track.id)
        .children(track -> new MusicTrackCard(feature, track));
```

Migrated components and their fluent config surfaces: `ReactiveGrid` (`.columns`, `.gap`, `.key`, `.empty`, `.emptyView`), `ForEach` (`.key`), `VirtualList` (`.key`, `.overscan`, `.gap`, `.onReachTop`, `.onReachBottom` — already fluent, join the pattern). `dynamic(...)` and `when(...)` are untouched: they switch whole subtrees, they are not child generators.

*Alternative considered:* keep positional constructors and add overloads with fewer args. Rejected — overload matrices grow combinatorially; the fluent form removes the six `Signal.of(1)` boilerplate sites and lets IDE autocomplete guide configuration order.

*Alternative considered:* mandatory `.key(...)` with a build-time guard. Rejected — identity keys are valid for the many list sites whose items are already keyed by `equals` (e.g. `ChatUser::getName` equivalents); the reconciler's duplicate-key throw covers misuse.

## Risks / Trade-offs

- [Removing `solim.core.Ui` breaks `solim-core` tests that import it] → Migrate tests in the same change; `SolimToken.isExpanding` semantics unchanged so token tests pass untouched. Run the full `:solim-core:test` suite.
- [Button `.text()` name could be shadowed by a static import of `UI.text` inside `Button`] → `Button` does not static-import `UI`; inner calls delegate explicitly. Verify via compilation.
- [`Button.icon()` hardcodes `unit(6)` while some mod sites use scaled icon sizes] → Helper covers standard cases only; scaled/colored icon sites remain fluent. Documented in `Button.icon` javadoc.
- [Bulk migration of ~50 mod icon-button sites risks visual drift] → Migrate only exact-shape chains (`.ghost().size(unit(11)).tooltip(...).onClick(...).children(icon unit(6))`); leave variants (checked chips, colored icons, non-ghost styles, reactive sizes) untouched. Compile + run mod tests.
- [Removing `grid(4-arg)` could break unseen callers] → Repo-wide grep confirmed only 2 mod call sites, both migrated; `:mod` compile validates.
- [Checkstyle `ignoreFailures = false`] → Run `checkstyleMain` across modules; removals/import churn must keep imports ordered and unused imports pruned.
- [`when()` grows the facade surface] → It replaces the dominant hand-written guard; it is a thin delegation, not a new component type. No new lifecycle semantics.
- [Identity key default silently rebuilds items lacking proper `equals`] → Reconciler throws on duplicate keys; sites relying on `equals`-based keys (e.g. `ChatUser::getName`) work unchanged. Reviewers can spot missing `.key(...)` in review since it is now visible at the call site.
- [`<T, K>` → `<T>` breaks mod field declarations] → Mechanical migration: ~10 `reactiveGrid` sites, 2 `forEach` sites, 1 `virtualList` site plus field types; `:mod` compile catches all.
- [Configuring after terminal `.children(...)` no longer compiles] → Intentional guidance. Callers needing post-config references assign the component to a variable before `.children(...)` (ChatMessageListView pattern).
- [Breaking API churn] → Accepted per the project migration rule: one coherent API over indefinite deprecation. Specs updated with the deltas.

## Migration Plan

1. `solim-core`: add `SolimToken.isExpandingChild`, `Units.unit(...)`; migrate 13 layout call sites and `Tabs`.
2. `solim-core`: add `Button.text/icon`; collapse overloads; remove dead methods from `Card`, `Text`, `Badge`, `Hud`, `SolimDialog`, `SolimToken`.
3. `solim-core`: migrate `ReactiveGrid`, `ForEach`, `VirtualList` to fluent configuration + terminal `children(...)` per D10; collapse generics to `<T>`.
4. `solim`: rewrite `UI.java` (text overloads, button overloads, `reactiveGrid`/`forEach`/`virtualList` fluent factories, `when`, `spacer` return, remove dead facade methods, delegate `unit`).
5. Delete `solim-core/src/solim/core/Ui.java`.
6. Migrate solim-core tests off `Ui`; update Button/Tabs/ReactiveGrid/ForEach/VirtualList tests; add tests for new APIs (terminal `children`, identity-key default, missing-factory guard).
7. Mod: add `WebStyles.iconButton`; migrate icon-button sites, `reactiveGrid`/`forEach`/`virtualList` call sites to the fluent form, `GodModeHudView` cleanup.
8. Update `openspec/specs/solim-widgets` and `openspec/specs/solim-lifecycle` deltas; run full test suites + checkstyle; grep for residual references to removed APIs.

Rollback: revert the change commit; all removals are within version control and no persisted/runtime state depends on the facade shape.

## Open Questions

- Whether `Button.icon` should also expose an explicit size parameter (e.g. `icon(Drawable, float)`) or keep custom sizing to `children()`. Current decision: keep it out until a concrete need beyond the GodMode scaled case appears.