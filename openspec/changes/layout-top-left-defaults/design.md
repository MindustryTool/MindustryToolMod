## Context

Solim layout containers wrap Arc `Table`. Arc has two alignment levels: (1) the table's own alignment inside its parent cell (`table.left()`), and (2) children's alignment inside the table (`table.defaults()` + per-cell `cell.align()`). `Row()` sets only (1) via `table.left()` and `Column()` sets only (1) via `table.top().left()`, so (2) falls back to Arc's built-in center. Explicit `.left()`/`.top()` overrides fix both levels (they set `defaults()` plus loop existing cells), which is why manual `.top().left()` works around the bug. `Scroll` is the only container that already defaults correctly: its attacher does `cell.top().left()` per child. `Card` and `Grid`/`ReactiveGrid` share the `Row`/`Column` bug. `solim.layout.Container` has zero production usages and `solim.layout.Justify` has one (`SettingsPanel` `justify(END)`); `Row.justify()` maps `START/CENTER/END` to `left()/center()/right()` and no-ops `BETWEEN/AROUND/EVENLY`.

## Goals / Non-Goals

**Goals:**
- All list/form/content containers default children to top-left with no per-callsite workaround.
- Delete `Container` and `Justify` so there is one obvious way to align (`top()/left()/center()/right()`).
- Keep explicit `.center()` working exactly as today for loaders, dialogs, and empty states.
- Preserve gap, grow, padding, and reactive binding behavior untouched.

**Non-Goals:**
- No change to `Align` (`START/CENTER/END/STRETCH`) or `Row.align()`/`Column.align()` mappings.
- No change to overlay/structural components (`SolimStack`, `Tabs`, `ForEach`, `Dynamic`) except where they document container alignment.
- No visual redesign beyond the default flip plus targeted `.center()` re-adds.

## Decisions

- **Default mechanism: ctor `defaults().top().left()` plus attacher `cell.top().left()` (both, not one).** Rationale: `defaults()` covers `table.add()` paths including direct `add(Element)` and tests; per-cell covers `ParentStack` attach paths and survives later `defaults()` mutation. Alternative of only one level was rejected because each level has a path the other misses. Precedent is `Scroll`'s per-cell approach extended with defaults.
- **Defaults-only, no alignment state on containers (follow-up).** An earlier revision forced `cell.top().left()` in the `Row`/`Column` attachers guarded by an `alignConfigured` flag, but explore review showed both are redundant: every attached cell comes from `table.add(child)` and already inherits `defaults()`, which the five overrides maintain (explicit config flows through defaults to future cells, matching the existing `Wrap` pattern that never had a flag). The flag and the force were deleted from `Row`/`Column` with zero test delta. `Card`/`Grid` attachers keep their stateless one-line force (no overrides exist to conflict with). `cardCenteringInsideColumn` exposed that `Card.center()` never self-centered (it relied on the old implicit parent default); the test now expresses centering explicitly via the card's own cell config, matching what `Column.center()` does internally.
- **Scope: `Row`, `Column`, `Card` (inner container), `Grid`, `ReactiveGrid`.** Rationale: these are the linear/grid content containers with the identical `table.add(child)` attacher shape. `Scroll` already correct, left unchanged as reference. `Button` (horizontal button internals), `Wrap` (flow with `padRight`/`padBottom` gap model), `SolimStack`/`Tabs` excluded from the default change to avoid altering overlay/flow semantics unintentionally.
- **Explicit overrides keep current override shape.** `.top()/.left()/.center()/.right()/.bottom()` continue to set table align plus `defaults()` plus loop existing cells (plus `cellConfig().alignCenter()` on `Column.center()`), so pre-existing explicit callsites behave identically.
- **`Justify` removal via direct substitution.** `justify(START)` becomes `left()`, `justify(CENTER)` becomes `center()`, `justify(END)` becomes `right()` (the `SettingsPanel` case). `BETWEEN/AROUND/EVENLY` have no substitution because they never took effect. Alternative of deprecating instead of deleting was rejected: one callsite makes a flag day cheaper than a deprecation cycle.
- **`Container` removal via deletion, no replacement type.** `Ui.container()` already returns `column()` so mod code is unaffected; only `solim-core` tests reference the class. Alternative of keeping it as a deprecated alias was rejected per project rule against dead abstractions.
- **Audit strategy: bare-callsite triage, not blanket `.center()`.** Bare `row()`/`column()` (no alignment modifier before `.children()`) split into intentional-center (dialogs, loaders, empty states get `.center()` added) versus content (left as new top-left). Screenshot verification via MCP for dialogs and browser views, since unit tests assert cells/padding but not visual intent.

## Risks / Trade-offs

- [Risk] Bare content layouts that accidentally looked acceptable centered now shift left/top → Mitigation: that shift is the intended fix; triage list separates them from intentional-center cases.
- [Risk] Missed intentional-center dialog renders off-center → Mitigation: audit explicitly searches `Dialog`, `Loader`, empty-state, and `scroll().grow().children(column()...)` patterns; MCP screenshot pass before merge.
- [Risk] Third-party/out-of-repo import of `Container` or `Justify` breaks on upgrade → Mitigation: accept as **BREAKING**; both were undocumented in `Ui` facade and unused in `mod/`.
- [Risk] `defaults().top().left()` interacts with later user `defaults()` mutation → Mitigation: per-cell attacher alignment is applied at attach time after defaults resolve, so attach-time intent wins.
