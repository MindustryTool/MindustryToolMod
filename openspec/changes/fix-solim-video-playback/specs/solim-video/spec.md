## MODIFIED Requirements

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

## ADDED Requirements

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

The control overlay SHALL lay its controls out inside the video surface at the size the component
is deployed at, without overlapping controls: the scrub bar SHALL own a full-width row, and the
transport controls SHALL sit in a separate row. A control SHALL NOT be drawn outside the surface,
and controls SHALL NOT be squeezed below the width they need, with the timestamp readout as the
flexible cell of its row and allowed to truncate rather than overlap its neighbours.

#### Scenario: Controls stay inside the surface without overlapping

- **WHEN** the overlay is laid out in a video surface of the size the mod deploys it at (320×180
  device-independent pixels)
- **THEN** every control SHALL lie inside the surface and no two sibling controls SHALL overlap

#### Scenario: The timestamp readout stays legible

- **WHEN** the overlay is laid out at the deployed size
- **THEN** the timestamp readout SHALL be given at least the width of its own text
