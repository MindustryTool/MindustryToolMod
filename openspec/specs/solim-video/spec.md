## Purpose

Behavior contract for `solim.display.video.SolimVideo` — the declarative video component with pluggable loader, shared disk caching, pure-Java H.264/AAC decoding, audio-mastered texture streaming, and auto-hide overlay controls. Established as a behavior baseline by change `solim-video-component`.

## Requirements

### Requirement: Video Loading and Disk Caching
The video subsystem SHALL load MP4 video files through an injectable `VideoLoader` interface (`load(String url, Cons<Fi> onSuccess, Cons<Throwable> onError)`). The default loader SHALL download the file over HTTP and store it in the shared Solim disk cache directory at `Vars.dataDirectory/solim/cache/networkImage/` using SHA-256 URL hashing (`<hash>.mp4`). Subsequent loads of the same URL SHALL be served directly from disk. `SolimVideo.setVideoLoader(loader)` SHALL allow injecting a custom loader, and passing `null` SHALL reset to the default loader.

#### Scenario: Remote video is downloaded and cached
- **WHEN** a video URL is requested and no local cache entry exists
- **THEN** the loader SHALL download the file, persist it to `Vars.dataDirectory/solim/cache/networkImage/<hash>.mp4`, and notify the success callback with the cached `Fi`

#### Scenario: Cached video bypasses network
- **WHEN** a video URL is requested whose file already exists and is non-empty in the disk cache
- **THEN** the loader SHALL immediately return the cached file without performing a network request

#### Scenario: Custom loader receives load requests
- **WHEN** a custom `VideoLoader` is registered via `SolimVideo.setVideoLoader`
- **THEN** all subsequent video load requests SHALL be dispatched to the custom loader

### Requirement: Cross-Platform Video Decoding Engine
The video playback engine (`VideoPlayer`) SHALL decode MP4 (ISO BMFF container) and H.264 baseline video streams using pure Java without depending on `java.awt`. Decoding SHALL run asynchronously on a background worker thread. The engine SHALL maintain a reusable double-buffered frame pipeline, converting decoded YUV420p frames directly into an Arc `Pixmap` backed by a direct `ByteBuffer` with zero heap allocations per frame during playback.

#### Scenario: Asynchronous background frame decoding
- **WHEN** the video engine is playing a loaded clip
- **THEN** bitstream parsing and pixel conversion SHALL execute on a background worker thread without blocking the Mindustry render thread

#### Scenario: In-place GPU texture updates
- **WHEN** the next frame is ready for presentation
- **THEN** the front buffer SHALL be uploaded to the reusable OpenGL texture via `glTexSubImage2D`

#### Scenario: Seamless playback looping
- **WHEN** playback reaches the end of the video stream and `loop(true)` is enabled
- **THEN** the decoder and audio playhead SHALL seek back to the beginning and continue playback continuously

