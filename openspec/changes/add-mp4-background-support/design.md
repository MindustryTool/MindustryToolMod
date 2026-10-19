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

## Risks / Trade-offs

- **[Risk] High-resolution video (e.g. 4K 60fps) causing high CPU utilization in pure-Java JCodec.**
  - *Mitigation*: Highlight in UI description that 1080p or 720p 30fps is recommended for smooth software decoding.
- **[Risk] Unsupported video codecs (HEVC, AV1).**
  - *Mitigation*: Catch `IllegalStateException` during demuxing in `VideoPlayer.Listener.onFailed`, show a clear translatable dialog explaining that H.264 AVC is required, and restore the original renderer.
- **[Risk] Slight delay when returning to main menu due to re-opening video.**
  - *Mitigation*: Loading happens asynchronously on `DECODE_WORKER`. A video fully replaces the default menu renderer, so while the first frame is still decoding the menu shows the cleared background colour and playback resumes automatically once ready.
