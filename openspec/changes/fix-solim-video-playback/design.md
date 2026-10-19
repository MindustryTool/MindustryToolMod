## Context

`SolimVideo` is the declarative video component (`solim-core`, `solim.display.video`). It
loads an MP4 through `VideoLoader`/`VideoCache`, demuxes it with JCodec 0.2.5, decodes H.264
on a background executor, converts YUV420p to RGBA into a reusable direct `ByteBuffer`, and
uploads it into an Arc `Texture`. Audio is transcoded once from the container's AAC track into
a cached 16-bit PCM WAV and played through `arc.audio.Music`.

The runtime is Mindustry 160.7, where Arc's audio backend is SoLoud/MiniAudio (verified from
`last_log.txt`: `[Audio] Initialized SoLoud 20260903 using MiniAudio`), not OpenAL. Arc's
`Music` is a thin wrapper over a native stream handle:

```java
// javap -c arc.audio.Music (Mindustry-v160.7.jar)
Music.load(Fi)      -> handle = Soloud.streamLoadFile(path)   // wrapped in ArcRuntimeException on throw
Music.play()        -> return if (handle == 0 || !Core.audio.initialized)
Music.getPosition() -> handle == 0 ? 0f : Soloud.idPosition(voice)
Music.setPosition() -> return if (handle == 0 || voice <= 0)
```

### Observed reproduction

Help dialog for the `quick-rotate` feature (`FeatureHelpView.videoPreview()` →
`solim-video-preview` wiring). Cache key
`sha256("https://raw.githubusercontent.com/MindustryTool/MindustryToolMod/main/preview/quick-rotate.mp4")`
= `8e27b66b…`, matching the on-disk entries:

| artifact | observation |
| --- | --- |
| `…/cache/networkImage/8e27b66b….mp4` | 1 978 055 B, SHA-256 identical to `preview/quick-rotate.mp4` — download path is healthy |
| `…/cache/networkImage/8e27b66b….wav` | **44 B**; header says `RIFF size=991268`, `channels=1`, `rate=44100`, `bits=16`, `dataLen=991232` |
| console | one `[ERROR] . (:0): Broken atom of size 0` |

The 44-byte WAV is exactly `writeWavHeader`'s output: the header claims ≈11.24 s of PCM
(`991232 / (44100 * 2)`), but zero sample bytes exist. That is the signature of the PCM body
being deleted after the header was rewritten — not of a failed AAC decode.

### Defect 1 — the temp WAV is deleted before it is renamed

`AacToWav.transcode` (`solim-core/src/solim/display/video/AacToWav.java`):

```java
126: boolean done = false;
130: try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
131:     writeWavHeader(out, 44100, 2, 0);            // placeholder header
134:     while ((packet = audio.nextFrame()) != null) { …write PCM… }
158: } finally {
159:     if (!done)
160:         tmp.delete();                            // ← done is still false: the try block just ended
161: }
162: if (dataLen == 0 || sampleRate <= 0 || channels <= 0) return null;
164: rewriteWavHeader(tmp, sampleRate, channels, dataLen);   // recreates tmp via RandomAccessFile("rw")
167: if (!tmp.renameTo(wavOut)) return null;
169: done = true;                                    // assigned after the finally already ran
```

The `finally` always executes before line 164, so the rename at 167 always moves the
44-byte recreated file. `dataLen` is non-zero (the decode succeeded), so the method returns a
non-null `AudioInfo` and `VideoPlayer` believes audio is available.

### Defect 2 — a 44-byte file is a valid cache entry

`VideoCache.isFresh` (`VideoCache.java:60`) rejects only `length() == 0`, so the header-only
WAV satisfies it. `VideoPlayer.transcodeAudio` (`VideoPlayer.java:500`) then returns `true`
without transcoding, and every subsequent open repeats the freeze. `CACHE_MAX_AGE` is 30 days,
so the poisoned entry outlives ordinary cache churn.

### Defect 3 — the audio backend owns the frame clock

`VideoPlayer.update` (`VideoPlayer.java:307-317`):

```java
float master = hasAudio && music != null ? music.getPosition() : (silentClock += delta);
lastPosition = master;
presentUpTo(master);
boolean pastEnd = durationSec > 0 && master >= durationSec - END_EPSILON;
```

