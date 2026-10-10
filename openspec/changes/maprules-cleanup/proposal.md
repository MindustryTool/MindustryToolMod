## Why

The Map Rules feature (`mindustrytool.features.maprules`) was built fast across several iterations and accumulated small defects that hurt readability and maintainability: invisible NUL bytes in `MapRulesView.java` (the file no longer opens in text tooling), a confused two-pass scan in ban search, and dead code (`sync()`, `cancelPending()`, a no-op `retainContentFields` call). Fixing these now keeps the follow-up descriptor-table rework on clean ground.

## What Changes

- Replace the invisible NUL separators in `MapRulesView.viewKey()` with visible delimiters.
- Collapse `bannedBlocksGrid` search to a single pass over content (drop the hidden-then-all two-scan and the 60-item early-cap interaction).
- Delete dead code: `RuleMutator.sync()`, `RuleMutator.cancelPending()`, and the `retainContentFields` call in `MapRulesFeature.captureSnapshot()`.
- No user-visible behavior changes; dialog, filtering, editing, saving, and reset flows must behave identically before and after.

## Capabilities

### New Capabilities

- `maprules-cleanup`: Behavior-preservation contract for the cleanup — search results, UI text, editing/saving/reset semantics, and verification gates must remain identical. Creates `specs/maprules-cleanup/spec.md`.

### Modified Capabilities

<!-- None: no existing spec-level requirements change. -->

## Impact

- **Code**: `mod/src/mindustrytool/features/maprules/MapRulesView.java`, `RuleMutator.java`, `MapRulesFeature.java` only.
- **Bundles/locales**: untouched (no user-visible text changes).
- **Follow-up**: unblocks the `maprules-descriptors` structural rework.
