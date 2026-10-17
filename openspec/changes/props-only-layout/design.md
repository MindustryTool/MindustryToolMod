## Context

Previously, Solim components like `Column`, `Row`, `Card`, `Wrap`, and `Grid` supported chained layout methods such as `.gap(float)` and `.center()`, `.top()`, `.left()`, etc. At the same time, `Values` provided equivalent `Prop` objects (`Values.gap(...)`, `Values.center()`, `Values.top()`). This dual API resulted in code style inconsistencies, ambiguous call chaining, and required developers to mix chained calls with `.props(...)`. 

## Goals / Non-Goals

**Goals:**
- Unify all layout, spacing, and alignment configuration into `.props(...)`.
- Remove legacy chained layout methods (`.gap()`, `.top()`, `.bottom()`, `.left()`, `.right()`, `.center()`, `.align()`) from `Column`, `Row`, `Card`, `Wrap`, and `Grid`.
- Expose key layout props directly via `solim.UI` so consumers importing `static solim.UI.*` have immediate access to `gap(...)`, `center()`, `top()`, `left()`, `right()`, etc.
- Migrate all callers in `mod/` and test suites to the pure `.props(...)` model.

**Non-Goals:**
- Changing runtime layout behavior or Scene2D layout calculations.
- Removing component-specific non-layout methods (e.g. `style(...)`, `onClick(...)`, `checked(...)`, `enabled(...)`).

## Decisions

### 1. Re-export common layout Prop factories from `solim.UI`
- **Choice**: Delegate `solim.UI.gap(float)`, `solim.UI.center()`, `solim.UI.top()`, `solim.UI.left()`, etc. to `solim.modifier.Values`.
- **Rationale**: Developers already `import static solim.UI.*`. Re-exporting these factories makes `.props(grow(), center(), gap(unit(2)))` friction-free without needing extra `import static solim.modifier.Values.*` imports.

### 2. Remove chained layout methods from `Column`, `Row`, `Card`, `Wrap`, `Grid`
- **Choice**: Remove `.gap(...)` and alignment methods from component classes.
- **Rationale**: Eliminates the dual API and prevents future regression back into chained layout methods.

### 3. Keep Text component text alignment distinct
- **Choice**: Keep `Text.left()`, `Text.center()`, `Text.right()` or migrate them as well via `Values.left()` which already coordinates both label text alignment and cell alignment.
- **Rationale**: `Values.left()` already handles both container alignment and `Text` label text alignment uniformly via `alignProp`.

## Risks / Trade-offs

- **[Large call-site migration]** → `mod/` has numerous occurrences of `.gap(...)` and alignment chaining.
  - *Mitigation*: Migrate systematically by feature directory, verifying compilation at each step.
