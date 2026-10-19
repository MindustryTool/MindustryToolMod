# Fix Solim Video Playback - Implementation Tasks

## 1. Reproduce & pin the baseline

- [x] 1.1 Reproduce with the `quick-rotate` help dialog; confirm the cached `.wav` is 44 bytes
      and that the header's `dataLen` is non-zero
      (on disk: `…/solim/cache/networkImage/8e27b66b….wav` = 44 B, `dataLen = 991232`)
- [x] 1.2 Confirm the audio-clock mechanism: `state == PLAYING` with a constant
      `music.getPosition()` of `0f`
      (`Music.getPosition()` is `handle == 0 ? 0f : …`, `play()` no-ops on `handle == 0`, and
      the empty cached WAV yields `handle == 0`; the resulting frame-0 freeze was inferred from
      the clock code, and is not on its own the reason nothing was drawn — see 1.4)
- [x] 1.3 Use `preview/quick-rotate.mp4` itself as the regression clip: the test resolves it by
      walking up from the working directory, so no second copy of the 1.9 MB asset is committed
- [x] 1.4 Pin the second reproduction (`fix-solim-video-playback` follow-up): blank surface,
      duration shown, play finishing almost immediately, no sound. Headless probe over the
      fixture: 269 packets, **0** pictures, first failure
      `IllegalArgumentException: newPosition > limit: (1677729461 > 21283)` in
      `H264Utils.decodeMOVPacketInplace`, and with that call removed
      `NullPointerException: "this.data" is null` in `Picture.getPlaneData` from
      `decodeFrame(data, null)`

## 2. Make the WAV commit lossless (`AacToWav`)

- [x] 2.1 Remove the `finally`-based delete of the `.part` file; delete only on the abandon
      paths (exception, empty decode, header rewrite failure, delete/rename failure)
- [x] 2.2 Rename into the final cache path only after the PCM body and the rewritten header
      are both written
- [x] 2.3 Return "no usable audio" explicitly when the transcode does not commit, instead of
      returning a success value derived from a stale path
      (`transcode` returns `null` unless the commit succeeded; `hasAudio` is then derived from a
      usable `Music` instance)

## 3. Cache integrity & invalidation (`VideoCache`, loaders)

- [x] 3.1 Replace the shared `length() != 0` rule with media-kind validity predicates
      (`isFreshVideo`: at least a container header; `isFreshAudio`: strictly larger than a
      44-byte header)
- [x] 3.2 Version the audio cache key (`<sha>.v2.wav`) so existing 44-byte entries are ignored
- [x] 3.3 Write downloaded MP4s atomically (`.part` + rename) — done centrally in
      `VideoCache.writeVideo`, so `DefaultVideoLoader` and `RequestVideoLoader` both get it;
      an interrupted write never lands at the final path
- [x] 3.4 Ensure stale entries are rejected by `isFreshVideo` / `isFreshAudio` (and therefore
      re-fetched / re-transcoded), not deleted mid-playback

## 4. Decouple presentation from the audio backend (`VideoPlayer`)

- [x] 4.1 Make the accumulated wall-clock delta the master presentation clock while `PLAYING`;
      keep pause/seek acting on that clock only
- [x] 4.2 Consult `music.getPosition()` only for bounded resynchronisation while the stream is
      advancing; never let it hold or rewind presentation (audio is corrected towards the wall
      clock, not the other way round)
- [x] 4.3 Verify the constructed `Music` is usable (`valid()`, i.e. `handle != 0`); if not,
      dispose it, mark audio unavailable, and continue silently
- [x] 4.4 Treat `Core.audio.initialized() == false` as "no audio" rather than as a frozen clock
- [x] 4.5 Keep the end-of-clip behaviour (`onEnded`, looping restart) driven by the wall clock
      and audio state, not by a position that may never move; a loop restart resets the
      presentation clock to 0

## 5. Diagnostics (`solim-core` init)

- [x] 5.1 Install a JCodec `LogSink` that forwards to Arc's log and demotes the known-benign
      `Broken atom of size 0` to debug
