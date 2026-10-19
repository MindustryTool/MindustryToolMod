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

The video engine SHALL extract the AAC audio track from the MP4 container and decode it into
16-bit PCM WAV audio using pure Java without `java.awt` (JCodec core's bundled AAC decoder).
The PCM body and its final header SHALL be committed atomically: the engine SHALL write to a
temporary file that carries a `.part` suffix and SHALL rename it to `<cache-name>.wav` only
after the complete decoded body and the rewritten header are both on disk. The temporary file
SHALL be removed only when the transcode is abandoned (decode error, empty decode, header
rewrite failure, or rename failure); it SHALL NOT be removed on the success path. A failed
transcode SHALL be reported as "no usable audio", distinct from a successful transcode that
was served from cache. A cached WAV SHALL be reused only when it passes the cache integrity
rules; a file no larger than a WAV header SHALL never be treated as fresh.

Audio playback SHALL be driven by `arc.audio.Music`, and the engine SHALL treat audio as
optional output rather than as the source of time:

- While the player is `PLAYING`, an accumulated wall-clock delta SHALL be the authoritative
  presentation clock. Pausing and seeking SHALL affect only that clock and the audio position,
  never global time scale.
- `music.getPosition()` MAY be consulted to resynchronise presentation only while the audio
  stream is genuinely advancing. A `Music` instance whose native handle is invalid, whose
  position does not advance, or whose backend is not initialised (`Core.audio.initialized`
  is `false`) SHALL be treated as "no audio" and SHALL NOT stall presentation.
- A `Music` instance SHALL be considered available only after it has been verified as usable
  after construction. If it is not usable it SHALL be disposed, playback SHALL continue
  silently, and no error state SHALL be entered.
- When no audio track exists, when audio is invalid, or when audio ends, frame presentation
  SHALL continue to advance from the wall clock until the end of the clip.

Audio volume SHALL be unmuted by default and scaled by Mindustry's SFX volume setting
(`sfxvol`).

#### Scenario: AAC audio is decoded and persisted in full

- **WHEN** an MP4 containing an AAC audio track is loaded for the first time
- **THEN** the audio track SHALL be decoded to PCM and persisted as a WAV whose file length
  exceeds the 44-byte header by exactly the decoded sample byte count and whose `RIFF` and
  `data` chunk sizes match that byte count

#### Scenario: Temporary file is never mistaken for a cache entry

- **WHEN** the transcode writes the PCM body
- **THEN** the in-progress data SHALL live in a `.part` file that the cache never reads, and
  the `.part` file SHALL be removed only if the transcode is abandoned

#### Scenario: Cached WAV skips audio decoding

- **WHEN** a video is loaded whose audio cache entry exists and passes the cache integrity rules
- **THEN** audio decoding SHALL be bypassed and the existing `.wav` file SHALL be loaded directly

#### Scenario: Header-only WAV is rejected and re-transcoded

- **WHEN** the audio cache entry is a 44-byte header-only file
- **THEN** the entry SHALL be treated as stale, the track SHALL be transcoded again, and the
  resulting file SHALL contain the decoded body

#### Scenario: Video advances while audio is inert

- **WHEN** the audio track exists but the `Music` instance has an invalid native handle, never
  advances, or the audio backend is not initialised
- **THEN** the video SHALL play silently from the wall clock, frame presentation SHALL advance,
  and the player SHALL NOT remain frozen in the `PLAYING` state

#### Scenario: Silent video fallback

- **WHEN** the MP4 contains no audio track
- **THEN** the engine SHALL advance frames using wall-clock delta time without throwing errors

#### Scenario: Audio volume scales with SFX settings

- **WHEN** the player adjusts the game's SFX volume in Mindustry settings
- **THEN** video playback volume SHALL scale proportionally with `sfxvol`

### Requirement: Declarative SolimVideo Component

`SolimVideo` SHALL be a declarative Solim component created via `solim.ui.video(...)` supporting static and reactive source properties (`Readable<String>` and `Readable<VideoSource>`), and declaring its configuration through `.props(...)`: sizing, rounded corner masking, and the media properties `autoplay`, `loop`, `volume`, `muted`, and `controls`. `autoplay`, `duration`-equivalent timing, and the overlay icons SHALL take plain values because they are read once; `loop`, `volume`, `muted`, and `controls` SHALL accept reactive sources. `muted` SHALL accept a `Signal<Boolean>` and bind two-way, so the overlay's mute toggle writes back to the caller's signal, and SHALL route through the component's internal mute state so there is a single source of truth. The component SHALL NOT hold a binding field for its source or its controls: the reactive binding SHALL be created by property application and owned by the component, and a source that changes over time SHALL be declared once as a `Readable` rather than re-declared. The chained setters `autoplay(boolean)`, `loop(boolean)`, `volume(float)`, `muted(boolean)`, `controls(...)`, `url(...)`, `placeholder(...)`, `fallback(...)`, `scaling(...)`, and the overlay-icon methods SHALL NOT exist, including their `Readable` overloads. When the component is disposed, all allocated GL textures, pixel buffers, `Music` instances, and decode worker tasks SHALL be released immediately.

#### Scenario: Declarative creation and playback

- **WHEN** `solim.ui.video("https://.../demo.mp4").props(autoplay(true))` is constructed in a Solim tree
- **THEN** the component SHALL initiate loading and display frames upon download completion

#### Scenario: Reactive URL update

- **WHEN** a reactive source property declared from a `Readable` changes to a new video source
- **THEN** the current playback SHALL stop, previous resources SHALL be cleaned, and the new source SHALL begin loading

#### Scenario: The source binding is owned by the component

- **WHEN** a video is disposed while a reactive source property is still live
- **THEN** the source binding is disposed with the component, and no component-held binding field survives disposal

#### Scenario: Mute toggle writes back

- **WHEN** the video's mute property is bound to a `Signal<Boolean>` and the user clicks the overlay mute button
- **THEN** the signal's value updates to reflect the new mute state

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

`SolimVideo` SHALL support disabling the control overlay through the `controls` property declared in `.props(...)`, taking a plain `boolean` or a `Readable<Boolean>`. When controls are disabled (`false`), the control overlay elements and hover listeners SHALL NOT be displayed, allowing the component to function as an uncluttered looping motion graphic or banner. The reactive form SHALL bind through the property application path and be owned by the component; the component SHALL NOT retain a controls binding field, and the chained `controls(...)` methods, including the `Readable` overload, SHALL NOT exist.

#### Scenario: Disabling controls hides all overlay elements

- **WHEN** `video(url).props(controls(false))` is declared
- **THEN** no play/pause button, scrub bar, volume toggle, or hover overlay SHALL be rendered over the video

#### Scenario: Reactive control visibility toggle

- **WHEN** a reactive `Readable<Boolean>` controls property switches from `true` to `false`
- **THEN** the control overlay SHALL be removed from display immediately

#### Scenario: Controls binding is owned by the component

- **WHEN** a video with a reactive controls property is disposed
- **THEN** the controls binding is disposed with the component, and no controls binding field remains referenced

### Requirement: Video Source Configuration
`SolimVideo` SHALL accept a `VideoSource` representing the source of the video stream, via constructor (`new SolimVideo(VideoSource)`), builder method (`src(VideoSource)`), and declarative property (`Values.src(VideoSource)` or `Values.src(Readable<VideoSource>)`), with convenience overloads for URL strings.

#### Scenario: Video initialized with VideoSource
- **WHEN** a `SolimVideo` is created with `VideoSource.of("http://example.com/test.mp4")`
- **THEN** it SHALL extract the URL and begin loading the video stream

#### Scenario: Video source configured via props
- **WHEN** a `SolimVideo` is configured via `.props(src(VideoSource.of(url)))`
- **THEN** the video component SHALL load and present the specified video source

### Requirement: H.264 Frame Decoding

The engine SHALL decode the demuxed video packets of an MP4 into pictures:

- Packets SHALL be passed to the decoder exactly as the container demuxer produces them. The
  engine SHALL NOT re-apply the AVCC → Annex-B conversion that the demuxer performs while reading
  samples.
- The decoder SHALL be constructed from the container's codec private data, and the destination
  picture buffer SHALL be supplied by the engine: allocated once, from the stream's
  sequence-parameter-set dimensions rounded up to whole macroblocks, and never `null`. The plane
  storage SHALL therefore cover the padded dimensions, independently of the display size.
- A packet that fails to decode SHALL be skipped: it SHALL NOT fail the stream, and decoding
  SHALL continue with the following packet.
- The presented image SHALL be taken over the picture's display crop (the cropped dimensions),
  not the padded macroblock dimensions.

#### Scenario: Every packet of a decodable clip decodes

- **WHEN** an MP4 with a decodable H.264 video track is loaded and demuxed
- **THEN** every demuxed packet SHALL decode into a picture, each picture's crop SHALL describe
  the display frame, and its plane storage SHALL be macroblock aligned

#### Scenario: Decoded packets carry moving content

- **WHEN** consecutive packets of the clip are decoded
- **THEN** the produced luma planes SHALL differ from one another, so the clip is not presented
  as one repeated frame

#### Scenario: Empty packet is not decoded

- **WHEN** the decoder is given no packet data
- **THEN** it SHALL report "no picture" instead of throwing

### Requirement: Frame Color Conversion

The engine SHALL convert a decoded picture to RGBA using JCodec's plane sample convention, in
which every plane sample is stored as a signed byte holding the sample value minus 128. The engine
SHALL recover the sample value from that signed representation before applying the range
conversion (limited range for `YUV420`, full range for `YUV420J`), and SHALL NOT read the stored
bytes as unsigned values.

#### Scenario: Neutral chroma renders as gray

- **WHEN** a picture whose luma and chroma samples are all neutral (stored sample `0`) is converted
- **THEN** every output pixel SHALL be a neutral gray with equal red, green, and blue channels,
  rather than a saturated color

#### Scenario: A decoded clip frame renders with natural colors

- **WHEN** a frame of a decodable H.264 clip is decoded and converted
- **THEN** the output pixels SHALL NOT be dominated by a single saturated channel

### Requirement: End of Clip and Looping

Playback SHALL reach its ended state only when there is nothing left to present:

- A non-looping player SHALL end when the presentation clock reaches the clip duration, or when
  the decode stream is exhausted and no frame remains queued for presentation.
- A looping player SHALL restart the loop when the decode stream is exhausted, resetting the
  presentation clock to 0 and repositioning the audio, and SHALL NOT enter the ended state while
  the clip is still delivering frames.
- A looping player SHALL end rather than restart when a complete pass over the stream produced no
  frame, so an undecodable stream cannot be reseeked indefinitely.

#### Scenario: Looping clip restarts instead of ending

- **WHEN** a looping clip reaches the end of its stream while frames were produced
- **THEN** the stream SHALL be re-sought from the start, the presentation clock SHALL return to 0,
  and the player SHALL remain in the playing state

#### Scenario: Undecodable looping clip terminates

- **WHEN** a looping clip is played but no packet of a full pass produces a picture
- **THEN** the player SHALL enter the ended state instead of seeking to the start again

#### Scenario: Non-looping clip ends at the end of the stream

- **WHEN** a non-looping clip is exhausted and no frame remains queued
- **THEN** the player SHALL enter the ended state and notify its listener

### Requirement: Video Cache Integrity

The disk cache SHALL validate an artifact before treating it as fresh. Validation SHALL
distinguish media kind:

- A video entry SHALL exist, not be a directory, and be at least large enough to contain a
  container header; an artifact that cannot be a container SHALL be stale.
- An audio entry SHALL exist, not be a directory, and be strictly larger than a WAV header.
- Cache entries SHALL be written atomically: the downloader SHALL write to a `.part` file and
  rename it into place, so an interrupted write cannot leave a truncated entry that later
  passes validation.
- The audio cache key SHALL carry a version segment, so an entry written by an older encoding
  of the pipeline is never read after an incompatible change.

#### Scenario: Truncated video entry is refetched

- **WHEN** the cached MP4 for a URL is smaller than a valid container header or is a directory
- **THEN** the loader SHALL treat it as stale and fetch the video again

#### Scenario: Interrupted download is not committed

- **WHEN** a download is interrupted before completion
- **THEN** no file at the final cache path SHALL be created or replaced

#### Scenario: Legacy audio entry is ignored

- **WHEN** an audio file written under a previous cache-key version exists on disk
- **THEN** it SHALL NOT be read, and the audio SHALL be transcoded under the current key

### Requirement: Playback Diagnostics

A successful playback SHALL NOT emit error-level output. JCodec's logger SHALL be routed into
Arc's logging so that its container-parse messages are reported at debug level, including the
known-benign `Broken atom of size 0` emitted for MP4 files whose `ilst` metadata contains
zero-filled entries. Logger installation SHALL happen once, SHALL tolerate being installed
after JCodec has already logged (Arc/JCodec `Logger already started` state), and SHALL NOT
change playback behaviour or failure reporting if it cannot be installed.

#### Scenario: Healthy playback is quiet

- **WHEN** a valid MP4 with an AAC track plays to the end
- **THEN** no `[ERROR]` line is written to the console or log

#### Scenario: Benign container-parse message is demoted

- **WHEN** JCodec reports `Broken atom of size 0` while parsing a metadata `ilst` entry
- **THEN** the message SHALL be recorded at debug level and playback SHALL continue normally

#### Scenario: Genuine container failure still surfaces

- **WHEN** JCodec cannot parse a video's movie box
- **THEN** the player SHALL still reach its failed state and notify its listener, independent of
  the log routing

### Requirement: Overlay Layout Within the Video Surface

The control overlay SHALL lay its controls out inside the video surface at every size the component
is deployed at, without overlapping controls: the scrub bar SHALL own a full-width row, and the
transport controls SHALL sit in a separate row. A control SHALL NOT be drawn outside the surface,
and controls SHALL NOT be squeezed below the width they need, with the timestamp readout as the
flexible cell of its row — allowed to shrink and truncate rather than push the row wider than the
surface.

#### Scenario: Controls stay inside the surface without overlapping

- **WHEN** the overlay is laid out in video surfaces at and below the size the help dialog gives it
  (320×180, 240×136, 160×88)
- **THEN** every control SHALL lie inside the surface and no two sibling controls SHALL overlap

#### Scenario: The timestamp readout stays legible while there is room

- **WHEN** the overlay is laid out in a surface 320×180 or wider
- **THEN** the timestamp readout SHALL be given at least the width of its own text

