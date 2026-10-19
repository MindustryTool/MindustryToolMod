## Context

Mindustry's `MenuRenderer` handles drawing the main menu background. `BackgroundFeature` replaces `Vars.ui.menufrag.renderer` with a `CustomMenuRenderer`. Currently, it only loads static images (`arc.graphics.Texture`).

`solim-core` already contains a complete video subsystem (`solim.display.video.VideoPlayer`, `AacToWav`, `YuvToRgbaConverter`) utilizing JCodec for pure-Java H.264 video decoding and AAC audio transcoding into WAV. This proposal extends `BackgroundFeature` to support MP4 video files seamlessly alongside images.

## Goals / Non-Goals

**Goals:**
- Play looping H.264 MP4 videos as the main menu background.
- Support both "Crop to Fill" (aspect-ratio preserving) and "Stretch" scaling modes for both video and static images.
- Provide audio playback from video with Mindustry menu music suppression when video audio is active.
- Fully dispose video decoders and textures when entering games/editors (`!Vars.state.isMenu()`) to guarantee zero CPU/GPU overhead during gameplay.
- Maintain full backward compatibility for static image files (`png`, `jpg`, `jpeg`).

**Non-Goals:**
- Supporting non-H.264 video codecs (e.g., HEVC, AV1, VP9) because JCodec does not support them in Java 8 without heavy native dependencies.
- In-game video backgrounds (background feature is strictly for the main menu).
- Interactive video scrubbing in the main menu (video loops continuously as background).

## Decisions

### 1. Engine: Use `solim.display.video.VideoPlayer`
- **Choice**: Leverage `solim.display.video.VideoPlayer` rather than bundling new libraries or external players.
- **Rationale**: `VideoPlayer` is already implemented, Java 8 compatible, tested, and exposes GL `Texture` updates via `.update(delta)`.
- **Alternatives Considered**:
  - *Native VLCJ / GStreamer*: Rejected due to cross-platform binary dependencies and Android incompatibility.
  - *GIF animations*: High RAM consumption and poor quality compared to H.264 MP4.

### 2. Audio Coordination & Menu Music
- **Choice**: Video audio is active by default. When active on the main menu, Mindustry's ambient menu music is silenced. When leaving the menu or if video volume is set to 0%, Mindustry music resumes.
- **Rationale**: Playing both video audio and game menu soundtrack simultaneously creates jarring conflicting audio.
- **Alternatives Considered**:
  - *Always mute video*: Disappoints users who intentionally pick video wallpapers with audio (cinematics, music videos).
  - *Mute Mindustry permanently*: Confusing when switching between menu and game.

### 3. Scaling Modes: Crop to Fill vs Stretch
- **Choice**: Add `scalingConfig` (`"crop"` | `"stretch"`, default `"crop"`).
  - Crop to Fill: Computes `scale = Math.max(screenWidth / textureWidth, screenHeight / textureHeight)`, rendering `(textureWidth * scale, textureHeight * scale)` centered. Excess edges are cropped outside the viewport.
  - Stretch: Uses `(screenWidth, screenHeight)` directly.
- **Rationale**: High-resolution videos look distorted if stretched across mismatched aspect ratios (e.g., 16:9 video on 21:9 ultrawide or mobile portrait screens).

### 4. Lifecycle: Full Teardown Outside Menu
- **Choice**: Hook game state change events (`EventType.StateChangeEvent`, `Trigger.update`). When `!Vars.state.isMenu()`, call `dispose()` on the active video renderer and nullify the player. Upon returning to menu (`Vars.state.isMenu()`), re-invoke `applyBackground()`.
- **Rationale**: Satisfies the project's strict Performance & Memory Discipline rule ("Disabled features perform no work", zero allocation frame loops). Decoding video in the background during gameplay would cause noticeable frame drops and battery drain.

### 5. Media Detection
- **Choice**: Inspect file extension. If `.mp4`, route to `VideoMenuRenderer`. Otherwise route to static `ImageMenuRenderer`.
- **Rationale**: Simple, deterministic, and fast without blocking I/O probe.

### 6. Replacing the Default Menu Renderer
- **Choice**: A video background takes over the menu renderer completely: `VideoMenuRenderer` is constructed with no default renderer to blend into, and the captured `MenuRenderer` is disposed while the video plays, rebuilt only when the default background becomes needed again. Image backgrounds keep the existing blend behaviour. The `MenuRenderer` subclass releases the 3D menu cache and framebuffer its own constructor generates, because custom backgrounds override `render()` and never use them.
- **Rationale**: Keeping the default 3D menu scene alive under a video wastes GPU memory and per-frame cost. Releasing the inherited resources also fixes a leak of the default menu cache on every applied background.
- **Alternatives Considered**:
  - *Blending video over the default menu*: The video is a full-screen background; blending produced a visible mismatch and extra draw cost.
  - *Keeping the default renderer alive*: Cheaper on menu re-entry, but holds the GL resources the video is meant to replace.

