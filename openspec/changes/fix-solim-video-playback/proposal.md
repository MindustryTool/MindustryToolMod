## Why

`SolimVideo` cannot play any video that contains an audio track. On a freshly cached
`preview/quick-rotate.mp4` the component renders a single frozen frame, the progress readout
stays at `00:00`, and no sound is produced. The only console output is the misleading line
`[ERROR] . (:0): Broken atom of size 0`.

Investigation against the runtime artifacts (Mindustry 160.7, Arc with SoLoud, JCodec 0.2.5)
established three independent defects:

1. **Decoded audio is always discarded.** `AacToWav.transcode` deletes its `<name>.wav.part`
   file in a `finally` block whose `done` flag is only assigned *after* the rename. The
   `finally` therefore always runs before the rename, so the PCM body is deleted and
   `rewriteWavHeader` recreates a 44-byte header-only file. On disk:
   `.../solim/cache/networkImage/<sha>.wav` is **44 bytes** while its header declares
   `dataLen = 991232` (≈11.2 s of 16-bit mono 44.1 kHz PCM). The AAC decode itself succeeds.
2. **The cached 44-byte WAV is trusted forever.** `VideoCache.isFresh` accepts any
   non-empty file, so the broken cache entry is reused for the full 30-day TTL and every
   later open skips transcoding.
3. **The video clock is owned by the audio backend.** `VideoPlayer.update` uses
   `music.getPosition()` as the master clock whenever an audio track exists. Arc's
   `Music.getPosition()` returns `0f` when the underlying SoLoud handle is `0` (failed or
   empty stream), and `Music.play()` is a silent no-op in that state. The master clock
   never advances, so `presentUpTo(0)` presents frame 0 forever while the state remains
   `PLAYING` — a frozen video with no sound, and no error path taken.

The reported `Broken atom of size 0` is **not** a symptom of the above. It is JCodec's own
`OutLogSink` reporting a zero-filled `ilst` metadata item this MP4 contains (Android
MediaMuxer writes `size=28, "skip", 20 zero bytes`). `IListBox.parse` feeds those bytes to
`Header.read`, whose zero-skipping loop consumes them and then reports `size 0`. The
metadata item is skipped and playback is unaffected; the line is cosmetic but it makes a
working (or broken) player look like it is failing, and it violates the "no console errors"
acceptance item already recorded in `feature-video-preview`.

The existing `solim-video` baseline spec mandates the very coupling that causes the freeze
("`music.getPosition()` SHALL serve as the master clock"), so the contract must change with
the fix.

## What Changes

- **Atomic, lossless audio cache write.** Restructure `AacToWav.transcode` so the temporary
  WAV is removed only on a genuine failure, and is renamed into place only after the PCM
  body and the final header are both on disk.
- **Cache integrity instead of existence checks.** Introduce audio/video cache validity
  predicates so a header-only WAV or a truncated MP4 is never treated as fresh; write both
  cache entries atomically; version the audio cache name so installs that already hold the
  broken 44-byte file recover without manual deletion.
- **Playback clock independent of the audio backend.** Drive frames from a wall-clock
  accumulator; use `music.getPosition()` only as a best-effort resynchronisation source when
  the audio stream is genuinely advancing. Treat a `Music` instance that is invalid
  (`handle == 0`) or never advances as "no audio".
- **Diagnostics that do not lie.** Route JCodec's logger into Arc's log so the known-benign
  container-parse messages are reported at debug level and a healthy playback produces no
  `[ERROR]` output; never let logger installation break playback.
- **Regression coverage for the actual failure.** Replace the header-writer-only test with an
  end-to-end AAC-in-MP4 → WAV assertion that the persisted file contains the decoded body,
  plus a clock test proving playback advances while audio is inert.

## Capabilities

### New Capabilities
<!-- None: the work changes existing video behaviour -->

### Modified Capabilities
- `solim-video`: audio decoding/clock requirement rewritten (atomic WAV persistence, cache
  validity, wall-clock master, audio as best-effort sync), plus new cache-integrity and
  diagnostic-output requirements.

## Impact

- `solim-core/src/solim/display/video/AacToWav.java` — temp-file lifecycle, commit order.
- `solim-core/src/solim/display/video/VideoCache.java` — validity predicates, atomic writes,
  versioned audio name, stale-entry handling.
- `solim-core/src/solim/display/video/VideoPlayer.java` — clock ownership, invalid-audio
  detection, audio resync policy.
- `solim-core/src/solim/display/video/DefaultVideoLoader.java` — atomic cache write.
- `mod/src/mindustrytool/components/RequestVideoLoader.java` — atomic cache write parity.
- `solim-core` tests (`AacToWavTest`, `VideoPlayerTest`, new fixture-based transcode test).
- No user-visible strings, no bundle changes, no public API break: `SolimVideo`,
  `VideoSource`, and the `.props(...)` surface are unchanged.
- No change to `feature-video-preview` behaviour; that change's Phase 2 verification items
  become passable once this lands.
