## Why

In Solim UI, reactive container components (`When`, `Dynamic`, `ForEach`, `QueryView`) wrap dynamic DOM subtrees in an internal Arc `Table`. While they implement `TableConfig` and `CellConfig`, invoking alignment modifier methods such as `.center()`, `.top()`, `.bottom()`, `.left()`, and `.right()` only delegates to `TableConfig.super.*` on the internal table, without:
1. Configuring parent cell alignment via `cellConfig().align(...)` and `applyAlignToParentCell(...)`.
2. Updating internal container defaults (`container.defaults().align(...)`) or updating already mounted child cells.
3. Preventing `applyContainerAlign()` from unconditionally resetting the internal container to `top()` whenever `constraints.align` remains unset.

As a result, chaining `.center()` on `when(...)` (and other reactive containers) fails to center the component inside its parent cell and fails to center children inside the reactive container.

## What Changes

- Update `When`, `Dynamic`, `ForEach`, and `QueryView` to explicitly override alignment methods (`align(int)`, `center()`, `top()`, `bottom()`, `left()`, `right()`) from `TableConfig`.
- Propagate alignment to:
  - Parent layout cell via `cellConfig().align*()` and `cellConfig().applyAlignToParentCell(element())`.
  - The internal container table via `table.align(...)` and `table.defaults().align(...)`.
  - Any currently mounted child cells inside the container.
- Ensure `applyContainerAlign()` in these reactive components honors `constraints.align` and correctly reflects the configured alignment across rebuilds and branch switches.
- Add unit tests verifying alignment propagation for both the outer cell and inner child elements across lifecycle transitions.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `solim-layout`: Add requirements for reactive container components (`When`, `Dynamic`, `ForEach`, `QueryView`) to propagate alignment modifiers to both parent cell constraints and container default child alignment.

## Impact

- Affects `solim-core` reactive components: `solim.reactive.When`, `solim.reactive.Dynamic`, `solim.reactive.ForEach`, `solim.reactive.QueryView`.
- Backward-compatible: existing layouts that did not call alignment modifiers retain their existing behavior (`top().left()` defaults). Code calling `.center()` will now center properly.
