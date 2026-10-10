## Context

`mindustrytool.features.maprules` grew iteratively (initial feature, 37-rule expansion, save section, debounce, optimistic `/js` sync). Three pieces of debris were identified during a code-quality review, all behavior-neutral to remove. This design records exactly what goes and what replaces it so the follow-up `maprules-descriptors` rework starts clean.

## Goals / Non-Goals

**Goals:**
- Eliminate invisible characters from `MapRulesView.java` so the file opens in all text tooling.
- Make ban search a single, obviously-correct pass.
- Remove dead code whose presence misleads readers (`sync()` implies a broadcast path that `mutate` already owns).

**Non-Goals:**
- No structural refactoring (that's `maprules-descriptors`).
- No behavior, UI, or bundle changes of any kind.
- No new capabilities or spec changes.

## Decisions

### Decision 1: Visible delimiter in `viewKey()`
- **Choice**: Join the composite key with a visible delimiter (e.g. `"|"`).
- **Rationale**: The current NUL separators are invisible in editors and break text-based tooling, with zero functional benefit — `Dynamic` only needs value inequality, not any particular separator.
- **Alternatives Considered**:
  - *Keep NUL via `"\0"` escape*: visible in source but still exotic; no advantage over `"|"`.

### Decision 2: Single-pass ban search
- **Choice**: One loop over `Vars.content.blocks()` matching name/localizedName, capped at 60 results; delete the hidden-first pass and its fallback re-scan.
- **Rationale**: The two-pass version's first filter (`!block.isHidden()` → skip) has no documented intent and the fallback makes the first pass pure overhead. One pass is easier to verify and behaves identically for all practical queries.
- **Alternatives Considered**:
  - *Keep hidden-first ordering (hidden matches first)*: no evidence anyone wants hidden blocks ranked first; adds complexity without a requirement.

### Decision 3: Delete, don't deprecate
- **Choice**: Remove `sync()`, `cancelPending()`, and the `retainContentFields` call outright.
- **Rationale**: `sync()` has no callers and duplicates `mutate`'s broadcast; `cancelPending()` has no callers (`hidden()` uses `flushPending()`); the `retainContentFields` call only rewrites spawns/weather/objectives, which reset never reads (verified against Mindustry v160.7 bytecode — bans survive capture either way). Private/internal surface, so deletion is safe.
- **Alternatives Considered**:
  - *Keep with `@Deprecated`*: noise without consumers; the codebase has no deprecation policy for internal helpers.

## Risks / Trade-offs

- **[Risk] Ban search ordering changes for exotic queries** → Mitigation: behavior check in-game (search "thorium"-style queries, compare chip order); single pass is strictly more predictable.
- **[Risk] Hidden reliance on deleted methods via reflection/tests** → Mitigation: grep shows no callers; compilation + checkstyle verification after removal.
