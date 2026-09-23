## Context

Solim builds UI declaratively: containers bracket construction via `ParentStack.push/pop` (`solim-runtime`), leaves build via `BaseComponent.element()` (`solim-core`), and reactive updates flow through `SignalDispatcher` per frame. Today's diagnostics are flat and gated: `DebugParentStack` records only structural spans slower than a threshold into a 256-entry ring of `PerfSpan{component, phase, durationMs, timestamp, detail}`, exposed via `Perf.snapshot()`. `UiProfiler` aggregates frame-level act/layout/draw timings. There is no nested trace, no leaf timing, and no flame-capable export. Module boundaries are strict: `mod` sees only `:solim` public facades, `:solim-runtime` internals stay hidden, Java 8 runtime APIs only, zero cost when profiling is disabled (singleton base/debug swap).

## Goals / Non-Goals

**Goals:**
- Hierarchical build-lane traces: container subtrees plus leaf builds, with parent linkage and nanosecond start/end.
- Bounded rolling capture (4096 spans) active only while tracing is enabled; base hot path unchanged.
- Public control via `Perf` facade; headless self-contained HTML export with dual-format payloads.
- Export-time pruning (`minMs`) so record stays complete and filtering stays cheap.

**Non-Goals:**
- Reactive-lane (effect dispatch) and frame-lane (per-element layout/draw) traces — deferred to follow-ups; design reserves `phase`/`lane` field for them.
- In-game interactive flame viewer — deferred; HTML export is the v1 viewer.
- New MCP tool contract — out of scope; exporter output is file-based and viewer-compatible.
- Changes to `PerfSpan`, slow-span behavior, `UiProfiler` aggregates, or `ParentStack` base logic.

## Decisions

### Decision: New `TraceParentStack` debug variant reusing singleton-swap isolation
`DebugParentStack` owns threshold-gated slow spans; flame capture needs full nesting with no threshold. A second debug variant keeps concerns separate: base `ParentStack` stays branch-free, slow tracking stays untouched, trace state (open-span stack, ring buffer, trace generation) lives only in the trace variant. `Perf` gains trace control that installs/reverts the variant, mirroring `setEnabled(boolean)`.
- Alternative considered: extend `DebugParentStack` with a trace mode flag — rejected because it mixes gated and ungated lifecycles and grows the hot path.

### Decision: New `TraceSpan` value type, `PerfSpan` untouched
Flame needs `traceId/spanId/parentId/startNs/endNs/depth/childCount`; retrofitting `PerfSpan` would break the `slow-component-tracking` contract and its stringly-typed `detail`. New immutable type in `solim-api` keeps compat and lets export compute durations and self-time from nanoseconds.
- Alternative considered: extend `PerfSpan` — rejected for compat and precision (float ms loses sub-ms card timing).

### Decision: Leaf timing via `ComponentContext`-adjacent hook, not `ParentStack` alone
Containers push `Table`s; leaves (labels, cards without sub-tables) never touch `ParentStack`, so container-only timing leaves hollow middles. Timing `BaseComponent.element()` build brackets every component exactly once (lazy single-build semantics preserved). Hook emits leaf spans into the active trace variant if present, else no-op.
- Alternative considered: containers only — rejected after browser page-change analysis showed card cost would vanish.

### Decision: `isolate`/`capture` save/restore open-span stack in lockstep
`Dynamic`/`reactiveGrid` build inside isolated contexts. Without lockstep save/restore (the pattern `DebugParentStack` already uses for `pushTimes`), parent chains corrupt across isolation boundaries. Trace variant overrides the same mutators.
- Alternative considered: global parent pointer without isolation handling — rejected, breaks keyed reconciliation paths.

### Decision: Headless HTML export with dual embedded payloads
Single self-contained HTML file embeds both Chrome Trace Events (`traceEvents` with `ph:"X", ts, dur, pid/tid`) and Speedscope-native profile, generated from one ordered span list. Headless keeps zero game-UI cost and works in CI/bug reports; dual format avoids betting on one viewer. `minMs` filtering and self-time (`total - children`) computed at export, keeping record cheap.
- Alternative considered: MCP `get_flame_graph` tool first — deferred; file export satisfies the chosen delivery without expanding the MCP read-only contract. Alternative in-game dialog — rejected for v1 jank risk.

### Decision: Rolling 4096-span ring while enabled, gated by flag
Always-on-while-enabled with a 4096 ring (~256KB) covers large browser pages without unbounded growth. Gating by the existing enable flag preserves the negligible-overhead-when-disabled requirement. No sampling in v1; completeness beats cleverness for attribution.
- Alternative considered: threshold floor at record — rejected, hollows flames. Alternative unbounded list — rejected on Android GC grounds.

## Risks / Trade-offs

- [Risk] Leaf hook touches core lifecycle (`BaseComponent.element`) → Mitigation: hook is a two-line delegate to the trace singleton, no-op when disabled; covered by lifecycle regression tests.
- [Risk] Auto-names (`solim-column-table`) make poor flame labels → Mitigation: exporter prefers explicit `name("...")`, falls back to `Class + element` pair; naming discipline documented as follow-up, not blocker.
- [Risk] 4096-ring drops oldest spans on huge pages → Mitigation: exporter reports `droppedCount`; trace generation id lets viewers distinguish windows; sizing is tunable via facade.
- [Risk] `nanoTime` skew across isolate boundaries → Mitigation: single clock source, monotonic deltas only; no wall-clock mixing except export timestamp.
- [Risk] Confusion with slow-span buffer (two buffers, two lifecycles) → Mitigation: `Perf` facade names separate them (`snapshot` vs `traceSnapshot`); docs state slow list is gated/recency-ordered, trace is full/document-ordered.
