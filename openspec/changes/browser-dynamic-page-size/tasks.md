## 1. Browser Layout Helper

- [x] 1.1 Create `BrowserLayout` in `mindustrytool.features.browser.common` with card dimensions, gaps, overhead, and column/row/capacity calculation methods.
- [x] 1.2 Add unit tests for `BrowserLayout` verifying column count, row count, capacity, and clamping to [20, 100] across various resolutions.

## 2. BrowserState Dynamic Page Size

- [x] 2.1 Add reactive `pageSize` signal (`Signal<Integer>`), `getPageSize()`, and `setPageSize(int)` to `BrowserState`.
- [x] 2.2 Update `autoFetch` effect in `BrowserState.start()` to track `pageSize.get()`.

## 3. Dialog Integration

- [x] 3.1 Update `SchematicBrowserDialog` to use `BrowserLayout` and query `searchSchematics` with `state.pageSize().peek()`.
- [x] 3.2 Update `MapBrowserDialog` to use `BrowserLayout` and query `searchMaps` with `state.pageSize().peek()`.
- [x] 3.3 Bind `pageSize` reactively to viewport dimensions via `BrowserLayout.calculatePageSize` in `BrowserContent` for both dialogs.

## 4. Verification & Build

- [x] 4.1 Compile the project with `./gradlew compileJava test` and ensure all tests pass.
- [x] 4.2 Package mod jar with `./gradlew :mod:jar` and verify artifact.