### 7. Decoding: Native First, JCodec Fallback
- **Choice**: Decoding sits behind `VideoFrameDecoder`. `VideoDecoders` prefers a platform decoder (`MediaFoundationVideoDecoder` on Windows) and falls back to `JCodecFrameDecoder` when native is unavailable, unsupported, or fails. `VideoDecoders.Preference` (default `AUTO`) and the `solim.video.decoder=software` system property force software decoding. A native decoder that fails mid-stream is reopened as a JCodec decoder at the last position. The Windows decoder is implemented on Media Foundation's source reader through the Foreign Function & Memory API (`java.lang.foreign`), reached reflectively so the mod still compiles against the Java 17 toolchain.
- **Rationale**: The pure-Java JCodec decoder cannot keep up with 1440p (measured ≈4.5 fps), while the operating-system decoder does. Keeping the fallback means every platform still works, and the abstraction keeps the platform code out of `VideoPlayer`. JNA was the original binding, but its `Native`, `NativeLibrary` and `Structure` classes require `java.util.logging`, which Mindustry's shipped Windows runtime (a jlink image of `java.base` and `jdk.unsupported`) omits, so JNA cannot initialize there. The Foreign Function & Memory API lives in `java.base` and works in that runtime. Native decoding therefore requires a JDK 22+ runtime (all current Mindustry desktop builds ship JDK 25); on older runtimes and on Android the JCodec fallback applies.
- **Alternatives Considered**:
  - *JCodec only*: Unusable above 720p30.
  - *JNA for the Windows binding*: Rejected; see the rationale above.
  - *Bundling native codec binaries or a JNI shim*: Rejected; not mod-compatible, adds per-platform binary weight, and requires a native build toolchain.
  - *Binding MediaCodec through reflection to avoid `android.jar`*: Compiles everywhere, but cannot be exercised here and would ship unverified code.

### 8. Media Foundation Output Format: NV12, Not RGB32
- **Choice**: Request NV12 from the source reader and convert to RGBA in `Nv12ToRgbaConverter` rather than requesting RGB32 and letting the video processor convert.
- **Rationale**: The in-box RGB32 conversion path is a software video processor and measured ≈52 ms/frame at 1440p, capping playback at ≈15 fps. Requesting NV12 makes the decode step ≈5.4 ms/frame (≈185 fps); the remaining cost is our own conversion, which is table-driven and split across a small worker pool owned by the decoder.
- **Alternatives Considered**:
  - *RGB32 through the video processor*: Simplest, but the conversion dominated the frame budget.
  - *NV12 on the GPU via a shader*: Avoids CPU conversion, but Arc's `SpriteBatch` has no per-draw shader hook, so this would mean bypassing the established render path.
- **Notes**: The reader reports coded (macroblock-aligned) dimensions in `MF_MT_FRAME_SIZE` and display dimensions in `MF_MT_GEOMETRIC_APERTURE`; the decoder reports display dimensions and crops during conversion. `MF_SOURCE_READER_ENABLE_ADVANCED_VIDEO_PROCESSING` must not be set on this machine — the reader fails with `E_INVALIDARG` when it is. Media Foundation needs COM initialized on the calling thread.

### 9. Android MediaCodec: Deferred
- **Choice**: Ship the platform decoder interface with only the Windows implementation; Android keeps using the JCodec fallback until an Android MediaCodec provider is added.
- **Rationale**: MediaCodec requires `android.jar` at compile time and a device or emulator to validate, neither of which is available in the current development environment. Adding the interface now means the Android provider can be dropped in later without touching `VideoPlayer` or the selection logic.
- **Alternatives Considered**:
  - *Binding MediaCodec through reflection to avoid `android.jar`*: Compiles everywhere, but cannot be exercised here and would ship unverified code.


## Risks / Trade-offs

- **[Risk] High-resolution video (e.g. 4K 60fps) causing high CPU utilization in pure-Java JCodec.**
  - *Mitigation*: Prefer the platform decoder; JCodec remains only the fallback. Measured on a Ryzen 5 5600H with a 2560×1440 H.264 clip: JCodec ≈4.5 fps, Media Foundation ≈44 fps (NV12 decode ≈5.4 ms/frame plus a parallel RGBA conversion). When only JCodec is available, highlight in the UI description that 1080p or 720p 30fps is recommended for smooth software decoding.
- **[Risk] Native decoding requires a JDK 22+ runtime.**
  - *Mitigation*: The mod compiles against Java 17, and `ForeignRuntime` reaches the Foreign Function & Memory API reflectively, so it is absent only on older runtimes. On those runtimes, and on Android, `VideoDecoders.isNativeAvailable()` returns false and the JCodec fallback is used. The native tests in `VideoDecoderSelectionTest` are guarded by `assumeTrue(VideoDecoders.isNativeAvailable())` and therefore skip on a Java 17 test runtime.
- **[Risk] Native access is restricted by default on JDK 25 (JEP 472).**
  - *Mitigation*: `System.loadLibrary` and `MemorySegment.reinterpret` emit a warning on first use but still work. The mod cannot add `--enable-native-access=ALL-UNNAMED` itself, so a future JDK that blocks restricted access instead of warning would disable native decoding; the JCodec fallback would then take over. No user-visible failure occurs either way.

- **[Risk] Unsupported video codecs (HEVC, AV1).**
  - *Mitigation*: Catch `IllegalStateException` during demuxing in `VideoPlayer.Listener.onFailed`, show a clear translatable dialog explaining that H.264 AVC is required, and restore the original renderer.
- **[Risk] Slight delay when returning to main menu due to re-opening video.**
  - *Mitigation*: Loading happens asynchronously on `DECODE_WORKER`. A video fully replaces the default menu renderer, so while the first frame is still decoding the menu shows the cleared background colour and playback resumes automatically once ready.