With a `Music` whose native handle is `0` (empty/invalid stream) `getPosition()` is a constant
`0f`, `play()` is a no-op, and `setPosition()` returns early. The result is a permanent
`PLAYING` state whose presentation clock never leaves frame 0: no decoding progress, no
`onEnded`, no `onFailed`, no error log. The same freeze occurs whenever
`Core.audio.initialized` is false or the audio device disappears, even with a valid WAV.

```
VideoPlayer.open ─► openInBackground (decode worker)
  ├─ transcodeAudio
  │    ├─ isFresh(44-byte .wav) == true ─────────────────────────────┐ Defect 2
  │    └─ AacToWav.decode ► transcode ► finally deletes the PCM ──────┘ Defect 1
  └─ finishOpen ► music = new Music(44-byte wav) ► handle == 0
        └─ update(): master = music.getPosition() == 0 ─────────────── Defect 3
              └─ frame 0 uploaded once; clock frozen; state stays PLAYING
```

### Defect 4 — every H.264 packet is corrupted before it reaches the decoder

Reported after Defects 1–3 were fixed (the cached `.wav` is now 991 276 B and the clock
advances). The video plays nothing: a blank surface, the duration label still shows the full
clip, pressing play finishes almost immediately, and no sound is produced.

A headless probe over the same clip (`preview/quick-rotate.mp4`; 269 packets, 11.096 s,
H.264 **High profile 100 / level 3.0**, 600×360 display inside 608×368 macroblocks) showed the
decode step itself was producing nothing:

| observation | value |
| --- | --- |
| demuxed packets | 269 |
| pictures produced | **0** |
| first failure | `IllegalArgumentException: newPosition > limit: (1677729461 > 21283)` at `H264Utils.decodeMOVPacketInplace` |
| second failure (conversion removed) | `NullPointerException: "this.data" is null` at `Picture.getPlaneData` ← `SliceDecoder.putMacroblock`, from `decodeFrame(data, null)` |

Two violations of JCodec's decode contract, both in `VideoPlayer.decodePacket`:

1. **The demuxer already converts the framing.** JCodec's `CodecMP4DemuxerTrack.getNextFrame`
   runs `H264Utils.decodeMOVPacket(result, avcC)` and prepends the Annex-B codec private
   (`SPS`/`PPS`) to IDR samples before handing the packet to the caller. `decodePacket` then
   called `H264Utils.decodeMOVPacketInplace(data, avcC)` *again*, which reads the leading
   `00 00 00 01` start code as a NAL length of 1 and next reads the SPS payload
   (`64 00 1E B5` = profile 100 / level 30 / next byte) as a length of 1 677 729 461 →
   `newPosition > limit`. Every packet threw; nothing decoded.
2. **`decodeFrame` requires a caller-allocated picture buffer.** The decoder builds the output
   frame *from* the `byte[][]` it is given; `null` produces a `Picture` whose `data` is `null`,
   and the first macroblock write throws. `reusePlanes` started as `null` and was only assigned
   after a *successful* decode, so it stayed `null` forever. JCodec's own
   `AVCMP4Adaptor.allocatePicture()` allocates
   `Picture.create(width, height, ColorSpace.YUV444).getData()` from the SPS's **macroblock**
   dimensions — 608×368 here, not the 600×360 display size (allocating from the display size
   would have under-sized the planes).

With `decodePacket` returning `null` for every packet, the decode loop skimmed the whole clip in
a fraction of a second and the frame queue stayed empty: no picture was ever uploaded (blank
surface), nothing was ever presented, and the end of the stream arrived with an empty queue. The
old `handleStreamEnd` re-looped that unproductive stream indefinitely whenever looping was on
(seek to 0, skim, seek again), and the presenter's `pastEnd`/`starved` decision was reached with
no picture either way. The audible and visible result was identical: silence and a blank frame.

Verification of the corrected pipeline on the same clip: 269/269 packets decode, the picture
carries `Rect [x=0, y=0, width=600, height=360]` over 608×368 `YUV420` planes, and the converted
RGBA is 216 000/216 000 non-black pixels. That last check was too weak: it only asked whether
pixels were not black, which a wrong chroma convention satisfies trivially. Defect 5 is what it
missed.

### Defect 5 — planes are read as unsigned, so every frame is a saturated color mask

Reported after Defect 4: frames now reach the screen, but the picture is a green/magenta mess.
Reproduced headlessly by running the clipped frame through the player's own pipeline
(`H264PacketDecoder` → `YuvToRgbaConverter`) and writing it out as a PNG for the same frames the
user saw:

