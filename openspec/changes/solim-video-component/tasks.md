## 1. Dependencies and Assets Setup

- [ ] 1.1 Add `org.jcodec:jcodec:0.2.5` to `:solim-core` dependencies in `build.gradle.kts` (no separate JAAD dependency — jcodec ships JAAD shaded in)
- [ ] 1.2 Download and place Lucide icons (`play.png`, `pause.png`, `volume-2.png`, `volume-x.png`) into `assets/icons/`
- [ ] 1.3 Verify project builds and passes checkstyle with new dependencies and assets

## 2. Video Loading and Disk Caching

- [ ] 2.1 Create `VideoLoader` interface in `solim.display.video` with `load(String url, Cons<Fi> onSuccess, Cons<Throwable> onError)`
- [ ] 2.2 Implement `DefaultVideoLoader` with disk caching under shared directory `Vars.dataDirectory/solim/cache/networkImage/` using SHA-256 URL hashing
- [ ] 2.3 Implement `RequestVideoLoader` in `:mod` routing network downloads through `mindustrytool.services.Request`
- [ ] 2.4 Wire `SolimVideo.setVideoLoader(new RequestVideoLoader())` during mod initialization

## 3. Video and Audio Decoding Pipeline

- [ ] 3.1 Implement `YuvToRgbaConverter` converting JCodec `Picture` YUV420p planes directly to direct `ByteBuffer` without AWT
- [ ] 3.2 Implement pure Java AAC audio decoding via jcodec's `org.jcodec.codecs.aac.AACDecoder`, saving 16-bit PCM WAV to `<hash>.wav` in the shared disk cache
- [ ] 3.3 Implement `VideoPlayer` managing asynchronous background frame decoding and thread-safe double buffering
- [ ] 3.4 Implement presentation timestamp (PTS) pacing with `arc.audio.Music` master clock and delta-time fallback
- [ ] 3.5 Implement seeking, volume scaling via `sfxvol`, and seamless stream looping in `VideoPlayer`

## 4. Solim UI Component and Overlay Controls

- [ ] 4.1 Implement `SolimVideo` leaf component supporting reactive URLs, autoplay, loop, volume, and rounded-corner masks
- [ ] 4.2 Implement auto-hide overlay control bar with Lucide icons (play/pause, progress scrubber, timestamp readout, replay button, mute toggle)
- [ ] 4.3 Add configurable controls toggle (`.controls(boolean)` and `.controls(Readable<Boolean>)`) to show or completely omit controls
- [ ] 4.4 Expose `video(String)` and `video(Readable<String>)` declarative builder methods in `solim.UI`
- [ ] 4.5 Ensure full resource disposal on component detachment (worker thread termination, texture disposal, `Music` disposal)

## 5. Testing and Verification

- [ ] 5.1 Add unit tests for `VideoLoader` disk caching and cache hit/miss behavior in shared directory
- [ ] 5.2 Add tests for `VideoPlayer` A/V synchronization, playback states, audio caching, and clean disposal
- [ ] 5.3 Add Solim UI tests for `SolimVideo` reactivity and controls visibility toggle
- [ ] 5.4 Run full verification (`./gradlew check checkNoFullyQualifiedNames jar`)
