## 1. Unified QueryCache fetch policy

- [ ] 1.1 Collapse `fetchOrJoin` / `fetchCached` / `prefetch` to a single always-cache + dedupe + drop-errors path (success writes data, failure preserves last good data)
- [ ] 1.2 Route `Query.doFetch` through the unified path with per-query `staleTimeMs`
- [ ] 1.3 Extend `QueryCache` unit tests for cache hit, inflight dedupe, and failure-preserves-data

## 2. Service migrations

- [ ] 2.1 Fix `Github` getters to the unified path and verify startup prefetch hit without extra network
- [ ] 2.2 Revert `MindustryTool.translate` and `TranslationFeature.translate` to network-only with blank-text short-circuit, removing `translation/*` keys
- [ ] 2.3 Revert `ChatService` user-batch `user/*` cache to direct fetch with caller-side inflight guard
- [ ] 2.4 Resolve `ChatChannels.setLoading` / `setError` no-op contract (delete or delegate to `invalidate` / `mutate`)

## 3. QueryView fetching UX

- [ ] 3.1 Lock full-spinner on background fetching in browser and room views, updating `QueryViewTest` for refetch-to-loading
- [ ] 3.2 Confirm `BrowserState.loading()` as `fetching()` is intentional and documented

## 4. Component-owned disposal

- [ ] 4.1 Add dialog open/close regression test asserting `observerCount` / `entryCount` return to baseline
- [ ] 4.2 Make state holders created outside component scope implement `Disposable` if the regression test fails

## 5. Verification

- [ ] 5.1 Run `solim-core` and `mod` tests, verify Java 8 runtime compat and `Core.bundle` keys for touched views
