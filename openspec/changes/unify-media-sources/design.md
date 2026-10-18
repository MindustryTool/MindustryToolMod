## Context

Currently, Solim has two separate components for image display:
- `SolimImage`: wraps an Arc `Image`, operates purely on in-memory `Drawable` instances, implements `IconCapability`, and cannot load from URLs or cache files.
- `NetworkImage`: wraps an Arc `Image`, operates on HTTP URLs, implements its own asynchronous loading loop, in-memory and disk cache, rounded corner generator, and has separate props (`url`, `placeholder`, `fallback`).

For video, `SolimVideo` operates on video URLs and implements `VideoCapability`.

In `Values.java`, modifiers like `scaling`, `url`, `fallback`, and `icon` have ad-hoc `instanceof` checks that inspect whether the host is `NetworkImage`, `SolimImage`, or `SolimVideo`.

This design unifies `SolimImage` and `NetworkImage` into an enhanced `SolimImage` driven by an `ImageSource` data provider abstraction, introduces `VideoSource` for `SolimVideo`, replaces `IconCapability` with `ImageCapability`, and extracts caching and decoding machinery out of the UI element into focused subsystems.

## Goals / Non-Goals

**Goals:**
- Provide a single `SolimImage` component that seamlessly handles static Drawables, TextureRegions, remote URLs, and local files.
- Introduce `ImageSource` and `VideoSource` types with factory methods (`ImageSource.of(drawable)`, `ImageSource.of(url)`, etc.).
- Allow `SolimImage` and `SolimVideo` to receive sources both at construction (`image(src)`, `video(src)`) and via declarative props (`props(src(...))`).
- Introduce `ImageCapability` for image component modifiers, replacing `IconCapability` on `SolimImage` and cleaning up `Values.java`.
- Completely eliminate `NetworkImage` without leaving deprecated dead code.
- Extract image caching, mask generation, and loading into `ImageCache` and `DefaultImageLoader` in `solim.display`.

**Non-Goals:**
- Merging `SolimImage` and `SolimVideo` into a single UI widget (video has unique overlay controls, scrubbers, audio decoders, and playback lifecycle that should not pollute `SolimImage`).
- Changing the underlying video decoding or pure-Java H.264/AAC pipeline.
- Creating a unified `MediaSource` super-interface across image and video (they resolve to completely different underlying types — `Drawable`/`TextureRegion` vs `VideoPlayer`/`Fi`).

## Decisions

### 1. Dedicated `ImageSource` and `VideoSource` value types
- **Decision**: Create `ImageSource` (sealed or interface with records/implementations: `DrawableImageSource`, `UrlImageSource`, `FiImageSource`) and `VideoSource` (`UrlVideoSource`, `FiVideoSource`).
- **Rationale**: Strongly typed, explicit, and extensible. Allows passing metadata (e.g. custom cache key or desired dimensions) without overloading widget methods.
- **Alternatives considered**:
  - *Unified `MediaSource`*: Rejected because image rendering outputs an Arc `Drawable`, whereas video playback produces continuous frame buffers uploaded via OpenGL texture streaming and synchronized audio tracks. Combining them causes type loss and runtime casting.
  - *Direct Object / String / Drawable parameters only*: Lacks a clear abstraction boundary for custom providers or future sources (e.g. classpath assets, procedural generators).

### 2. Complete removal of `NetworkImage`
- **Decision**: Delete `NetworkImage.java` and its specific tests, replacing them with enhanced `SolimImage` tests. Update `ChatAvatar`, `SchematicBrowserPerfTest`, and `UI.java`.
- **Rationale**: Keeping a hollow alias creates confusion and violates the project rule: "Do not introduce complexity unless it provides clear value."

### 3. Decouple Caching and Loader into `ImageCache`
- **Decision**: Move `cache`, `applyRoundedMask`, and disk caching logic out of the component class into `ImageCache` and `DefaultImageLoader`.
- **Rationale**: UI components should describe display and lifecycle, not act as global static caches. `SolimImage` can still expose static delegates (`SolimImage.clearCache()`) for ergonomics and backward-compatible test fixtures.

### 4. Transition from `IconCapability` to `ImageCapability`
- **Decision**: Introduce `ImageCapability` for `SolimImage`. `Button` continues to support `icon(...)` via its own capability or retained `IconCapability`.
- **Rationale**: An image is not just an icon. `ImageCapability` cleanly hosts `src`, `rounded`, `placeholder`, `fallback`, and `scaling` without polluting buttons or requiring hacky runtime type checks.

## Risks / Trade-offs

- **[Risk] Existing callers or tests referencing `NetworkImage` break**
  → *Mitigation*: The usage footprint in the codebase is small (`ChatAvatar.java`, `UI.java`, and a few test classes). Migrate all usages in the same change.
- **[Risk] In-flight load cancellation when changing sources rapidly**
  → *Mitigation*: Retain generation counting (`loadGeneration++`) and ensure reactive bindings to `Readable<ImageSource>` cleanly discard stale async responses.
- **[Risk] Reactive source switching between different source types (e.g. from URL to Drawable)**
  → *Mitigation*: When source changes, synchronously apply static drawables and cancel any active async loads immediately.
