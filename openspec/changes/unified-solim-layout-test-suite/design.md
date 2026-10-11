## Context

Solim uses Arc's retained widget tree with declarative wrappers. Alignment and spacing operate at two levels:
1. Inner container level: `Table.margin()` for internal padding, `Table.align()` for inner alignment, and `Table.defaults()` for child default alignment.
2. Parent cell level: `PendingCellConfig` applying `Cell.pad()` (combining `margin` and `gap`) and `Cell.align()` (outer alignment within parent table cell).

Existing tests (such as `AxisSpacingTest`, `LayoutTest`, and `SolimStackTest`) test isolated pieces of spacing or specific layout components, but lack comprehensive cross-component coverage for every combination of alignment bitmasks (especially compound alignments like `top().center()`, `bottom().right()`, and overrides), as well as verification that `gap()`, `margin()`, and `padding()` do not collide when composed together statically or dynamically via signals.

## Goals / Non-Goals

**Goals:**
- Provide a unified test suite in `solim-core/src/test/java/solim/layout/` verifying:
  - Alignment bitmasks and overrides for all components (`top`, `bottom`, `left`, `right`, `center`, and their compositions).
  - Proper separation and synergy between inner `padding()`, outer `margin()`, and sibling `gap()`.
  - Geometric layout correctness: element coordinates `(x, y, width, height)` after `pack()` / `layout()`.
  - Dynamic updates via reactive signals (`Signal<Float>`) and `SignalDispatcher.flush()`.
- Cover all four component categories:
  - Containers (`Column`, `Row`, `Card`, `Grid`, `Wrap`, `SolimCollapser`, `Scroll`)
  - Reactive Containers (`When`, `Dynamic`, `ForEach`, `QueryView`)
  - Interactive Controls (`Button`, `Checkbox`, `Switch`, `SolimSelect`, `SolimSlider`, `SolimTextField`)
  - Display Primitives (`Text`, `SolimImage`, `NetworkImage`)

**Non-Goals:**
- Modifying production layout code unless regressions or bugs are uncovered during test development.
- Testing non-layout behaviors (such as event firing, click propagation, text styling).

## Decisions

### Decision 1: Hybrid testing model (property assertions + layout geometry)
- **Rationale**: Simply checking bitmask flags or cell pad variables doesn't guarantee that Arc actually computes the right coordinates. Conversely, checking only coordinates makes test failures opaque. A hybrid model asserts configuration properties first, then calls `pack()` on parent tables and asserts `x`, `y`, `width`, `height`.
- **Alternatives considered**:
  - Property assertions only: Faster, but could miss layout bugs in Arc/Solim interaction.
  - Geometry only: Brittle to small floating point rounding or font metrics; hard to diagnose why an element misaligned.

### Decision 2: Structured test files by concern rather than monolithic single file
- **Rationale**: Organizing into 4 targeted test classes:
  1. `UnifiedComponentAlignmentTest.java`: Bitmask and composition matrix.
  2. `UnifiedSpacingCompositionTest.java`: `gap` vs `margin` vs `padding` isolation and combination.
  3. `UnifiedLayoutGeometryTest.java`: Coordinate and bounding box geometry under `pack()`.
  4. `ReactiveSpacingAlignmentTest.java`: Signal reactivity, `SignalDispatcher.flush()`, and disposal.
  Provides fast targeted execution and clear failure isolation.

### Decision 3: Shared component factory / provider for parameterized testing
- **Rationale**: Using JUnit `@ParameterizedTest` or helper test fixtures that instantiate each component category ensures all components adhere to the same contract without duplicating 50 lines of test code per component.

## Risks / Trade-offs

- [Risk] Layout pack tests need an active Arc Scene.
  → Mitigation: Extend `SolimEnv` and use `ArcTestEnv.newScene()` with `installMinimalStyles` to provide a mock graphics/font context.
- [Risk] Floating point inaccuracies in coordinate checks.
  → Mitigation: Use `assertEquals(expected, actual, 0.01f)` tolerance on all position and dimension checks.
- [Risk] Uncovered bugs in existing components during test creation.
  → Mitigation: If an existing component fails an alignment or spacing contract, fix the defect in `solim-core` to fulfill the contract.
