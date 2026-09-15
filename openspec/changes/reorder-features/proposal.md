## Why

Feature cards in the settings dialog render in hardcoded metadata order, so players cannot arrange features by personal priority and existing duplicate `order(...)` values resolve by registration luck.

## What Changes

- Persist a user-defined feature order as an ordered-ID list in a `ConfigValue<Seq<String>>` JSON entry owned by `ModSettings`.
- Render the settings grid from the persisted list; chevron-left/right buttons on each non-development `FeatureCard` swap that card with its global-list neighbor and persist the result.
- Pin development features absolutely last regardless of persisted order; hide their chevrons and sort the dev block deterministically by feature id.
- Normalize the persisted list on load (dedupe keeping first occurrence, drop stale and development ids, append missing non-dev ids sorted by metadata order then id, persist back only when dirty); corrupt payloads reset to metadata order.
- Disable chevrons while a search filter is active and at the global-list boundaries (first item cannot move left, last non-dev item cannot move right).
- Wrap the card `statusText` in a row with a spacer pushing the chevrons to the most-right position; keep card toggle, divider accent, and existing buttons unchanged.
- Add localized labels/tooltips for the move-left/move-right buttons with translator comments; no hardcoded user-visible text.

## Capabilities

### New Capabilities

- `feature-order`: user-defined ordering of features via persisted ordered-ID list, swap-based chevron reordering, load-time normalization, dev pinning, and card UI.

### Modified Capabilities

- `app-settings`: feature registry ordering requirement changes from pure metadata order to persisted ordered-ID list with development pinning (registry contains the same features; display order follows the new rule).

## Impact

- `ModSettings` gains a new persisted `ConfigValue<Seq<String>>` entry with a custom ordered JSON persister (existing `STRING_SET` is unordered and cannot be reused).
- `FeatureManager` ordering/normalization logic and its reactive feature sequence; `FeatureSettingsView` grid derivation must observe both filter and order.
- `FeatureCard` bottom-row layout and new bundle keys.
- Scope is the settings grid only; QuickAccess HUD order is unchanged.
