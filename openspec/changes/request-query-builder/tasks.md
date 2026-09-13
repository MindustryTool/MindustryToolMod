## 1. RequestBuilder Query Support

- [x] 1.1 Add `Map<String, List<String>> queryParams` field to `RequestBuilder`
- [x] 1.2 Add `.query(String key, String value)` overload with null key check and null/empty value skip
- [x] 1.3 Add `.query(String key, int value)` overload
- [x] 1.4 Add `.query(String key, float value)` overload
- [x] 1.5 Add `.query(String key, List<String> values)` overload (append each)
- [x] 1.6 Add `.query(Map<String, String> params)` overload (replace per key)
- [x] 1.7 Assemble query string in `sendAsync()` before `resolveUrl()`
- [x] 1.8 Add unit tests in mod verifying query parameter assembly and edge cases

## 2. Migration & Cleanup

- [ ] 2.1 Migrate `searchMaps` to use `.query()` with corrected param names (name, author, verification)
- [ ] 2.2 Migrate `searchSchematics` to use `.query()` with corrected param names
- [ ] 2.3 Delete `buildPagedUrl` method
- [ ] 2.4 Verify project builds and all tests pass with `./gradlew :mod:compileJava :mod:test`

## 3. Verification Filter

- [ ] 3.1 Add `verification` signal to `BrowserState` with default "ALL"
- [ ] 3.2 Update `BrowserState.fetch()` to accept and pass verification parameter
- [ ] 3.3 Update `BrowserFilterDelegate` functional interface to include verification
- [ ] 3.4 Add verification toggle chips to `BrowserFilterDialog` (ALL | PENDING | VERIFIED | REJECTED)
- [ ] 3.5 Wire `SchematicBrowserDialog` and `MapBrowserDialog` to pass verification through
- [ ] 3.6 Add translation keys for verification filter labels in `bundle.properties`
