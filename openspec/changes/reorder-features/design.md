## Context

`FeatureManager` sorts features once at `register()` time by `FeatureMetadata.getOrder()` (development forced to `Integer.MAX_VALUE`). `FeatureSettingsView` derives its grid from `filter.map(...)` reading `FeatureManager.getFeatures()` via a non-reactive peek, so order changes cannot propagate today. `FeatureCard` renders `statusText` as a lone growing text plus a divider accent; the whole card toggles enable on click. `ConfigPersister.STRING_SET` round-trips through `HashSet` and cannot preserve order. Autoplay task-order (`Core.settings.getJson/putJson` with `Seq`) is the in-repo precedent for ordered persistence. Chevron assets (`chevron-left.png`, `chevron-right.png`) exist with the `FileIcon.of(name, fallback)` pattern, and `BrowserFooter` establishes the prev/next button idiom (tooltip + `.enabled(...)`).

## Goals / Non-Goals

**Goals:**

- User-reorderable feature cards with persisted order across sessions.
- Duplicate orders made impossible by construction after a one-time load reconciliation.
- Development features unconditionally last with deterministic sub-order.
- Minimal, honest chevron UX (boundaries + filtered view handled explicitly).

**Non-Goals:**

- QuickAccess HUD order (unchanged; settings grid only).
- Drag-and-drop reordering (chevrons only).
- Per-feature order values or settings-dialog order controls.
- Reordering development features.

## Decisions

### 1. Ordered-ID list persisted as `ConfigValue<Seq<String>>` JSON in `ModSettings` (user-decided)

Position in the list is the order. A custom ordered JSON persister (`getJson`/`putJson` with `Seq`, following the autoplay precedent) backs a new `ModSettings` entry, because `ConfigGroup.setValue`/`STRING_SET` uses `HashSet` and destroys order.

Alternatives considered: per-feature `intValue("order", ...)` (order is cross-feature state, fan-out reads, duplicate class of bugs remains); central per-id int registry (same duplicate problem); comma-joined `String` preserving first-seen order (ad-hoc parsing, no precedent — rejected by user).

### 2. ABSOLUTE PIN for development features (user-decided)

Development ids are never persisted; normalize strips them if present. The dev block always renders after all non-dev features, sorted deterministically by feature id. Chevrons are hidden on dev cards.

Alternatives considered: grouped sort honoring persisted order within the dev block (rejected — persisted order ignored for dev); disabled chevrons with tooltip (rejected — hidden is more honest).

### 3. Swap with global-list neighbors (user-decided)

Chevron-left swaps the feature one position earlier in the global ordered list; chevron-right swaps one position later. A deduped list cannot gain duplicates through swaps. Boundaries disable: the first item cannot move left, the last non-dev item cannot move right (follows the `BrowserFooter` `.enabled(...)` precedent).

Alternatives considered: decrement/increment numeric order (meaningless for a list; gaps/dupes ambiguous); visible-neighbor swap under filter (silently reorders hidden items — rejected).

### 4. Normalize on load in `FeatureManager.init`, corrupt resets, grid-only scope (user-decided)

After all `register()` calls, reconcile: dedupe keeping first occurrence, drop stale (unregistered) ids, drop development ids, append missing non-dev ids sorted by `(metadata.order, id)`, then re-sort the reactive feature sequence (non-dev in list order, dev block by id after). Persist back only when the reconciled list differs from storage. Unparseable/corrupt payloads fall back to pure `(metadata.order, id)` default order and overwrite the key. Scope is the settings grid; HUD untouched.

Alternatives considered: lazy normalize on first dialog open (rejected — order should be canonical before any render); silent in-memory fallback without persisting (rejected — storage stays corrupt).

### 5. Card bottom row: status text left, spacer, chevrons most-right (user-decided)

`statusText` is wrapped in a row: text left-aligned, `spacer()`, chevron-left button, chevron-right button. Divider accent below the row is unchanged. Chevron clicks must not propagate to the card toggle (same consumption mechanism as the existing header shortcut buttons). New bundle keys for labels/tooltips with translator comments; no hardcoded text.

### 6. Reactive propagation prerequisite

`filteredFeatures` must observe both the search filter and the ordered feature sequence (currently `filter.map(...)` with a non-reactive `getFeatures()` peek, so order writes would not refresh the grid). The derivation becomes a combined computed over both signals; the grid stays keyed by feature id so reorders reconcile by move, not rebuild.

## Risks / Trade-offs

- [Risk] Late `register()` after `init()` misses normalization → Mitigation: render path treats unknown ids as append-last defensively.
- [Risk] `ConfigValue` signal persists on every write; rapid chevron clicks cause rapid `Core.settings` writes → Mitigation: swaps are discrete single writes; no debounce introduced (accepted).
- [Risk] Grid keyed reconciliation assumed to move rather than rebuild on reorder → Mitigation: verify during implementation; keys are stable ids.
- [Risk] Global-swap under filter would confuse (neighbor invisible) → Mitigation: chevrons disabled while search query is non-empty (user-decided).
- [Risk] Eager persist-on-boot when dirty rewrites the key → Mitigation: write only when reconciled list differs; accepted as canonical-storage behavior.