- [x] 5.2 Guard installation against `Logger already started` and any other failure; playback
      must be unaffected (a latch keeps a second install a no-op)
- [x] 5.3 Do not suppress genuine container failures: the failed path must still reach
      `VideoPlayer.Listener.onFailed`

## 6. Regression tests (`solim-core`)

- [x] 6.1 End-to-end transcode test: decode the committed fixture and assert the persisted WAV
      is larger than 44 bytes, that the extra bytes equal the decoded sample count, and that
      the `RIFF`/`data` chunk sizes match the file length
      (`AacToWavTest.transcodeCommitsDecodedPcmToTheFinalFile`, verified to fail on the old code)
- [x] 6.2 Atomicity test: an abandoned transcode leaves no `.wav` at the final path and no
      leftover `.part` (`AacToWavTest.abandonedTranscodeLeavesNoPartFile`, verified to fail on
      the old code, which leaked the `.part` file)
- [x] 6.3 Cache validity test: a 44-byte audio entry is reported stale; a header-only or
      directory video entry is reported stale (`VideoCacheTest`)
- [x] 6.4 Playback clock test: with audio inert (`getPosition()` constant `0f`), presentation
      advances with delta time and never remains stuck at frame 0 (`VideoPlayerTest`)
- [x] 6.5 Verify the existing `AacToWavTest` header/wrap assertions still pass unchanged

## 7. Decode the H.264 packet stream (`H264PacketDecoder`)

- [x] 7.1 Stop re-applying the AVCC → Annex-B conversion: JCodec's `CodecMP4DemuxerTrack`
      (`H264Utils.decodeMOVPacket` + prepended codec private on IDR samples) already returns
      Annex-B NAL units, so `H264Utils.decodeMOVPacketInplace` corrupted every packet
      (probe: `newPosition > limit: (1677729461 > 21283)`, 0 of 269 packets decoded)
- [x] 7.2 Supply the decoder's destination picture buffer: allocate once from the SPS dimensions
      rounded up to whole macroblocks (608×368 for the 600×360 clip) with `ColorSpace.YUV444`
      planes, as JCodec's own `AVCMP4Adaptor.allocatePicture()` does; a `null` buffer made every
      frame throw (`Picture.getPlaneData` NPE) and left `reusePlanes` `null` forever
- [x] 7.3 Isolate the contract in `H264PacketDecoder` (decoder + planes + `decode(ByteBuffer)`),
      so `VideoPlayer.decodePacket` only reports "no picture" and the loop moves on
- [x] 7.4 Confirm the pipeline end-to-end on the fixture: 269/269 packets decode, crop
      `Rect[0,0,600,360]` over 608×368 `YUV420` planes, converted RGBA 216 000/216 000 non-black
      (the check was too weak to catch the chroma defect — see 8.1)
- [x] 7.5 Make the end of a looping stream deterministic: re-loop only when the finished pass
      produced at least one frame, and end a looping player on `pastEnd` by restarting the clock
      instead of entering the ended state
- [x] 7.6 Regression tests: `H264PacketDecoderTest` (every packet decodes into a macroblock-aligned
      picture with a display crop; consecutive frames differ; empty packets are rejected) —
      verified to fail with the previous conversion + `null` buffer behaviour
- [x] 7.7 Playback tests over the real clip (`VideoPlayerTest`): a non-looping player advances past
      5 s of clip time before ending (it ended at ≈0 s with the old decoder), and a looping player
      restarts its clock without ending — both verified to fail with the previous decoder

## 8. Render the decoded frame with JCodec's sample convention

- [x] 8.1 Reproduce: after 7.4 the frames reached the screen but rendered as a green/magenta mask.
      Headless render of the clip through the player's own pipeline (`H264PacketDecoder` →
      `YuvToRgbaConverter`, written out as a PNG for frames 0/60/200): average green channel 237,
      81 % of pixels a single saturated channel, edges only faintly visible
