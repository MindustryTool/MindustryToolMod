## Context

Solim's reactive container components (`When`, `Dynamic`, `ForEach`, `QueryView`) encapsulate dynamically rendered elements inside an internal Arc `Table container`. They implement `CellConfig` and `TableConfig`.

Currently:
1. `TableConfig` default methods (`center()`, `top()`, `bottom()`, `left()`, `right()`, `align(int)`) only affect the internal table content alignment (`t.center()`), but do NOT set `constraints.align` in `PendingCellConfig`, and do NOT call `cellConfig().applyAlignToParentCell(element())`.
2. When the reactive container mounts inside a parent layout (such as `Column` or `Row`), the parent cell alignment is controlled by `PendingCellConfig`. Since `constraints.align` was never set by `center()`, the parent cell defaults to top-left.
3. Furthermore, internal child default alignment (`container.defaults()`) is not aligned with container modifiers, and when `rebuild()` calls `applyContainerAlign()`, it resets the container to `top()` unless `constraints.align` was populated.

## Goals / Non-Goals

**Goals:**
- Provide full alignment synchronization on `When`, `Dynamic`, `ForEach`, and `QueryView`:
  - Calling alignment modifiers (`center()`, `top()`, `bottom()`, `left()`, `right()`, `align(int)`) sets `constraints.align` via `cellConfig()`.
  - Propagates alignment to the parent cell immediately if already mounted (`applyAlignToParentCell(element())`).
  - Configures `container.align(...)` and `container.defaults().align(...)`.
  - Re-aligns existing child cells in `container.getCells()`.
  - Preserves alignment across rebuilds and branch/collection changes in `applyContainerAlign()`.
- Add test coverage verifying both outer cell alignment and inner element alignment.

**Non-Goals:**
- Modifying static layouts (`Column`, `Row`, `Card`) which already implement alignment propagation.
- Altering default alignment when no modifier is called (defaults remain `top().left()`).

## Decisions

### Decision 1: Override alignment methods on reactive wrappers directly
- **Rationale**: `When`, `Dynamic`, `ForEach`, and `QueryView` have internal containers and can be attached to parent tables. Overriding `align(int)`, `center()`, `top()`, `bottom()`, `left()`, `right()` directly ensures fluent chaining works as expected:
  ```java
  when(cond).center().thenDo(...);
  ```
- **Alternative considered**: Implementing alignment inside `TableConfig` default methods. Rejected because `TableConfig` does not assume `this` has a parent cell or `PendingCellConfig`.

### Decision 2: Update existing child cells and container defaults
- **Rationale**: Setting `table.defaults().align(a)` affects subsequent items, and iterating `container.getCells()` updates already-mounted items immediately if alignment is called post-build.

### Decision 3: Update `applyContainerAlign()`
- **Rationale**: `applyContainerAlign()` should apply `constraints.align` if present, without reverting to hardcoded `top()` when a custom alignment was requested.

## Risks / Trade-offs

- **[Risk]** Existing UI relying on `when().center()` having only partial effect.
  - **Mitigation**: In Solim, calling `.center()` expresses the intent to center. The previous behavior of remaining top-left aligned was a bug.
