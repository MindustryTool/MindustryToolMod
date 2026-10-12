## Context

Solim UI is the declarative, reactive UI system for Mindustry mods running on Java 8 (JVM desktop) and Android (via D8 desugaring). While Solim provides `NetworkImage` for static raster assets, dynamic video content with synchronized audio (e.g. tutorials, feature demos, changelog teasers, schematic demonstrations) currently has no native engine support in Arc or Mindustry.

Adding video and audio capability requires addressing cross-platform container demuxing, video and audio stream decoding, efficient OpenGL texture updates, SoLoud audio playback, and clean UI integration without violating Android compatibility constraints (e.g. absence of `java.awt` and restrictive dynamic JNI library loading).

## Goals / Non-Goals

**Goals:**
- Provide a pure Java MP4 / H.264 baseline video decoder using `org.jcodec:jcodec` that functions identically on both Desktop and Android without `java.awt`.
- Provide a pure Java AAC audio decoder via jcodec's bundled `org.jcodec.codecs.aac.AACDecoder` to decode in-container audio tracks into 16-bit PCM WAV data.
- Ensure zero-allocation frame updates during playback to eliminate garbage collection hitching.
- Synchronize video frame presentation to the audio playhead (`arc.audio.Music.getPosition()`), with automatic fallback to delta-time pacing for silent video clips.
- Share the exact same disk cache directory as `NetworkImage` (`Vars.dataDirectory/solim/cache/networkImage`), caching both the downloaded `.mp4` and the pre-decoded `.wav` file by SHA-256 URL hash.
- Implement a declarative `SolimVideo` leaf component (`solim.UI.video(url)`) with support for size, aspect ratio, rounded corners, autoplay, looping, and audio volume scaled to Mindustry's SFX channel (`sfxvol`).
- Provide an auto-hide overlay control bar featuring Lucide icons (`play.png`, `pause.png`, `volume-2.png`, `volume-x.png`, `rotate-ccw.png`), interactive progress scrub bar, elapsed/total time readout, replay button, volume/mute toggle, and a buffering indicator.
- Support disabling controls entirely (`.controls(false)` or `.controls(Readable<Boolean>)`) so the component can function as an uncluttered looping motion graphic or banner.
- Implement an injectable `VideoLoader` with transparent disk caching, allowing `:mod` to route network fetching through `mindustrytool.services.Request`.

**Non-Goals:**
- Native hardware decoding via FFmpeg, MediaCodec, or JNI binaries.
- High-definition 1080p60/4K decoding (the pure Java software pipeline targets clips < 1 min at 240p–480p).
- Live adaptive streaming protocols (HLS, DASH, RTSP).

## Decisions

### Decision 1: Pure Java JCodec Core Only, No AWT
- **Choice**: Depend on `org.jcodec:jcodec:0.2.5` core directly in `:solim-core` (excluding `jcodec-javase`) and nothing else. AAC decoding uses jcodec's `org.jcodec.codecs.aac.AACDecoder`.
- **Rationale**: `jcodec-javase` depends on `java.awt.image.BufferedImage`, which does not exist on Android and causes immediate `NoClassDefFoundError` crashes. JCodec core compiles to Java 8 bytecode, references no `java.awt`/`javax.media`/`java.util.function` types, and desugars smoothly via D8 for Android API 14+. It also has **zero transitive runtime dependencies** and already **shades JAAD in** under `net.sourceforge.jaad.aac.*` (wrapped by `org.jcodec.codecs.aac.AACDecoder`), so a separate JAAD artifact is redundant. Adding one would also put duplicate `net.sourceforge.jaad.aac.*` classes on the classpath.
- **Alternatives Considered**:
  - *Standalone JAAD fork (`com.tianscar.javasound:jaad:0.9.4`, `com.gitlab.9lukas5:jaad:0.8.8`, `de.sfuhrm:jaad:0.8.7`)*: Duplicate classes against jcodec's shaded copy, and the 0.9.x fork dropped the original high-level `Decoder`/`AudioFrame`/`MediaManager` wrappers. Rejected.
  - *`net.sourceforge.jaadec:jaad`*: Not published to Maven Central — the coordinate only ever existed on JCenter, which shut down in 2021. Unresolvable.
  - *JavaCV / FFmpeg native binaries*: Exceeds 50MB, causes architecture fragmentation (x86_64, arm64-v8a, armeabi-v7a), and fails on Android under Mindustry's `ModClassLoader`.
  - *Android `MediaExtractor`/`MediaCodec`*: Desktop/Android behavior diverges, defeating the identical-cross-platform goal.
  - *External sidecar audio files*: Requires hosting two separate files (`.mp4` and `.ogg`) instead of a self-contained MP4.

