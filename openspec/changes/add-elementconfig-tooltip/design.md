## Context

In Solim, `ElementConfig<SELF>` provides common element-level modifier methods (`width`, `height`, `size`, `position`, `visible`, `opacity`, `name`, `onClick`, `draggable`). Currently, tooltips are only exposed as component-specific methods on `Button`. As a result, other Solim components such as `Checkbox`, `Row`, `Column`, and `Card` cannot declare tooltips directly.

In `GeneralSettingsView.java`, the lack of a `.tooltip()` modifier on `Checkbox` led to an anti-pattern: creating temporary `Row` variables wrapping individual checkboxes and imperatively attaching Arc `Tooltip` instances via `.table().addListener(new Tooltip(...))`.

## Goals / Non-Goals

**Goals:**
- Add `.tooltip(String)`, `.tooltip(Readable<String>)`, and `.tooltip(Cons<Table>)` directly to `ElementConfig`.
- Implement listener replacement semantics so repeated `.tooltip()` calls clean up previous tooltip listeners rather than stacking duplicates.
- Ensure reactive tooltips automatically bind a `Label` to the `Readable<String>` and register the underlying `Effect` with `ComponentContext` for ambient lifecycle management.
- Remove duplicate tooltip logic from `Button` so it inherits directly from `ElementConfig`.
- Migrate `GeneralSettingsView.java` to pure declarative Solim, removing intermediate variables, unused wrapper rows, and imperative listener calls.
- Add unit tests in `solim-core` verifying tooltip attachment, replacement, and reactive updates.

**Non-Goals:**
- Custom re-implementation of Arc's hover popup / tooltip system (`arc.scene.ui.Tooltip`).
- Refactoring settings views other than `GeneralSettingsView.java` (though custom builder support prepares other views such as `PrettyChatSettingsView` for future cleanup).

## Decisions

### Decision 1: Place Tooltips in `ElementConfig` Mixin
- **Rationale**: `ElementConfig` is already the common ancestor for all Solim components with an underlying Arc `Element`. Putting tooltips here immediately makes them available on `Checkbox`, `Button`, `Row`, `Column`, `Card`, `Text`, `SolimImage`, etc., without duplicating code across components.
- **Alternatives Considered**: Adding `.tooltip()` individually to `Checkbox` and `Row`. Rejected because it creates fragmentation and leaves out other components like `Card` or `Text`.

### Decision 2: Tooltip Overloads (`String`, `Readable<String>`, `Cons<Table>`)
- **Signatures**:
  1. `default SELF tooltip(@Nullable String tip)`
  2. `default SELF tooltip(@Nullable Readable<String> tip)`
  3. `default SELF tooltip(@Nullable Cons<Table> tooltipBuilder)`
- **Rationale**: Covering static text, reactive signals/computeds, and custom table builders addresses 100% of tooltip use cases across the mod (including simple labels, dynamic localized text, and styled multi-line cards).
- **Implementation**:
  - `tooltip(String)` delegates to `tooltip(t -> t.add(tip))`. Passing `null` or `""` removes existing tooltip listeners.
  - `tooltip(Readable<String>)` creates a `Label`, instantiates an `Effect` registered via `ComponentContext.register(e)`, and adds the label to `t`. Passing `null` removes existing tooltips.
  - `tooltip(Cons<Table>)` cleans up prior `Tooltip` listeners on `element()`, then adds `new Tooltip(tooltipBuilder)`.

### Decision 3: Listener Cleanup and Replacement
- **Rationale**: Arc's `Element.addListener(...)` allows multiple listeners. If a component reconfigures its tooltip or clears it, multiple tooltips could trigger concurrently. Iterating backwards through `element().getListeners()` to remove existing `Tooltip` instances ensures clean replacement semantics, matching `ElementConfig.onClick()` behavior.

### Decision 4: Clean up `GeneralSettingsView.java`
- **Rationale**: Remove `Row betaRow = ...; betaRow.table().addListener(...)`. Directly declare `.tooltip(...)` on `checkbox(...).growX()`. Remove unnecessary single-child wrapper rows. Remove `import arc.scene.ui.Tooltip;` and `import solim.layout.Row;`.

## Risks / Trade-offs

- **[Risk]** `element().getListeners()` returns a `SnapshotArray` which might be modified during event dispatch.
  - **Mitigation**: Iterate backwards using index or standard snapshot iteration when removing existing `Tooltip` instances.
- **[Risk]** Reactive effects created inside `new Tooltip(t -> ...)` could leak if `BaseComponent` is never disposed.
  - **Mitigation**: Tooltip effect is registered via `ComponentContext.register(e)` with the active building component, so it disposes synchronously with the view lifecycle.
