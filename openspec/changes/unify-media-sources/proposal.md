## Why

Currently, image display in Solim is split across two separate components with distinct paradigms:
- `SolimImage`: static/memory-based drawables with direct Arc `Image` wrapping.
- `NetworkImage`: URL-based asynchronous image loading with disk/memory caching and corner rounding.

This creates duplicated capability wiring, conditional branching (`if host instanceof SolimImage ... else if host instanceof NetworkImage`) in modifier definitions, and inconsistent developer ergonomics between images and videos. Merging `NetworkImage` into `SolimImage` and standardizing both images and videos on dedicated data providers (`ImageSource` and `VideoSource`) provides a clean, unified API across all Solim media rendering.

## What Changes

- Introduce `ImageSource` in `solim.display`: encapsulates in-memory `Drawable`, `TextureRegion`, remote URL strings, and local `Fi` file sources.
- Introduce `VideoSource` in `solim.display.video`: encapsulates remote URL strings, local `Fi` files, or custom players.
- Merge all `NetworkImage` functionality directly into `SolimImage`:
  - Asynchronous loading, caching, fallback, placeholder, and rounded corner masking.
  - Pluggable loader support via `ImageLoader` and dedicated `ImageCache`.
- **BREAKING**: Remove `NetworkImage` class. All callers (`ChatAvatar`, tests, `UI.networkImage`) migrate to `SolimImage` / `UI.image`.
- Add `src(...)` modifier to `Values` for both `ImageCapability` and `VideoCapability`.
- Replace `IconCapability` with `ImageCapability` for image component modifiers, preserving separate button icon behavior where appropriate.
- Extract image caching, mask generation, and loader infrastructure from the UI component into dedicated `ImageCache` and `DefaultImageLoader` classes.

## Capabilities

### New Capabilities
- `media-sources`: Abstract data provider abstractions (`ImageSource`, `VideoSource`) and their lifecycle resolution for Solim display primitives.

### Modified Capabilities
- `network-image`: Replaced and subsumed by `SolimImage`. The behavior contract (async loading, placeholder, fallback, memory/disk cache, rounded masks) is now fulfilled by `SolimImage`.
- `solim-video`: Extended to accept `VideoSource` instances alongside URLs, supporting unified `src(...)` props.

## Impact

- `solim-core`: `NetworkImage.java` removed; `SolimImage.java` enhanced with source resolution and async loading; `ImageSource.java` and `ImageCache.java` added; `VideoSource.java` added; `Values.java` updated to route `src(...)` and update capabilities.
- `solim`: `UI.java` replaces `networkImage(...)` with overloaded `image(...)` factories accepting `ImageSource`, `String` (URL), and `Drawable`.
- `mod`: `ChatAvatar.java` and browser tests updated to use `UI.image(...)` / `SolimImage`.
