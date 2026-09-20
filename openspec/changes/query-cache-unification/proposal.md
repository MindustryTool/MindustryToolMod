## Why

Query adoption left three fetch paths (`fetchOrJoin`, `fetchCached`, `prefetch`) with diverging stale/error semantics, causing Github getters to lose memoization and translation/user keys to grow unbounded. Dialog-scoped Query disposal and background-fetching UX also remain implicit.

## What Changes

- Unify `QueryCache` fetch policy to always-cache + dedupe inflight + drop errors on failure (single path, success writes data, failure preserves last good data).
- Fix `Github` getters and `Query.doFetch` to use the unified stale-aware path so prefetch warming is effective.
- Remove long-term `QueryCache` retention for translations (and `user/*` profiles): revert to network-only calls with inflight button guard, eliminating per-sentence permanent keys.
- Document full-spinner-on-fetching as intended `QueryView` behavior (`fetching ? Loader : content`).
- Verify component-owned `Query` disposal via `ComponentContext`: dialog-scoped Queries auto-dispose on unmount, proven by observer/entry-count regression test.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `query-cache`: unify fetch policy to always-cache + dedupe + drop errors; clarify stale vs cache-first.
- `services`: Github uses unified cache path; translation reverts to network-only.
- `query-view`: background fetching renders full spinner (no stale retention in view layer).
- `query-primitive`: component owns Query lifecycle via `ComponentContext` auto-dispose.

## Impact

- `solim-core`: `Query.java`, `QueryCache.java`, `CacheEntry.java`, `QueryView.java`, unit tests (`QueryTest`, `QueryViewTest`, `QueryCacheTest`).
- `mod`: `Github.java`, `MindustryTool.translate`, `TranslationFeature.translate`, `ChatService` user-batch cache, `BrowserState`, `MapBrowserDialog`/`SchematicBrowserDialog`, `RoomBrowserView`, `ChatChannelListView`, `ServerService`.
- No breaking external API; internal cache semantics and disposal guarantees tighten.