| measurement (frame 60 of `preview/quick-rotate.mp4`) | broken | fixed |
| --- | --- | --- |
| average green channel | 237 | 61 |
| pixels that are exactly one saturated channel (`>200` while the other two are `<60`) | 81 % | 0 % |
| rendered PNG | flat bright green with colored fringes on edges | plain Mindustry scene |

Root cause: JCodec stores every plane sample as a **signed** byte holding `value - 128`
(`Picture`'s own contract: "The data is -128 shifted, so 0 is represented by -128 and 255 is
represented by +127"), and its own converters depend on it:

```java
// org.jcodec.scale.Yuv420pToRgb.YUV420pToRGBN2N(byte y, byte u, byte v, …)
int c = y + 112;                     // y is signed, so this is luma - 16
int r = (298 * c + 409 * v + 128) >> 8;   // v is signed, so this is V - 128
```

`YuvToRgbaConverter` read the same bytes with `& 0xFF` instead, so a neutral chroma sample
(stored `0x00`) became `U = V = 0`, giving `d = e = -128`, hence `r = b = 0` and `g` clipped to
255 for *every* luma value: a uniformly green frame whose luma is invisible, with real color only
leaking through where the stored bytes happened to be large. That is exactly the screenshot.

Fix: read the samples as the signed values they are (`Y = yPlane[i] + 128`, same for chroma) and
leave the existing limited/full-range math untouched — the three `+ 128`s are the whole change.

### Defect 6 — the one-row overlay does not fit the stage, so its controls overlap

Reported alongside Defect 5: the play button sat behind the timeline bar. The overlay was a single
Arc row, `[play][scrub][readout][replay][mute]`, inside a `Stack` that fills the stage, and the
stage the mod creates is 320×180 (`FeatureHelpView.videoPreview`:
`props(growX(), maxWidth(unit(80)), prefHeight(unit(45)))`).

The row cannot fit that width:

| column | minimum width | why |
| --- | --- | --- |
| padding | 40 | `pad(unit(1))` on five cells |
| three buttons | 132 | `unit(11)` = 44 at the time |
| scrub bar | 140 | Arc `ProgressBar.getPrefWidth()` is a fixed 140, and a `Cell`'s minimum defaults to the widget's |
| readout | ~104 | `Label.getMinWidth()` is `getPrefWidth()` unless the label wraps |

416 > 320. Arc's `Table.layout` then keeps every column at its minimum
(`columnWeightedWidth = columnMinWidth` when `totalGrowWidth == 0`), centres the 416px of content
in the 320px stage, and — because a fill cell keeps the element at least as wide as its own
minimum (`c.elementWidth = Math.max(spannedCellWidth * fillX, c.minWidth())`) — draws the slider
40px to the left of its cell. Measured headlessly at 320×180 with a font that reports real glyph
widths (the stage and cells are pure layout math, no GL required):

| | play | scrub | readout | replay | mute | inside stage |
| --- | --- | --- | --- | --- | --- | --- |
| before | x 4, w 44 | x 8, w 140 | x 108, w 104 | x 220, w 44 | x 272, w 44 | yes, but scrub and readout overlap the play button and the scrub bar by 40px |
| after | x 4, w 36 | x 4, w 312 | x 48, w 180 | x 236, w 36 | x 280, w 36 | yes, no overlaps |

Fix: give each control row the width it needs instead of one over-constrained row — the scrub bar
owns the first row, and a transport row below holds `[play][readout][replay][mute]` with the
readout as the flexible cell (`growX`, so it receives the leftover width) and `ellipsis`, so a
narrower stage truncates the readout instead of spilling it over the neighbouring buttons. The
buttons drop to `unit(9)` (36px), which the design rules allow for dense inline contexts, so the
two rows occupy 72 of the 180px. The scrub bar keeps its 140px minimum: the first row is 312px, so
it is never squeezed.

### The earlier freeze analysis was incomplete

Defects 1–3 were each reproduced independently (44-byte WAV on disk, `isFresh` accepting it,
`getPosition()` constant at `0f`) and their fixes are verified by tests. They were *not*,
however, the only reason nothing played: the packet pipeline never produced a single frame. The
"frozen at frame 0" reading of the original symptom was inferred from the clock code rather than
observed on a decoded frame — the surface was empty from the start.

### The `Broken atom of size 0` line is unrelated

`grep` over the classpath shows the string exists only in
`org.jcodec.containers.mp4.boxes.Header.read`:

```java
long size = 0;
while (input.remaining() >= 4 && (size = Platform.unsignedInt(input.getInt())) == 0);
if (input.remaining() < 4 || size < 8 && size != 1) {
    Logger.error("Broken atom of size " + size);   // size == 0 whenever < 4 bytes remain
    return null;
}
```

Parsing `preview/quick-rotate.mp4` (2-level box walk) shows the trigger inside
`moov → meta → ilst`:

```
ilst (size 62)
├─ item size=26  type="\0\0\0\1"  → index 1, child "data" (size 18, payload "15")
└─ item size=28  type="skip"     → 20 bytes of 0x00
```

`IListBox.parse` reads the 4-byte item size, then reads the "type" field as an integer index,
then loops `while (local.hasRemaining()) Header.read(local)`. The remaining 20 zero bytes are
consumed by `Header.read`'s zero-skip loop; the buffer is then empty and `size` is `0`, so the
error is emitted once and the metadata item is skipped. The outer loop then terminates
cleanly. Playback is unaffected.

The line is printed by JCodec's default `OutLogSink`, whose format is
`[#level]#class.#method (#file:#line): #message` with an empty caller when the level is above
`DEBUG` — hence `[ERROR]\t. (:0):\tBroken atom of size 0`. It fires once per `moov` parse, so
up to twice per play (`MP4Demuxer` for video + the second demuxer opened by `AacToWav`).

## Goals / Non-Goals

**Goals:**
- A video with an AAC track plays with sound, and a cached second open plays immediately.
- Every packet of a supported MP4 decodes into a picture whose display crop is what gets shown.
- A broken or truncated audio/video cache entry can never freeze or mute playback silently.
- Video presentation advances even when audio is absent, invalid, uninitialised, or has ended.
- A healthy playback produces no `[ERROR]` output.
- Existing installs recover from the poisoned 44-byte cache entry without manual cleanup.

**Non-Goals:**
- Changing the `SolimVideo`/`VideoSource` public API or the `.props(...)` surface. The overlay's
  *layout* is in scope (Defect 6); its props and icons are not.
- Adding codecs, platforms, or a new audio backend; SoLoud/MiniAudio behaviour is accepted as-is.
- Reworking the frame buffer pool, the decode queue, or the texture upload: once a picture exists
  and is read with the right convention (Defect 5), those stages are correct (216 000/216 000
  non-black pixels on the clip).
- Supporting H.264 profiles/features JCodec 0.2.5 cannot decode; such a clip continues to fail
  through `onFailed`, but it now ends deterministically instead of looping a stream that produces
  no frames.
- Poster-frame decoding at `READY` (blank-before-play polish). Noted below as deferred.
- Making JCodec parse iTunes metadata correctly; only its log output is in scope.
- Audio/video drift handling beyond bounded resynchronisation (no A/V offset calibration).

## Decisions

### 1. Delete the temp WAV only on real failure, then commit

- **Decision:** Remove the `finally`-based delete. Delete `<name>.wav.part` only on the paths
  that abandon the decode (exception, empty decode, header rewrite failure, delete/rename
  failure), and rename only after `rewriteWavHeader` has produced the final file. Keep the
  `.part` suffix so a half-written file is never mistaken for a cache entry.
- **Rationale:** The current flag ordering is the direct cause of the data loss; cleanup belongs
  on failure paths, not on the success path.
- **Alternatives considered:** Keep the `finally` and set `done = true` before it — rejected
  because `done` would then no longer mean "committed", and any later failure would leak the
  `.part` file. Writing the header at the end in a second pass (`RandomAccessFile` seek) —
  rejected as more I/O for the same result.

### 2. Cache validity predicates instead of `length() != 0`

- **Decision:** Give `VideoCache` explicit validity predicates:
  - `isFreshVideo(Fi)`: the file exists, is not a directory, and holds at least a container header
    (`MIN_VIDEO_BYTES = 16`, i.e. a box header plus an `ftyp` brand);
  - `isFreshAudio(Fi)`: the file exists, is not a directory, and is strictly larger than a WAV
    header (`WAV_HEADER_BYTES + 1 = 45`).
  Both delegate to one private size/TTL check, so the media kind supplies the threshold instead of
  a single shared "non-empty" rule. The previous public `isFresh` is replaced by these two names;
  both loaders call `isFreshVideo`.
- **Rationale:** Existence is not validity; both poisoned artifacts in this incident were
  non-empty. This is the guard that converts "silently frozen forever" into "transcode again".
- **Alternatives considered:** Cache a `.ok` marker beside each artifact — rejected as extra
  files to keep in sync. Trust only files newer than the build — rejected, the TTL already
  covers staleness and would still accept the 44-byte file.

### 3. Version the audio cache name so existing installs self-heal

- **Decision:** Change the audio cache key from `<sha>.wav` to `<sha>.v2.wav` (single constant
  holding the version suffix). Old entries stay on disk and are removed by the existing
  age-based cleanup; they are simply never read again.
- **Rationale:** Every existing install already holds a 44-byte `<sha>.wav` that would
  otherwise be accepted by any check loose enough to allow very short clips. Renaming is
  cheaper and safer than a migration or a startup purge.
- **Alternatives considered:** Delete all `*.wav` in the cache directory on startup — rejected
  as an unnecessary mass invalidation and a startup I/O spike. Validate the RIFF `data` length
  against the file length — good, and kept *in addition* as a validity check, but it cannot
  distinguish a legacy file from a legitimately tiny one as cleanly as a version bump.

### 4. Wall clock is the master; audio is best-effort synchronisation

- **Decision:** `silentClock += delta` becomes the authoritative presentation clock whenever
  the player is `PLAYING`, decided by a primitive-only helper
  `advanceClock(clock, delta, audioPosition, audioAdvancing)`: the wall clock advances first, and
  the audio position is adopted only when it is ahead of the wall clock by at most
  `RESYNC_THRESHOLD` (0.25 s). Audio further ahead is ignored, so a backend that reports a bogus
  large position cannot yank presentation forward. `music.getPosition()` is consulted only when
  the stream is actually advancing (`audioPosition > lastAudioPosition + AUDIO_EPSILON`);
  otherwise the wall clock is used and the audio is treated as unavailable. When the wall clock
  has passed a genuinely advancing audio stream by more than the threshold, the *audio* is
  corrected with `music.setPosition(wallClock)` — presentation is never rewound to follow audio.
  A loop restart resets the presentation clock to 0 (`loopRestartRequest`), because the clock is
  no longer supplied by a music stream that restarts on its own.
- **Rationale:** The stream position is an *output* of the audio backend, and the audio backend
  can legitimately be absent (headless, device loss, `Core.audio.initialized == false`, invalid
  handle). Presentation must not depend on an output that is allowed to be inert.
  Additionally, Arc/Soloud reports `0f` rather than failing, so there is no exception to catch —
  the clock itself has to be independent.
- **Alternatives considered:** Keep audio as master and fall back only when
  `music.getPosition()` is unchanged for N frames — rejected as a heuristic that still freezes
  for N frames and still depends on backend behaviour. Use `Time.time` directly — rejected:
  pausing and seeking must not be affected by global time scaling.

### 5. Treat an invalid `Music` as "no audio"

- **Decision:** After constructing `Music` in `finishOpen`, verify it is usable with
  `AudioSource.valid()` (`handle != 0`, confirmed from the Arc bytecode). If it is not, dispose it
  and continue silently. `hasAudio` is then derived from the surviving instance
  (`hasAudio = music != null`) rather than from "a wav path survived the transcode attempt", so it
  finally means "audio can play". The same block is skipped entirely when
  `Core.audio.initialized()` is false.
- **Rationale:** `hasAudio` currently means "a wav path survived the transcode attempt", not
  "audio can play". Correcting the meaning makes the clock decision in #4 deterministic.
- **Alternatives considered:** Probe by calling `play()` once and checking `isPlaying()` —
  rejected as a side effect that races the real playback state.

### 6. Route JCodec logging into Arc instead of stdout

- **Decision:** Add `VideoLog`, a JCodec `LogSink` that forwards to Arc's log: messages whose text
  starts with `Broken atom of size` are demoted to debug, other `ERROR`s go to `Log.err`, `WARN` to
  `Log.warn`, and everything else to `Log.debug`. `VideoLog.install()` latches a flag and is called
  once from `VideoPlayer`'s static initialiser, wrapped so that
  `IllegalStateException("Logger already started")` and any other failure leaves playback
  untouched. Do not use `Logger.setLevel`: its ordinal comparison cannot suppress only `ERROR`.
- **Rationale:** A component that reports `[ERROR]` while working correctly trains users (and
  us) to ignore real errors, and it blocks the "no console errors" acceptance item.
- **Alternatives considered:** Leave it and document — rejected, the user-visible symptom was
  precisely this line. Drop all JCodec `ERROR` messages — rejected, that would hide genuine
  container failures.

### 7. Decode packets exactly as the demuxer produces them

- **Decision:** `VideoPlayer` no longer touches the packet's NAL framing. The decode step moves
  into `H264PacketDecoder`, which builds the JCodec decoder from the container's Annex-B codec
  private, allocates one macroblock-aligned picture buffer from the SPS dimensions at construction
  time, and passes the demuxed packet and that buffer straight to `H264Decoder.decodeFrame`. A
  packet that throws is reported as "no picture" and the loop continues with the next packet.
- **Rationale:** Both failures were misuse of the JCodec API, not properties of the container or
  the clip: `CodecMP4DemuxerTrack` owns the AVCC → Annex-B conversion, and `H264Decoder.decodeFrame`
  requires an allocated destination (its own `AVCMP4Adaptor.allocatePicture()` is the reference).
  Isolating the contract in a small unit makes it verifiable headlessly, which the old inline code
  was not — it shipped zero decoded frames behind a `catch (Throwable)` that returned `null`.
- **Alternatives considered:** Keep the conversion but skip it when the buffer already begins with
  a start code — rejected as content sniffing that hides the contract. Let JCodec allocate the
  destination itself — not supported by the API. Leave the logic in `VideoPlayer` and rely on
  manual play — rejected: that is how the defect survived the first fix.

### 8. A looping clip only restarts when the pass produced frames

- **Decision:** `handleStreamEnd` restarts a loop only if the finished pass enqueued at least one
  frame (`framesSinceLoop`), and the presenter only ends playback on `pastEnd` when `looping` is
  false; a looping player instead restarts the clock at 0. `reopen()` resets the counter.
- **Rationale:** A looping player must not end at `duration - END_EPSILON`, because the
  presentation clock restarts from the demuxer's own loop; and a stream that decodes no frames
  must end rather than seek to 0 forever (the pre-existing behaviour spun on a full re-skim of the
  file per iteration).
- **Alternatives considered:** Wait inside the decode loop for the presentation queue to drain
  before seeking — rejected: it delays the loop point by the pipeline depth but does not
  materially improve the restart, and adds a blocking wait with its own teardown cases.

### 9. Convert JCodec planes with JCodec's own sample convention

- **Decision:** `YuvToRgbaConverter` reads every plane sample as the signed byte it is and adds 128
  back to recover the sample value (`Y = yPlane[i] + 128`, likewise for `U` and `V`); the
  limited/full-range math below it is unchanged. A regression test pins the convention with a
  frame whose planes are all zero: it must convert to gray, not to a saturated color.
- **Rationale:** The convention is JCodec's, documented on `Picture` and relied on by
  `Yuv420pToRgb`/`Yuv420jToRgb`, so matching it is the only way the decoder's output renders
  correctly; adding it at the read site keeps the color math readable, and it is one add per
  sample in a loop that already indexes three planes.
- **Alternatives considered:** Re-encode the planes into a YUV444 unsigned buffer in
  `H264PacketDecoder` before conversion — rejected: copies every frame to change a convention the
  reader can apply for free. Convert with JCodec's `Transform`/`Yuv420pToRgb` — rejected: its
  destination is also `-128`-shifted RGB, so it would only move the same correction to the upload
  path and add a per-frame `Picture` allocation.

### 10. Split the overlay into a scrub row and a transport row

- **Decision:** The overlay's first row is the scrub bar (`growX` across the full stage width); the
  second row is `[play][readout][replay][mute]` with the readout as the flexible cell (`growX`)
  and `ellipsis`, and the three buttons at `unit(9)`.
- **Rationale:** The measured failure is structural, not cosmetic: one row cannot satisfy Arc's
  minimums at the stage size the component is deployed at, so the table must have room for the
  slider's 140px minimum and for the readout's text width in *different* rows. Making the readout
  the flexible cell gives it the leftover width, and `ellipsis` bounds the failure mode on a
  narrower stage to "the readout truncates" rather than "the readout paints over the buttons".
  The buttons stay well above the touch-target size, and the overlay auto-hides while playing.
- **Alternatives considered:** Shrink the buttons and keep one row — rejected: the row's minimum is
  still 140 (slider) + the readout width + three buttons + padding ≈ 354px, so it stays
  over-constrained at 320. Set the slider cell's minimum width to 0 (`minWidth(0)`) to let it
  shrink — rejected: it frees the row geometry at the cost of a cramped scrub bar and it still
  leaves the readout competing with the buttons. Drop the readout — rejected: it is part of the
  component's contract.

## Risks / Trade-offs

- **Wall clock can drift from audio on long clips.** → Bounded resync only when the audio
  position advances; the drift is inaudible for the short preview clips this component serves,
  and audio is never *stopped* by the resync.
- **Clock change alters the existing `solim-video` contract.** → Captured as a MODIFIED
  requirement; any other consumer relying on audio-as-master must be reviewed (none found:
  `VideoPlayer` is used only by `SolimVideo` today).
- **Versioned audio file leaves an orphan per URL.** → Picked up by the existing age-based
  cleanup; at most one extra 44-byte file per previously played video.
- **JCodec sink is global and install-order sensitive.** → Installing from a static initialiser
  is the earliest safe point, and in the mod it always precedes decoding (`VideoPlayer`'s static
  initialiser runs when the player is constructed, before `open()` reaches `AacToWav`).
  The limitation is real and was observed while testing: once JCodec has emitted any message its
  `stageSinks` list is latched to the default `OutLogSink`, and `addSink` can never succeed again.
  `VideoLog.install()` therefore latches a flag so a second call is a no-op, and a late install
  degrades to "the previous behaviour" rather than a failure. The sink's routing is unit-tested
  directly (`VideoLog.post`) so the test does not depend on which global state the JVM is in.
- **A truncated MP4 that is still "large enough" passes the size check.** → Atomic write
  (`.part` + rename) removes crash-truncation as a source; decode failure still surfaces via
  `onFailed` → `fallback` as today.
- **A clip whose profile JCodec cannot decode produces no frames.** → The stream now ends
  deterministically instead of seeking in a loop, the surface stays blank, and the failure is
  visible as an ended video. Coverage for the contract is pinned to the fixture clip:
  `H264PacketDecoderTest` (packets → pictures, and a frame that must not render as a saturated
  color mask) and `VideoPlayerTest` (a real clip must advance past 5 s before ending, and a
  looping player must restart), both verified to fail on the old packet handling.
- **The color convention is a property of JCodec, not of this code.** → `Picture` documents the
  `-128` shift and JCodec's own converters depend on it, so a future upgrade that changes it would
  break every consumer, not just this one. `YuvToRgbaConverterTest` pins the behavior at the
  conversion boundary (neutral planes → gray), and `H264PacketDecoderTest` pins it end to end on
  a real decoded frame, so such an upgrade fails loudly instead of tinting the preview.
- **The two-row overlay covers 72px of a 180px surface while the controls are shown.** → The
  overlay auto-hides 2.5 s into playback and reappears on interaction, which is the existing
  behavior; the alternative (a single over-constrained row) is a defect, not a smaller footprint.
- **The overlay layout is validated by layout math rather than a rendered frame.** → The headless
  test measures bounds at the deployed 320×180 with a font that reports real glyph widths, which
  is exactly what `Table.layout` consumes; it reproduces the pre-fix overlap and passes after the
  fix. Rendering (GL) is out of reach in the test environment, so in-game confirmation remains
  part of the acceptance checklist.

## Deferred

- **Two decode loops after a replay on a lagging device:** `reopen()` (reached from `play()` while
  `ENDED`) starts a new decode loop without retiring the old one. The current decode loop exits on
  its own as soon as the stream is exhausted, which always happens *before* `pastEnd` unless the
  decoder lags real time; only then can both loops share the demuxer and decoder. A generation
  counter would retire the old loop, but the trigger is narrow and not exercised by the shipped
  configuration (the preview component loops by default, so it never reaches `ENDED`). Left as
  known debt rather than folded into this fix.
- **Poster frame at `READY`:** set `singleFrame = true` once after `finishOpen` so the component
  shows frame 0 before the user presses play. Useful for the help dialog (which does not
  autoplay) but orthogonal to the freeze; a separate small change.
- **Status signals for load failure:** `SolimVideo` exposes no failed/loaded `Readable`, so
  `feature-video-preview` needs a separate preflight probe to avoid empty space.
