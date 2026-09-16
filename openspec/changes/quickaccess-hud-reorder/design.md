## Context

Quick Access HUD buttons render in `FeatureManager` registration order (`QuickAccessHudView.computeVisibleItems` iterates `FeatureManager.getFeatures()`), and the settings dialog (`QuickAccessSettingsView`) lists one static checkbox row per feature built with a plain Java `for` loop. Two reorder precedents exist: the settings-grid chevrons (`FeatureCard`, swap-adjacent on `ModSettings.featureOrder`, reactive disabled boundaries) and the pretty-chat pipeline arrows (up/down buttons at each row's right end, placeholder spacers at boundaries, order in the feature's own config). The new display order must be independent of both `FeatureMetadata.order` and `ModSettings.featureOrder`.

## Goals / Non-Goals

**Goals:**
- Persist a Quick Access display order owned by `QuickAccessFeature`, surviving restarts.
- Let players move any quick-access feature one slot up/down from the settings dialog.
- Render HUD buttons in stored order; keep hidden features' slots; keep the settings button last.
- Re-render settings rows and HUD reactively on every move.

**Non-Goals:**
- No changes to settings-grid order, metadata order, or their normalization.
- No drag-and-drop; swap-adjacent only.
- No reordering of development features (they never appear in either surface).

## Decisions

### Order list owned by QuickAccessFeature config
New `ConfigValue<Seq<String>>` via `config.value("display-order", <default>, new OrderedSeqPersister())`, mirroring `ModSettings.featureOrder` but scoped to the feature group. Alternative considered: storing in `ModSettings` next to `featureOrder` — rejected in favor of feature-local ownership like pretty-chat's pipeline order.

### Swap-adjacent moves on the full list
`moveUp(id)`/`moveDown(id)` swap with the adjacent entry in the stored list (hidden features included, so slots are stable). Alternative considered: moving within the visible-only order — rejected so hidden features keep positions.

### Placeholder spacers at boundaries
First row shows a spacer instead of up-arrow, last row a spacer instead of down-arrow (pretty-chat pattern). Alternative considered: reactive disabled buttons like the settings grid — rejected for the confirmed static-spacer look.

### Lazy normalization on read
Every read heals the list (drop stale ids, append newly registered quick-access non-development ids at the end) and writes back only when changed. Alternative considered: normalizing once in the feature constructor or at dialog build — rejected so both HUD and dialog always see a healed list regardless of entry point.

### Newcomers appended at end
Missing ids go last (before the settings button, which is render-only and never stored). Alternative considered: metadata-order insertion like the settings grid — rejected for simplicity.

### Icons via new asset with fallback
Add `assets/icons/chevron-up.png`; use `FileIcon.of("chevron-up.png", Icon.up)` and existing `chevron-down.png` with `Icon.down` fallback, matching the `chevron-left/right` pattern. Alternative considered: bare `Icon.up`/`Icon.down` — rejected to complete the chevron asset set.

### Reactive rows via order signal
Settings rows render from a `ForEach` over the ordered-id list mapped to features (keyed by id), replacing the static `for` loop; HUD `items` derive from a computed combining the order signal and the hidden set. Alternative considered: rebuilding the whole dialog on each move — rejected for localized structural updates.

## Risks / Trade-offs

- [Risk] HUD grid reconciliation on reorder → Mitigation: keyed by feature id so existing buttons shuffle instead of rebuilding; toggle states survive moves.
- [Risk] Concurrent heals from HUD and dialog both writing back → Mitigation: heal is deterministic and writes only on change, so redundant writes converge.
- [Risk] Stale ids between normalization points → Mitigation: row factory skips ids with no registered feature; HUD mapping does the same.
- [Risk] New `chevron-up.png` must match existing chevron style → Mitigation: derive from the same source as `chevron-down.png` (flipped) if possible.
