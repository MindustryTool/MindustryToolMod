## MODIFIED Requirements

### Requirement: NetworkImage Creation and Display
`SolimImage` SHALL subsume all capabilities of `NetworkImage`. `SolimImage` SHALL be a Solim leaf component wrapping an Arc `Image` that displays textures from static drawables, remote URLs, or local files via `ImageSource`. It SHALL support `placeholder(Drawable)` shown while loading (and for null/blank URLs), `fallback(Drawable)` shown after a load failure, `scaling(Scaling)`, `rounded(int radius)` (with target-size-aware mask scaling), and both static `src(ImageSource)` and reactive `src(Readable<ImageSource>)` sources (with convenience overloads for URL strings and Drawables).

#### Scenario: Placeholder shown during load
- **WHEN** a `SolimImage` is given an async `ImageSource` with no cached image and a placeholder drawable
- **THEN** the placeholder SHALL be displayed until the fetch completes or fails

#### Scenario: Fallback shown on failure
- **WHEN** a load fails and a fallback drawable is set
- **THEN** the fallback SHALL replace the placeholder

#### Scenario: Blank URL uses fallback or placeholder
- **WHEN** an empty or null URL `ImageSource` is set
- **THEN** no fetch SHALL be triggered and the fallback (or placeholder if no fallback) SHALL be displayed

#### Scenario: Stale result discarded
- **WHEN** the source or radius changes while a fetch is in flight, and the old fetch completes afterward
- **THEN** the stale result SHALL NOT be applied to the component

### Requirement: Loader Injection
Image fetching SHALL occur through an injectable `ImageLoader` with a single method `load(String url, int radius, float targetW, float targetH, Cons<TextureRegion> onSuccess, Cons<Throwable> onError)`. `SolimImage.setImageLoader(null)` SHALL restore the default loader. The default loader SHALL fetch over HTTP, write the response to the disk cache, and decode the bytes.

#### Scenario: Custom loader receives full parameters
- **WHEN** a load is triggered with radius `r` and target size `w×h`
- **THEN** the loader SHALL be invoked with exactly those radius and target values

#### Scenario: Loader reset to default
- **WHEN** `setImageLoader(null)` is called
- **THEN** subsequent loads SHALL use the default HTTP loader

### Requirement: Memory Cache API
`SolimImage` and `ImageCache` SHALL expose memory-cache helpers `cacheKey(url, radius, targetW)`, `isCached(...)`, `getCached(...)`, `putCache(...)`, and `clearCache()` backed by an in-memory map keyed by URL, radius, and target width. Cached entries SHALL be applied without invoking the loader.

#### Scenario: Cached entry skips loader
- **WHEN** a URL/radius combination is already in the memory cache
- **THEN** the loader SHALL NOT be invoked and the cached drawable SHALL be displayed

#### Scenario: Radius participates in the cache key
- **WHEN** the same URL is loaded with radius 0 and then radius 8
- **THEN** two distinct cache entries SHALL exist

## REMOVED Requirements

### Requirement: NetworkImage Component Class
**Reason**: `NetworkImage` is deleted and completely replaced by `SolimImage`.
**Migration**: Replace `new NetworkImage(url)` or `UI.networkImage(url)` with `new SolimImage(url)` or `UI.image(url)`.
