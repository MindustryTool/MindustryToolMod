## Why

Mindustry currently lacks built-in support for playing video and accompanying audio (e.g. MP4 clips) in its Arc-based UI, requiring mod developers to rely on static images or complex sprite sheets for tutorials, feature showcases, changelog teasers, and previews. Providing a lightweight, cross-platform video and audio player component in Solim allows rich motion media to be embedded cleanly across game dialogs and HUDs on both Desktop and Android without requiring heavy native dependencies like FFmpeg.

## What Changes

- **Pure Java Video Decoder**: Add `org.jcodec:jcodec:0.2.5` to `:solim-core` to demux MP4 (ISO BMFF) and decode H.264 baseline video streams without `java.awt` dependencies.
- **Pure Java Audio Decoder**: Decode in-container AAC audio tracks into 16-bit PCM WAV data using jcodec's bundled `org.jcodec.codecs.aac.AACDecoder` (JAAD ships shaded inside `org.jcodec:jcodec`) — pure Java, no `java.awt`.
- **Shared Cache Directory**: Store downloaded `.mp4` files and pre-decoded `<hash>.wav` audio files in the exact same cache directory as `NetworkImage` (`Vars.dataDirectory/solim/cache/networkImage/`) keyed by SHA-256 URL hash.
- **Lucide Icons**: Download and package Lucide icons (`play.png`, `pause.png`, `volume-2.png`, `volume-x.png`) into `assets/icons/` for the playback and audio controls, loaded via `FileIcon`.
- **Pluggable VideoLoader**: Create `solim.display.video.VideoLoader` and `DefaultVideoLoader` with shared disk caching, with a `RequestVideoLoader` adapter registered in `:mod` routing downloads through `mindustrytool.services.Request`.
- **Zero-Allocation GPU Pipeline**: Create `solim.display.video.VideoPlayer` driving background decoding, thread-safe double-buffered Arc `Pixmap` swapping, and in-place OpenGL texture uploads (`glTexSubImage2D`).
- **A/V Master Clock Synchronization**: Drive video frame pacing from `arc.audio.Music` (SoLoud playhead), with automatic fallback to delta-time pacing for silent video clips. Audio is unmuted by default and scaled by Mindustry's SFX volume (`sfxvol`).
- **Interactive Auto-Hide Controls**: Provide an overlay with play/pause toggle, interactive progress scrub bar, timestamp readout (`mm:ss / mm:ss`), replay button, volume toggle, and buffering spinner that auto-hides after 2.5 seconds of inactivity.
- **Configurable Controls Toggle**: Support `.controls(false)` and `.controls(Readable<Boolean>)` to completely omit overlay controls and listeners, enabling clean looping banner and motion graphic use cases.
- **Declarative Solim Facades**: Expose `solim.UI.video(String)` and `solim.UI.video(Readable<String>)` builders with full reactive binding support.

## Capabilities

### New Capabilities
- `solim-video`: Introduces video and audio downloading, shared Solim disk caching, pure Java H.264/AAC decoding, audio-mastered zero-allocation GPU texture streaming, and the `SolimVideo` declarative UI component with configurable auto-hide overlay controls and Lucide icons.
  - **Requirement: Video Loading and Disk Caching**: Fetching via pluggable `VideoLoader` and caching `.mp4` in `Vars.dataDirectory/solim/cache/networkImage/<hash>.mp4`.
  - **Requirement: Cross-Platform Video Decoding Engine**: Background JCodec H.264 decode to direct `ByteBuffer` with zero per-frame heap allocations and `glTexSubImage2D` texture updates.
  - **Requirement: Audio Decoding and Synchronization**: Bundled AAC-decoder AAC-to-WAV decode cached as `<hash>.wav`, played via `arc.audio.Music`, acting as master clock for video frame presentation, unmuted by default and scaled by `sfxvol`.
  - **Requirement: Declarative SolimVideo Component**: `solim.UI.video(...)` with reactive URLs, sizing, rounded masking, autoplay, loop, and automatic disposal.
  - **Requirement: Interactive Auto-Hide Overlay Controls with Lucide Icons**: Auto-hiding control overlay with Lucide icons (`play.png`, `pause.png`, `volume-2.png`, `volume-x.png`, `rotate-ccw.png`), scrub bar, and time display.
  - **Requirement: Configurable Controls Toggle**: Full toggle support (`.controls(boolean)` and `.controls(Readable<Boolean>)`) to suppress controls overlay entirely.

### Modified Capabilities
<!-- None: Solim video is a strictly additive capability to the UI system. -->

## Impact

- **Dependencies**: Adds `org.jcodec:jcodec:0.2.5` to `:solim-core` (pure Java, zero transitive runtime deps, fully compatible with Java 8 and Android D8 desugaring). No separate JAAD dependency — the required AAC decoder ships shaded inside jcodec.
- **Assets**: Adds Lucide icon assets (`play.png`, `pause.png`, `volume-2.png`, `volume-x.png`) to `assets/icons/`.
- **APIs**: Introduces `solim.display.video.*` and adds `solim.UI.video(...)` facades.
- **Cache Footprint**: Reuses `Vars.dataDirectory/solim/cache/networkImage/` alongside `NetworkImage` assets.
- **Mod Runtime**: Wires `RequestVideoLoader` during mod initialization to route video downloads through `mindustrytool.services.Request`.