- [x] 8.2 Pin the cause: JCodec stores plane samples as signed bytes holding `value - 128`
      (`Picture`) and its own converters rely on it (`Yuv420pToRgb` computes `c = y + 112`,
      `Yuv420jToRgb` computes `(y + 128)`); `YuvToRgbaConverter` read them with `& 0xFF`, so
      neutral chroma (stored `0x00`) became `U = V = 0` → `r = b = 0`, `g` clipped to 255 for any
      luma
- [x] 8.3 Read the samples as the signed values they are (`+ 128` at the three plane reads) and
      leave the range math untouched
- [x] 8.4 Re-render the same frames: average green channel 61, 0 % saturated-mask pixels, the PNG
      is an ordinary Mindustry scene
- [x] 8.5 Regression tests: `YuvToRgbaConverterTest.neutralPlanesConvertToGray` (all-zero planes
      must convert to gray) and `H264PacketDecoderTest.decodedFrameRendersWithNaturalColors`
      (a decoded frame must not be a saturated color mask) — both verified to fail with the
      previous unsigned reads

## 9. Give the overlay controls room (Defect 6)

- [x] 9.1 Reproduce at the deployed stage size (320×180, `FeatureHelpView`): the row's minimum is
      40 (padding) + 3×44 (buttons) + 140 (Arc `ProgressBar`'s fixed preferred width) + the
      readout's text width ≈ 416 > 320
- [x] 9.2 Pin the Arc behaviour that turns the over-constrained row into an overlap:
      `Table.layout` keeps the columns at their minimums when `totalGrowWidth == 0`, centres the
      wider content in the stage, and a fill cell keeps its element at least as wide as its own
      minimum (`c.elementWidth = max(spannedCellWidth * fillX, c.minWidth())`), so the scrub bar
      is drawn 40px left of its cell, over the play button
- [x] 9.3 Restructure the overlay: scrub bar as a full-width first row, transport row below it
      with `[play][readout][replay][mute]`, the readout as the flexible cell (`growX`) with
      `ellipsis`, and the three buttons at `unit(9)`
- [x] 9.4 Re-measure at 320×180: scrub bar 312px, readout 180px, play/replay/mute 36px each, all
      inside the stage, no sibling overlaps
- [x] 9.5 Regression test: `SolimVideoTest.overlayControlsFitTheVideoStage` lays the overlay out at
      the deployed stage size with a font that reports real glyph widths and asserts that no
      control escapes the stage, that siblings do not overlap, and that the readout can display
      its own text — verified to fail (readout given 24px of the 104px it needs) with the previous
      single-row layout

## 10. Manual verification

- [ ] 10.1 `quick-rotate` help dialog: frames are visible with correct colors, audio is audible,
      progress advances
- [ ] 10.2 The play button, timeline, replay, and mute controls no longer overlap
- [ ] 10.3 Second open plays from the disk cache without re-downloading or re-transcoding
- [ ] 10.4 Delete only the `.wav` cache entry; playback recovers on the next open
- [ ] 10.5 An MP4 with no audio track plays silently and to completion
- [ ] 10.6 Audio unavailable (backend uninitialised / invalid handle): video still plays
      silently
- [ ] 10.7 Console shows no `[ERROR]` line during a healthy playback
- [ ] 10.8 Seek, pause, replay, and looping (`setLoop(true)`, the default) still behave as before
- [ ] 10.9 `setLoop(false)`: playback ends at the end of the clip and `onEnded` fires

## 11. Hygiene

- [x] 11.1 No new user-visible strings; `assets/bundles/bundle.properties` unchanged
- [x] 11.2 No new listeners or timers without an owner; no per-frame allocations introduced in
       `update()` (`advanceClock`, `audioPosition`, `resyncAudio` are primitive-only)
- [x] 11.3 `:solim-core` and `:mod` tests pass; `checkBundles`, encapsulation guard, and
       modifier-import checks stay green (root `check` is green)
- [ ] 11.4 Once verified, tick the manual verification items in
       `openspec/changes/archive/2026-10-19-feature-video-preview/tasks.md` that this change
       satisfies (the feature-preview help dialog items)
