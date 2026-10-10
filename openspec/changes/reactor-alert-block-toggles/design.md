## Context

`ReactorAlertFeature` detects explosive reactors with a fixed union predicate (`BlockFlag.reactor` flag OR positive `explosionRadius`) and alerts for every match within radius. Its only configuration is the alert radius. Players cannot mute individual block types, so noisy-but-familiar reactors force an all-or-nothing choice.

`RangeDisplayFeature` already establishes the project's per-block toggle pattern: per-block booleans keyed by `block.name` (default true), cached reactive signals bound to settings UI, a lazy event-time lookup, bulk All/None actions, and content scanning at dialog-build time. This design reuses that pattern rather than inventing a new one.

## Goals / Non-Goals

**Goals:**
- Let players enable/disable alert monitoring per explosive block type.
- Discover the block list dynamically so modded explosive reactors appear with no hardcoded names.
- Default unknown blocks to monitored (fail-open) so new threats warn rather than stay silent.
- Keep detection fast at event time and the settings UI reactive.

**Non-Goals:**
- Do not add a reset-to-defaults button (explicitly deferred; stale keys for uninstalled mods are accepted as inert).
- Do not change alert presentation (chat/toast), radius, cooldown, builder attribution, or the explosive predicate itself.
- Do not scope toggles per team, per map, or per game mode.

## Decisions

1. **Toggle storage: one boolean per block, keyed by `block.name`, default `true`.**
   Same shape as `RangeDisplayFeature.blockSettingKey` under the feature's config namespace. `block.name` is globally unique (modded blocks are mod-prefixed), stable across sessions, and human-readable in settings. Chosen over a single serialized set because individual keys give free per-key reactivity and survive block-list changes.

2. **Fail-open default for newly discovered blocks.**
   A warning feature must warn about unknown reactors rather than stay silent. Matches the RangeDisplay precedent and the archived spec's "cover every explosive reactor" intent.

3. **Labeled rows over icon chips in settings.**
   Icon + localized name + explosion radius + checkbox per row. The vanilla set is ~4 blocks, so readability beats the compactness of RangeDisplay's filter chips; chips only win if a mod pack adds dozens of explosive blocks. Rows also give a natural place to show the radius value that informs the mute decision.

4. **Lazy discovery, no lifecycle listeners.**
   Scan `Vars.content.blocks()` with the existing `isExplosiveReactor` predicate when the settings dialog builds; rebuild the event-time lookup on first use when absent. Chosen over world-load listeners because staleness within a session (mods toggled mid-game) is harmless for a warning toggle and the wiring stays minimal.

5. **Gate placement: alongside the predicate check.**
   Detection becomes `isExplosiveReactor(block) && isBlockEnabled(block)`, evaluated before distance/cooldown work so muted blocks cost one lookup. Presentation, radius, and cooldown paths are untouched.

6. **All/None bulk buttons, section-scoped.**
   One-tap enable/mute for the whole explosive set, mirroring RangeDisplay's per-section bulk actions.

7. **List only placeable explosive blocks.**
   Blocks players cannot build (hidden/test blocks) add noise without value; filtering keeps the section to actionable choices.

## Risks / Trade-offs

- **Mid-session staleness** (mods toggled while playing) → Accepted; list refreshes on dialog reopen and the lookup rebuilds lazily. Mitigation documented in spec scenarios.
- **Stale keys for uninstalled mods linger** → Accepted as inert bytes; no reset/prune UI in scope. Revisit if settings bloat is ever observed.
- **Modded blocks with colliding display names** → Keys use `block.name` (unique), display uses `localizedName`; collisions are display-only and rare.
- **Missing block icons** → Fall back to `fullIcon`, then a blank drawable (same fallback chain as RangeDisplay chips).

## Migration Plan

No migration. Additive settings keys defaulting to monitored preserve current behavior exactly for existing users (every block behaves as enabled until muted). Disabling the feature still disables all alerts.

## Open Questions

- None blocking. All UX decisions (rows, fail-open, lazy scan, All/None without reset) were confirmed during exploration.
