## 1. QueryView Component Implementation

- [ ] 1.1 Implement `QueryView<T>` in `solim-core/src/solim/reactive/QueryView.java` supporting `.loading()`, `.error()`, and `.data()` with SWR refetch awareness
- [ ] 1.2 Implement built-in default UI for loading (centered spinner) and error (error text + retry button calling `query.refetch()`) in `QueryView`
- [ ] 1.3 Expose `UI.query(Query<T>)` factory method in `solim/src/solim/UI.java`
- [ ] 1.4 Create unit tests for `QueryView` state transitions: initial state, loading, success, error, loading → success, loading → error, and error → retry → success
- [ ] 1.5 Create unit tests for `QueryView` SWR refetch states: success → refetch → fetching, success → refetch → success, and success → refetch → error
- [ ] 1.6 Create unit tests for `QueryView` data reconciliation: data changes, keyed forEach reconciliation, empty data, and no unnecessary rebuilding
- [ ] 1.7 Create unit tests for `QueryView` concurrency & edge cases: stale/late requests, callback exceptions, duplicate keys, query shared by multiple components, query outliving component, and disposal/unmount

## 2. Browser Filter Dialog Migration

- [ ] 2.1 Refactor `BrowserFilterDialog` in `mod/src/mindustrytool/features/browser/common/BrowserFilterDialog.java` to use cached `Query` for tag categories with 10m stale time
- [ ] 2.2 Refactor `BrowserFilterDialog` to use cached `Query` for planets with 10m stale time
- [ ] 2.3 Verify filter dialog tags and planets render cleanly and reuse cached data on reopen

## 3. Detail Dialogs Author Lookup Migration

- [ ] 3.1 Refactor `MapDetailDialog` in `mod/src/mindustrytool/features/browser/map/MapDetailDialog.java` to use `Query` for author profile lookup and render via `QueryView`
- [ ] 3.2 Refactor `SchematicDetailDialog` in `mod/src/mindustrytool/features/browser/schematic/SchematicDetailDialog.java` to use `Query` for author profile lookup and render via `QueryView`
- [ ] 3.3 Verify author lookups deduplicate across dialogs and cache author profiles

## 4. Core Services Migration

- [ ] 4.1 Refactor `ServerService` in `mod/src/mindustrytool/services/ServerService.java` to use a `Query` with `refetchInterval(Duration.ofMinutes(15))`
- [ ] 4.2 Refactor `Github` and `UpdateService` to prefetch version metadata into `QueryCache` on startup
- [ ] 4.3 Run full test suite and verify clean compilation and execution