### Requirement: Audio Decoding and Synchronization
The video engine SHALL extract the AAC audio track from the MP4 container and decode it into 16-bit PCM WAV audio using pure Java without `java.awt` (JCodec core's bundled AAC decoder). The decoded audio SHALL be persisted as `<hash>.wav` in the shared disk cache directory. Audio playback SHALL be driven by `arc.audio.Music`. When an audio track is present, `music.getPosition()` SHALL serve as the master clock for video frame presentation; if no audio track exists, the engine SHALL automatically fall back to game delta-time pacing. Audio volume SHALL be unmuted by default and scaled by Mindustry's SFX volume setting (`sfxvol`).

#### Scenario: AAC audio is decoded and cached as WAV
- **WHEN** an MP4 containing an AAC audio track is loaded for the first time
- **THEN** the audio track SHALL be decoded to PCM, saved as `<hash>.wav` in `Vars.dataDirectory/solim/cache/networkImage/`, and loaded into `arc.audio.Music`

#### Scenario: Cached WAV skips audio decoding
- **WHEN** a video is loaded whose `<hash>.wav` already exists in the disk cache
- **THEN** audio decoding SHALL be bypassed and the existing `.wav` file SHALL be loaded directly

#### Scenario: Video frames synchronize to audio master clock
- **WHEN** audio is playing
- **THEN** video frame presentation timestamps SHALL align with `music.getPosition()`, dropping lagging frames and holding early frames

#### Scenario: Audio volume scales with SFX settings
- **WHEN** the player adjusts the game's SFX volume in Mindustry settings
- **THEN** video playback volume SHALL scale proportionally with `sfxvol`

#### Scenario: Silent video fallback
- **WHEN** the MP4 contains no audio track
- **THEN** the engine SHALL advance frames using game delta time without throwing errors

### Requirement: Declarative SolimVideo Component
`SolimVideo` SHALL be a declarative Solim component created via `solim.UI.video(...)` supporting static and reactive URL sources (`Readable<String>`), sizing, rounded corner masking, autoplay (`autoplay(boolean)`), looping (`loop(boolean)`), and volume control. When the component is disposed, all allocated GL textures, pixel buffers, `Music` instances, and decode worker tasks SHALL be released immediately.

#### Scenario: Declarative creation and playback
- **WHEN** `solim.UI.video("https://.../demo.mp4")` is constructed in a Solim tree with `autoplay(true)`
- **THEN** the component SHALL initiate loading and display frames upon download completion

#### Scenario: Reactive URL update
- **WHEN** a reactive `Readable<String>` URL changes to a new video URL
- **THEN** the current playback SHALL stop, previous resources SHALL be cleaned, and the new video SHALL begin loading

#### Scenario: Component disposal releases resources
- **WHEN** a `SolimVideo` component is removed from the active UI hierarchy and disposed
- **THEN** the underlying `VideoPlayer`, `Music`, GL texture, and background decode threads SHALL be cleanly stopped and disposed

### Requirement: Interactive Auto-Hide Overlay Controls with Lucide Icons
`SolimVideo` SHALL provide an interactive control overlay using Lucide icons (`play.png`, `pause.png`, `volume-2.png`, `volume-x.png`, `rotate-ccw.png` loaded via `FileIcon`), featuring a play/pause toggle button, an interactive progress scrub bar, an elapsed and total timestamp readout (`mm:ss / mm:ss`), a replay button, and a mute/unmute volume toggle. The overlay SHALL display upon cursor hover or touch input and SHALL automatically fade out after 2.5 seconds of user inactivity while the video is playing.

#### Scenario: Play and pause toggle
- **WHEN** the user clicks the play/pause button on the overlay
- **THEN** video and audio playback SHALL alternate between paused and playing states, and the button icon SHALL switch between `play.png` and `pause.png`

#### Scenario: Seeking via scrub bar
- **WHEN** the user interacts with the progress scrub bar
- **THEN** both video and audio playheads SHALL seek to the corresponding normalized position in the video

#### Scenario: Volume mute toggle
- **WHEN** the user clicks the volume button
- **THEN** audio playback SHALL toggle between muted and unmuted states, and the icon SHALL toggle between `volume-2.png` and `volume-x.png`

#### Scenario: Auto-hide after inactivity
- **WHEN** the video is playing and no hover or input events occur for 2.5 seconds
- **THEN** the control overlay SHALL smoothly fade out from view

### Requirement: Configurable Controls Toggle
`SolimVideo` SHALL support disabling the control overlay via `.controls(boolean)` and `.controls(Readable<Boolean>)`. When controls are disabled (`false`), the control overlay elements and hover listeners SHALL NOT be displayed, allowing the component to function as an uncluttered looping motion graphic or banner.

#### Scenario: Disabling controls hides all overlay elements
- **WHEN** `video(url).controls(false)` is specified
- **THEN** no play/pause button, scrub bar, volume toggle, or hover overlay SHALL be rendered over the video

#### Scenario: Reactive control visibility toggle
- **WHEN** a reactive `Readable<Boolean>` control signal switches from `true` to `false`
- **THEN** the control overlay SHALL be removed from display immediately