### Decision 2: Zero-Allocation YUV-to-RGBA Pipeline & In-Place Texture Uploads
- **Choice**: The decoder background thread converts JCodec `Picture` YUV420p byte planes directly into a pre-allocated direct `ByteBuffer` backing a reusable Arc `Pixmap`. The render thread calls `Texture.draw(frontPixmap, 0, 0)` which invokes OpenGL's `glTexSubImage2D`.
- **Rationale**: Reallocating GL textures or `Pixmap` instances at 24–30 FPS causes severe heap churn and GPU resource leaks. A double-buffered ping-pong pair of `Pixmap` instances decouples background decoding from main-thread rendering without allocations.

### Decision 3: Audio Transcoding to Disk-Cached WAV and Native SoLoud Playback
- **Choice**: On initial video download, the AAC audio track is extracted and decoded to 16-bit PCM by `org.jcodec.codecs.aac.AACDecoder` on a background worker, prepended with a standard 44-byte WAV header, and saved to disk as `<hash>.wav` in the shared cache directory. Playback is driven natively by Arc's `arc.audio.Music` backed by SoLoud.
- **Rationale**: Mindustry's audio engine (SoLoud) natively supports WAV with hardware mixing, pitch/volume scaling, and precise playhead reporting (`getPosition()`). Caching the decoded `.wav` on disk ensures repeat views never repeat the AAC decoding step.

### Decision 4: Master Audio Clock Synchronization with Silent Fallback
- **Choice**: When audio is present, `music.getPosition()` serves as the master clock. If video frame PTS lags behind `music.getPosition() - 30ms`, non-reference frames are dropped to catch up; if video is ahead, texture updates pause until audio catches up. If the video has no audio track, the engine automatically falls back to game delta-time pacing.
- **Rationale**: Prevents audio crackle and drift while preserving synchronization across varying system loads.

### Decision 5: Shared Disk Cache Directory with NetworkImage
- **Choice**: Store cached `.mp4` and `.wav` files directly in `Vars.dataDirectory.child("solim").child("cache").child("networkImage")`, using SHA-256 hashing.
- **Rationale**: Consolidates Solim's disk cache footprint in one unified directory managed by existing cache maintenance and cleanup policies.

### Decision 6: Auto-Hide Overlay Controls with Lucide Icons
- **Choice**: Use Lucide icons (`play.png`, `pause.png`, `volume-2.png`, `volume-x.png`, `rotate-ccw.png`) loaded via `FileIcon`. The overlay automatically fades out after 2.5 seconds of user inactivity during playback. Calling `.controls(false)` omits the overlay completely.
- **Rationale**: Maintains visual consistency with MindustryTool UI tokens and WebStyles.

## Risks / Trade-offs

- **[Risk] High resolution pure Java decoding CPU overhead**  
  → *Mitigation*: Run demuxing and decoding strictly on a daemon worker pool (`solim-video-decode`); document recommended clip specs (360p or 480p baseline H.264, 24–30 fps).
- **[Risk] Audio decode latency on initial play**  
  → *Mitigation*: For `< 1 min` clips, the AAC track decodes to WAV in ~50–150ms on a background thread; once saved as `<hash>.wav`, all future plays load instantly.
- **[Risk] Volume spikes**  
  → *Mitigation*: Audio volume is clamped and scaled by Mindustry's SFX volume setting (`Core.settings.getInt("sfxvol") / 100f`).
- **[Risk] Resource leaks on dialog closure**  
  → *Mitigation*: `SolimVideo` implements `Disposable` and hooks into Solim's automatic component lifecycle ownership, terminating background decode workers, stopping `Music`, and releasing OpenGL texture handles upon disposal.
