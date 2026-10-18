## Purpose

Behavior contract for `solim.display.SolimImage` — the unified async image component with pluggable loader, memory/disk caching, placeholder/fallback, target-size-aware rounded-corner masking, and reactive `ImageSource` switching. Established as a behavior baseline by change `network-image-refactor` and extended by `unify-media-sources`.

## Requirements

### Requirement: SolimImage Creation and Display
`SolimImage` SHALL subsume all capabilities of the deleted `NetworkImage`. `SolimImage` SHALL be a Solim leaf component wrapping an Arc `Image` that displays textures from static drawables, remote URLs, or local files via `ImageSource`. It SHALL support `placeholder(Drawable)` shown while loading (and for null/blank URLs), `fallback(Drawable)` shown after a load failure, `scaling(Scaling)`, `rounded(int radius)` (with target-size-aware mask scaling), and both static `src(ImageSource)` and reactive `src(Readable<ImageSource>)` sources (with convenience overloads for URL strings and Drawables).

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

### Requirement: Disk Cache Transparency
The default loader SHALL write fetched bytes to a disk cache under the Solim data directory and SHALL serve subsequent loads of the same URL from that cache when the memory cache misses. Corrupt disk entries SHALL be deleted and the load SHALL fall back to the network within the same load call. `SolimImage` SHALL NOT expose disk-cache implementation details in its API.

#### Scenario: Second load served from disk without network
- **WHEN** the same URL is loaded twice through the default loader (memory cache cleared between loads) and the disk cache entry is present
- **THEN** the second load SHALL be served from the disk cache without a second HTTP request

#### Scenario: Corrupt disk entry falls back to network
- **WHEN** a disk cache entry exists but its bytes are unreadable or fail to decode
- **THEN** the entry SHALL be deleted and the load SHALL proceed over the network in the same load call

### Requirement: Rounded Corners
`ImageCache.applyRoundedMask(pixmap, radius, targetW, targetH)` SHALL apply an alpha-falloff rounded-corner mask scaled by the ratio of source to target width, and is deterministic for identical inputs. `rounded(int)` on `SolimImage` SHALL re-load the current source so the mask matches the new radius.

#### Scenario: Radius change re-keys the cache
- **WHEN** `rounded(8)` is called after a load with radius 0
- **THEN** the fetch SHALL be re-keyed under the new radius and SHALL NOT overwrite the radius-0 cache entry

### Requirement: Disposal
`SolimImage` SHALL implement `Disposable`; disposal SHALL release the reactive source binding and the component. Disposal SHALL NOT clear the shared static memory cache or shut down the shared decode worker (application-lifetime resources).

#### Scenario: Dispose releases binding
- **WHEN** a `SolimImage` with a reactive source binding is disposed
- **THEN** the binding SHALL be disposed and source changes SHALL no longer trigger loads

#### Scenario: Dispose preserves shared cache
- **WHEN** a `SolimImage` is disposed
- **THEN** the static memory cache SHALL retain its entries for other components
