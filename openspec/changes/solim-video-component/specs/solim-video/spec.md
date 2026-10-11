## ADDED Requirements

### Requirement: Video Loading and Disk Caching
The video subsystem SHALL load MP4 video files through an injectable `VideoLoader` interface (`load(String url, Cons<Fi> onSuccess, Cons<Throwable> onError)`). The default loader SHALL download the file over HTTP, store it in the local disk cache directory under `cache/videos/`, and serve subsequent loads from the cached file. `SolimVideo.setVideoLoader(loader)` SHALL allow injecting a custom loader, and passing `null` SHALL reset to the default loader.

#### Scenario: Remote video is downloaded and cached
- **WHEN** a video URL is requested and no local cache entry exists
- **THEN** the loader SHALL download the file, persist it to disk cache, and notify the success callback with the cached `Fi`

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

#### Scenario: Frame pacing matches presentation timestamps
- **WHEN** game delta time accumulates to the next frame timestamp
- **THEN** the front buffer SHALL be updated and uploaded to the GPU texture via `glTexSubImage2D`

#### Scenario: Seamless playback looping
- **WHEN** playback reaches the end of the video stream and `loop(true)` is enabled
- **THEN** the decoder SHALL seek back to the beginning and continue playback continuously

### Requirement: Declarative SolimVideo Component
`SolimVideo` SHALL be a declarative Solim component created via `solim.UI.video(...)` supporting static and reactive URL sources (`Readable<String>`), sizing, rounded corner masking, autoplay (`autoplay(boolean)`), and looping (`loop(boolean)`). When the component is disposed, all allocated GL textures, pixel buffers, and decode worker tasks SHALL be released immediately.

#### Scenario: Declarative creation and playback
- **WHEN** `solim.UI.video("https://.../demo.mp4")` is constructed in a Solim tree with `autoplay(true)`
- **THEN** the component SHALL initiate loading and display frames upon download completion

#### Scenario: Reactive URL update
- **WHEN** a reactive `Readable<String>` URL changes to a new video URL
- **THEN** the current playback SHALL stop and the new video SHALL begin loading

#### Scenario: Component disposal releases resources
- **WHEN** a `SolimVideo` component is removed from the active UI hierarchy and disposed
- **THEN** the underlying `VideoPlayer`, GL texture, and background decode threads SHALL be cleanly stopped and disposed

### Requirement: Interactive Auto-Hide Overlay Controls
`SolimVideo` SHALL provide an interactive control overlay featuring a play/pause toggle button, an interactive progress scrub bar, an elapsed and total timestamp readout (`mm:ss / mm:ss`), and a replay button. The overlay SHALL display upon cursor hover or touch input and SHALL automatically fade out after 2.5 seconds of user inactivity while the video is playing.

#### Scenario: Play and pause toggle
- **WHEN** the user clicks the play/pause button on the overlay
- **THEN** video playback SHALL alternate between paused and playing states, and the button icon SHALL reflect the current state

#### Scenario: Seeking via scrub bar
- **WHEN** the user interacts with the progress scrub bar
- **THEN** playback SHALL seek to the corresponding normalized position in the video

#### Scenario: Auto-hide after inactivity
- **WHEN** the video is playing and no hover or input events occur for 2.5 seconds
- **THEN** the control overlay SHALL smoothly fade out from view

### Requirement: Configurable Controls Toggle
`SolimVideo` SHALL support disabling the control overlay via `.controls(boolean)` and `.controls(Readable<Boolean>)`. When controls are disabled (`false`), the control overlay elements and hover listeners SHALL NOT be displayed, allowing the component to function as an uncluttered looping motion graphic or banner.

#### Scenario: Disabling controls hides all overlay elements
- **WHEN** `video(url).controls(false)` is specified
- **THEN** no play/pause button, scrub bar, or hover overlay SHALL be rendered over the video

#### Scenario: Reactive control visibility toggle
- **WHEN** a reactive `Readable<Boolean>` control signal switches from `true` to `false`
- **THEN** the control overlay SHALL be removed from display immediately
