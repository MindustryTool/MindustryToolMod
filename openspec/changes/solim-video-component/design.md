## Context

Solim UI is the declarative, reactive UI system for Mindustry mods running on Java 8 (JVM desktop) and Android (via D8 desugaring). While Solim provides `NetworkImage` for static raster assets, dynamic video content (e.g. tutorials, feature demos, changelog teasers, schematic demonstrations) currently has no native engine support in Arc or Mindustry.

Adding video capability requires addressing cross-platform container demuxing, video stream decoding, efficient OpenGL texture updates, and clean UI integration without violating Android compatibility constraints (e.g. absence of `java.awt` and restrictive dynamic JNI library loading).

## Goals / Non-Goals

**Goals:**
- Provide a pure Java MP4 / H.264 baseline video decoder using `org.jcodec:jcodec` that functions identically on both Desktop and Android.
- Ensure zero-allocation frame updates during playback to eliminate garbage collection hitching.
- Implement a declarative `SolimVideo` leaf component (`solim.UI.video(url)`) with support for size, aspect ratio, rounded corners, autoplay, and looping.
- Provide an auto-hide overlay control bar featuring play/pause toggle, interactive progress scrub bar, elapsed/total time readout, replay button, and a buffering indicator.
- Support disabling controls entirely (`.controls(false)` or `.controls(Readable<Boolean>)`) so the component can function as a pure looping banner or GIF replacement.
- Implement an injectable `VideoLoader` with transparent disk caching under the Solim cache directory, allowing `:mod` to route network fetching through `mindustrytool.services.Request`.

**Non-Goals:**
- Audio demuxing or synchronized OpenAL audio playback (videos are silent motion clips).
- Native hardware decoding via FFmpeg, MediaCodec, or JNI binaries.
- High-definition 1080p60/4K decoding (the pure Java software pipeline targets clips < 1 min at 240p–480p).
- Live adaptive streaming protocols (HLS, DASH, RTSP).

## Decisions

### Decision 1: Pure Java JCodec Core without AWT
- **Choice**: Depend on `org.jcodec:jcodec:0.2.5` core directly in `:solim-core`, excluding `jcodec-javase`.
- **Rationale**: `jcodec-javase` depends on `java.awt.image.BufferedImage`, which does not exist on Android and causes immediate `NoClassDefFoundError` crashes. Core JCodec is pure computational Java that compiles cleanly to Java 8 bytecode and desugars smoothly via D8 for Android API 14+.
- **Alternatives Considered**:
  - *JavaCV / FFmpeg native binaries*: Exceeds 50MB, causes architecture fragmentation (x86_64, arm64-v8a, armeabi-v7a), and fails on Android under Mindustry's `ModClassLoader`.
  - *Animated GIF / APNG*: Lacks standard video compression efficiency (file sizes 5–10x larger than H.264).

### Decision 2: Zero-Allocation YUV-to-RGBA Pipeline & In-Place Texture Uploads
- **Choice**: The decoder background thread converts JCodec `Picture` YUV420p byte planes directly into a pre-allocated direct `ByteBuffer` backing a reusable Arc `Pixmap`. The render thread calls `Texture.draw(frontPixmap, 0, 0)` which invokes OpenGL's `glTexSubImage2D`.
- **Rationale**: Reallocating GL textures or `Pixmap` instances at 24–30 FPS causes severe heap churn and GPU resource leaks. A double-buffered ping-pong pair of `Pixmap` instances decouples background decoding from main-thread rendering without allocations.
- **Alternatives Considered**:
  - *Creating new `Texture(pixmap)` per frame*: Causes severe GPU memory fragmentation and OpenGL context stalls.

### Decision 3: Pluggable `VideoLoader` with Disk Caching
- **Choice**: Mirror `NetworkImage`'s architecture with `VideoLoader` (`Fi load(String url, Cons<Fi> onSuccess, Cons<Throwable> onError)`). Solim provides `DefaultVideoLoader` using standard HTTP streams, while `:mod` registers `RequestVideoLoader` backed by `mindustrytool.services.Request`.
- **Rationale**: Keeps `:solim-core` decoupled from the mod module while enforcing the mandatory project rule that all mod HTTP traffic flows through `Request`. Videos are cached by URL hash in `cache/videos/` so repeat views do not hit GitHub or burn network bandwidth.

### Decision 4: Auto-Hide Overlay Controls with Complete Toggle
- **Choice**: Controls are structured as an overlay layer on top of the video canvas. Mouse movement or touch interaction reveals the overlay, which automatically fades out after 2.5 seconds of continuous playback without input. Calling `.controls(false)` removes the overlay listener and controls entirely.
- **Rationale**: Maximizes visual real estate for demo clips while providing full media player ergonomics when needed.

## Risks / Trade-offs

- **[Risk] High resolution pure Java decoding CPU overhead**  
  → *Mitigation*: Run demuxing and decoding strictly on a daemon worker pool (`solim-video-decode`); document recommended clip specs (360p or 480p baseline H.264, 24–30 fps).
- **[Risk] Seeking latency on long GOP intervals**  
  → *Mitigation*: Decoder checks for nearest preceding keyframe (IDR frame) when scrubbing and fast-forwards decode to the target timestamp; recommend 1–2 second keyframe intervals in asset guidelines.
- **[Risk] Resource leaks on dialog closure**  
  → *Mitigation*: `SolimVideo` implements `Disposable` and hooks into Solim's automatic component lifecycle ownership, terminating background decode workers and releasing OpenGL texture handles upon disposal.
