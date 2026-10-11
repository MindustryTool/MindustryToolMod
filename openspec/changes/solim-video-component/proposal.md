## Why

Mindustry currently lacks built-in support for playing video media (e.g. MP4 clips) in its Arc-based UI, requiring mod developers to rely on static images or complex sprite sheets for tutorials, feature showcases, changelog teasers, and previews. Providing a lightweight, cross-platform video player component in Solim allows rich motion media to be embedded cleanly across game dialogs and HUDs on both Desktop and Android without requiring heavy native dependencies like FFmpeg.

## What Changes

- Add pure Java `org.jcodec:jcodec:0.2.5` to `:solim-core` dependencies to decode MP4 (ISO BMFF container) and H.264 baseline video streams without `java.awt` dependencies.
- Create `solim.display.video.VideoLoader` and `DefaultVideoLoader` with disk caching under Solim data storage, and pluggable loader injection matching the `NetworkImage` architecture.
- Create `solim.display.video.VideoPlayer` driving asynchronous background frame decoding, delta-time presentation scheduling, and double-buffered zero-allocation OpenGL texture updates (`glTexSubImage2D`).
- Create `solim.display.video.SolimVideo` leaf component supporting reactive source URLs, auto-play, looping, rounded masking, sizing, and an interactive auto-hide overlay control bar (play/pause, progress scrubber, timestamp readout, replay button, and buffering indicator).
- Add support for toggling controls completely off (`.controls(false)` or `.controls(Readable<Boolean>)`) to allow pure looping banner/GIF behavior.
- Add declarative `video(String)` and `video(Readable<String>)` facade methods to `solim.UI`.
- Register a `RequestVideoLoader` adapter in `:mod` so that network video downloads use `mindustrytool.services.Request` in compliance with mod rules.

## Capabilities

### New Capabilities
- `solim-video`: Introduces video downloading, disk caching, pure Java H.264 decoding, zero-allocation GPU texture streaming, and the `SolimVideo` declarative UI component with configurable auto-hide overlay controls.

### Modified Capabilities
<!-- None: Solim video is a strictly additive capability to the UI system. -->

## Impact

- **Dependencies**: Adds `org.jcodec:jcodec:0.2.5` to `:solim-core` (pure Java, ~1.8MB, fully compatible with Java 8 and Android D8 desugaring).
- **APIs**: Introduces `solim.display.video.*` and adds `solim.UI.video(...)` facades.
- **Mod Runtime**: Wires `RequestVideoLoader` during mod initialization to route video downloads through `mindustrytool.services.Request`.
