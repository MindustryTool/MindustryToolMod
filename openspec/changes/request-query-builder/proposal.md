## Why

`buildPagedUrl` in `MindustryTool.java` manually concatenates URL query strings with encoding. It's a private static method called from 2 places (`searchMaps`, `searchSchematics`). This pattern:

- Mixes URL construction logic into service code
- Doesn't compose with `RequestBuilder`'s fluent API
- Requires manual encoding and null/empty handling at each call site
- Makes adding new query parameters a multi-line affair

Additionally, the current API parameters don't match the backend:
- `query` should be `name` (backend expects `?name=foo`, not `?query=foo`)
- `author` (UUID filter) exists in the backend but is not wired
- `verification` (ALL | PENDING | VERIFIED | REJECTED) exists in the backend but is not wired

## What Changes

### Layer 1: RequestBuilder query support

- Add `.query(key, value)` overloads to `RequestBuilder` supporting `String`, `int`, `float`, `List<String>`, and `Map<String, String>`
- Accumulate query parameters in a `LinkedHashMap<String, List<String>>` on `RequestBuilder`
- Assemble the final `?key=value&...` string at `sendAsync()` time, before `resolveUrl()`
- URL-encode all keys and values using `URLEncoder.encode(UTF_8)`
- Skip null/empty String values; throw `NullPointerException` on null keys
- `Map` overload replaces existing entries for each key (vs append for other overloads)
- Delete `buildPagedUrl` from `MindustryTool.java` and migrate call sites

### Layer 2: API parameter alignment

- Rename `query` parameter to `name` in `searchMaps` and `searchSchematics` signatures
- Add `author` parameter (String UUID, nullable) — API only, no UI
- Add `verification` parameter (String enum, nullable) — API + UI filter
- Update `buildPagedUrl` removal to use `.query()` with corrected parameter names

### Layer 3: Verification filter UI

- Add `verification` signal to `BrowserState` (default: ALL)
- Render verification toggle chips in `BrowserFilterDialog` (ALL | PENDING | VERIFIED | REJECTED)
- Wire verification signal through fetch pipeline

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None.

## Impact

- `mod/src/mindustrytool/services/Request.java` — `RequestBuilder` gains query parameter support
- `mod/src/mindustrytool/services/MindustryTool.java` — delete `buildPagedUrl`, migrate `searchMaps` and `searchSchematics` with corrected params
- `mod/src/mindustrytool/features/browser/common/BrowserState.java` — add `verification` signal
- `mod/src/mindustrytool/features/browser/common/BrowserFilterDialog.java` — add verification filter chips
- `mod/src/mindustrytool/features/browser/schematic/SchematicBrowserDialog.java` — pass verification through
- `mod/src/mindustrytool/features/browser/map/MapBrowserDialog.java` — pass verification through
- `assets/bundles/bundle.properties` — add verification filter translation keys
