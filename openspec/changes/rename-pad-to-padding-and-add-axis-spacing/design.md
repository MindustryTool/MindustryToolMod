## Context

In Solim UI, spacing methods have evolved organically, resulting in some inconsistencies:
1. `ElementModifiers`, `Column`, and `Row` expose abbreviated `pad()` methods alongside full `padding()` methods. Other components only support `padding()`.
2. Configuring two-axis spacing (horizontal: left + right, or vertical: top + bottom) currently requires either 4-parameter calls or multiple chained calls (e.g. `.paddingLeft(x).paddingRight(x)` or `.margin(y, 0, y, 0)`).
3. The user explicitly requested to migrate all `pad()` occurrences to `padding()`, drop backwards compatibility for `pad*`, and introduce `paddingX`, `paddingY`, `marginX`, and `marginY`.

## Goals / Non-Goals

**Goals:**
- Eliminate all `pad()`, `padTop()`, `padBottom()`, `padLeft()`, and `padRight()` methods from Solim, establishing `padding*` as the sole canonical nomenclature.
- Add `paddingX(float x)` and `paddingY(float y)` to `ElementModifiers`, `Column`, `Row`, `Text`, `SolimImage`, `NetworkImage`, `Container`, and `Card`.
- Add `marginX(float x)`, `marginY(float y)`, `marginX(Readable<Float> x)`, and `marginY(Readable<Float> y)` to `LayoutModifiers` so all layout components automatically support two-axis outer margin constraints.
- Add `marginX` and `marginY` to `ElementModifiers`, `Column`, `Row`, `Text`, `SolimImage`, `NetworkImage`, and `Button`.
- Migrate all existing usages and tests in `mod` and `solim-core` to the new methods.

**Non-Goals:**
- Retaining deprecated `pad` aliases (per user directive, complete migration without backwards compatibility).
- Altering Arc's underlying `Cell.pad` / `Table.margin` mechanics.

## Decisions

### Decision 1: Complete removal of `pad*` in favor of `padding*`
- **Choice**: Remove `pad()`, `padTop()`, `padBottom()`, `padLeft()`, `padRight()` from `ElementModifiers`, `Column`, and `Row`.
- **Alternatives Considered**: Deprecating `@Deprecated` with forwarding. Rejected per user prompt: clean break with no backwards compatibility.
- **Rationale**: Eliminates confusion between `pad` and `padding` and enforces uniform API naming across the entire framework.

### Decision 2: Axis Spacing Semantics (`paddingX`, `paddingY`, `marginX`, `marginY`)
- **Horizontal Axis (`*X`)**: Symmetrically sets both left and right spacing to the given value.
- **Vertical Axis (`*Y`)**: Symmetrically sets both top and bottom spacing to the given value.
- **LayoutModifiers (`marginX`, `marginY`)**:
  - `marginX(float x)` / `marginX(Readable<Float> x)`: Sets `padLeft` and `padRight` on `SizeConstraints` and applies to the parent cell.
  - `marginY(float y)` / `marginY(Readable<Float> y)`: Sets `padTop` and `padBottom` on `SizeConstraints` and applies to the parent cell.
- **ElementModifiers**:
  - `paddingX(Table, x)` / `paddingY(Table, y)`: Configures table margins horizontally / vertically.
  - `paddingX(Element, x)` / `paddingY(Element, y)`: Delegates to table margin if `Table`, or sets cell `padLeft`/`padRight` or `padTop`/`padBottom` if inside a parent cell.
  - `marginX(Table, x)` / `marginY(Table, y)` and `marginX(Element, x)` / `marginY(Element, y)`: Analogous helpers for margin consistency.
- **Component-Level Delegations**:
  - `Column`, `Row`: Forward `paddingX`/`paddingY` to `ElementModifiers.paddingX`/`paddingY` and `marginX`/`marginY` to `ElementModifiers.marginX`/`marginY`.
  - `Text`, `SolimImage`, `NetworkImage`: Set internal `padLeft = padRight = x` / `padTop = padBottom = y` and `marginLeft = marginRight = x` / `marginTop = marginBottom = y`, then call `applySpacing()`.
  - `Container`, `Card`: Expose `paddingX`/`paddingY` for inner content padding.
  - `Button`: Expose `marginX`/`marginY` (static float and reactive `Readable<Float>`).

## Risks / Trade-offs

- **[Risk: Breaking existing calls in mod and tests]** →
  - **Mitigation**: Grep and update all `.pad(` invocations in active `mod` code (`AuthOverlay.java`) and Solim test classes (`GapCoexistenceTest.java`, `DynamicComponentTest.java`, `DisplayTest.java`, etc.).
