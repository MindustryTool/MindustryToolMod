## Context

WavePreview is a development placeholder (metadata only: `wave-preview`, swords icon, order 1, `quickAccess(true)`, `development(true)`). The `old/` tree holds a complete imperative implementation: a `Table` injected into vanilla `waves/editor` → `waves`, rebuilt every 60 ticks and on `WorldLoadEvent`, computing per-type counts from `rules.spawns` via `getSpawned(wave - 1)` with campaign difficulty scaling, sorted by unit health in a 3-per-row grid with opacity/scale from raw settings keys and no settings dialog. The rewrite program's established pattern (TeamResource, TimeControl) favors standalone draggable `Hud` overlays with `ConfigGroup` settings; the user instead decided to keep the old injected placement. All decisions below were made by the user during exploration; unresolved points sit under Open Questions.

## Goals / Non-Goals

**Goals:**

- Full old parity as a real enable-capable Solim feature, enabled by default.
- Same injected placement with defensive node handling.
- Event-driven refresh with no polling loop.
- Domain-split units (ground/air/naval, ground fallback) in wave-major layout.
- Lookahead-depth setting (default 1, max 5) and a minimal settings dialog (opacity, scale, depth).

**Non-Goals:**

- Threat synthesis or scoring beyond domain grouping and counts.
- Standalone/draggable HUD presentation and position persistence.
- Changes to spawn, difficulty, or wave-progression logic.
- Touching `old/` sources.

## Decisions

All decisions in this section are user decisions gathered before proposal writing, with the rejected alternatives noted.

### 1. Re-inject into the vanilla waves panel (user-decided)

The panel renders inside vanilla `waves/editor` → `waves` exactly as before, with defensive null handling and error logging when nodes are absent. Rejected: standalone draggable `Hud`; standalone HUD with docked-default position near the waves panel.

### 2. Event-driven refresh, no polling (user-decided)

Recompute on world load and wave-advance events only; the 60-tick `Interval` loop is removed. Rejected: keeping tick polling; events plus a slower safety poll.

### 3. Enabled by default (user-decided)

Matches the old default so updaters see the panel immediately. Rejected: disabled-by-default opt-in.

### 4. Minimal settings dialog with opacity, scale, and depth (user-decided)

A settings dialog exposes opacity, scale, and lookahead-depth rows bound to persisted config entries; no position reset (injected panels have no position). Rejected: no dialog with config-only opacity/scale.

### 5. Strict port plus domains and lookahead (user-decided)

Beyond parity: units grouped ground/air/naval with unmatched units in the ground bucket, wave-major layout (per-wave section, domain rows inside), and a persisted lookahead-depth setting defaulting to 1 with a ceiling of 5. Rejected: strict port only; port plus lookahead alone; port plus richer threat info.

## Risks / Trade-offs

- [Risk] Vanilla `waves/editor` node names may change across game versions, silently dropping the panel → Mitigation: defensive lookup with error logging (user-decided); no fallback surface specified (see Open Questions).
- [Risk] The wave-advance event name and `getSpawned` index posture are unverified assumptions inherited from old code → Mitigation: explicit verification spikes before porting the formula.
- [Risk] Event-only refresh can go stale if an advance path fires no event → Mitigation: none selected by the user (safety poll explicitly rejected); staleness would surface as frozen counts.
- [Risk] Injected content inside a vanilla-owned table fights the retained-tree model on vanilla rebuilds → Mitigation: re-attach on world load and on enable; behavior on vanilla panel rebuilds otherwise unspecified.
- [Risk] Three concurrent edits (`reorder-features`, `popup-instead-of-hud`, this change) touch the same stub-registry/count lines → Mitigation: sequence merges at apply time; this change's delta is written against the main spec.

## Open Questions

The following were not decided by the user and must be resolved before or during implementation (user explicitly reserved all decisions):

- Config key names and `ConfigGroup` placement for opacity, scale, and lookahead-depth entries.
- Exact panel metrics (icon sizes, gaps, rows per line) and bundle key names for new labels including domain headers and depth control.
- Treatment of empty domain rows (hidden vs placeholder) and of waves with zero iliads in lookahead range.
- Exact above/below ordering of domain rows and multi-wave sections beyond the wave-major decision.
- Reactive wiring shape for injected Solim content inside the vanilla table (ownership and disposal on detach).
- Fallback surface (if any) when vanilla wave nodes are absent.
