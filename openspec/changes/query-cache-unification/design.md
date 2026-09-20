## Context

`Query`, `QueryCache`, and `QueryView` were adopted across browsers, chat, PlayerConnect, and services over Phase 1, Phase 2, and clean-migration. Current state: `QueryCache` exposes three overlapping paths (`fetchOrJoin` always-network + join inflight, `fetchCached` stale-aware, `prefetch` stale-aware + drop errors); `Query.doFetch` and `Github` getters use the always-network path, nullifying prefetch warming. Translation and `user/*` profile keys retain per-sentence entries forever (no observer, no GC). `QueryView` background-fetching UX is implemented as full spinner in dialogs but spec'd as stale-retention. Dialog-scoped Query disposal via `ComponentContext.register` is assumed but unverified for state holders constructed outside `build()`.

Constraints: Java 8 runtime APIs only, all UI mutations on main thread via `Core.app.post()`, Solim declarative ownership (`ComponentContext`, no manual `own()` in mod), i18n via `Core.bundle`.

## Goals / Non-Goals

**Goals:**
- Single `QueryCache` fetch policy: always serve cached data if present, dedupe inflight, drop errors (preserve last good data).
- Fix Github + `Query` to respect stale/cache so warming works; remove translation/user long-term retention.
- Lock full-spinner fetching UX as intended and align spec.
- Prove component-owned disposal with regression coverage for dialog open/close cycles.

**Non-Goals:**
- Pagination primitives inside `Query`; paged keys remain parameterized.
- New translation providers or relay protocol changes.
- LRU/size-cap cache eviction (deferred; network-only for translations removes the acute leak).

## Decisions

### Decision 1: Unified fetch — always-cache + dedupe + drop errors
- **Choice:** Collapse to one policy: if entry has data (regardless of stale? stale decides background refetch at Query layer), join inflight if present; otherwise fetch once, `setData` on success, leave cache untouched on failure. `CacheEntry.error` is no longer written by fetch paths; `Query.error` signal remains local-only.
- **Rationale:** Matches user answer and `prefetch` failure guarantee already spec'd. Eliminates `fetchOrJoin` vs `fetchCached` divergence that caused Github always-network bug.
- **Alternatives:** Keep split with clarified docs (rejected: keeps the bug surface); policy param `CacheFirst/NetworkOnly/SWR` (rejected: larger refactor, no caller needs NetworkOnly except manual `refetch()` which bypasses cache explicitly).

### Decision 2: Translation and user profiles go network-only
- **Choice:** `TranslationFeature.translate` / `MindustryTool.translate` call provider directly; `ChatService` user-batch calls `getUserBatch` directly. Guard duplicate taps via disabled button/inflight join in the caller, not `QueryCache` keys. Remove `QueryKey.of("translation", ...)` and `QueryKey.of("user", ...)` retention.
- **Rationale:** User decision; chat sentences are unbounded cardinality, 24h retention pins memory with no GC path. Rate-limit risk accepted; common-phrase repeats are cheap relative to leak.
- **Alternatives:** TTL+GC 24h or LRU-500 (rejected per answer; keeps complexity for low-value cache).

### Decision 3: Full spinner on background fetching
- **Choice:** `MapBrowserDialog` / `SchematicBrowserDialog` / `RoomBrowserView` keep `(data, fetching) -> fetching ? Loader : content`. `BrowserState.loading()` returning `fetching()` stays intentional.
- **Rationale:** User choice for simplicity; avoids designing per-surface stale indicators now.
- **Alternatives:** Stale + badge (rejected for this change; can revisit if flicker complaints).

### Decision 4: Component owns Query via ambient context + regression test
- **Choice:** Queries created for a dialog (including inside `BrowserState`/`ChatChannels` constructors when those holders are themselves created inside the dialog's `build()`/`children()` scope) are auto-disposed with the dialog via `ComponentContext`. Add open/close-cycle test asserting `observerCount`/`entryCount` returns to baseline. If holders are created outside any component scope, they must implement `Disposable` and be disposed explicitly — test will force the answer.
- **Rationale:** User choice; makes the assumption falsifiable instead of assumed.
- **Alternatives:** Manual dispose everywhere (rejected: verbose, easy to forget); permanent singletons per key (rejected: pins memory, breaks dialog isolation).

## Risks / Trade-offs

- **[Risk]** Dropping cache-side errors hides failure history from late mounters; they refetch instead of showing last error. **Mitigation:** `Query.error` still drives `QueryView` error state for active observers; late mounters get fresh fetch, not stale error.
- **[Risk]** Network-only translation increases provider calls/rate-limit hits. **Mitigation:** Inflight tap guard + retry(0) for translations (fail fast, no backoff spam); monitor rate-limit errors.
- **[Risk]** Full spinner flickers on every keystroke filter change. **Mitigation:** Debounce search input (existing) + `staleTime` guards mount refetch; accept residual flicker.
- **[Risk]** `ComponentContext` may not cover state-holder constructors; test may fail and force manual `Disposable` on holders. **Mitigation:** Treat test failure as design signal, add explicit dispose path.
