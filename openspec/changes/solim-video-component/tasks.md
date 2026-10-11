## 1. Dependencies and Build Setup

- [ ] 1.1 Add `org.jcodec:jcodec:0.2.5` to `:solim-core` compile and runtime dependencies in `build.gradle.kts`
- [ ] 1.2 Verify project builds and passes checkstyle with new dependency

## 2. Video Loading and Caching

- [ ] 2.1 Create `VideoLoader` interface in `solim.display.video` with `load(String url, Cons<Fi> onSuccess, Cons<Throwable> onError)`
- [ ] 2.2 Implement `DefaultVideoLoader` with transparent disk caching under `cache/videos/` using URL hashing
- [ ] 2.3 Implement `RequestVideoLoader` in `:mod` routing network downloads through `mindustrytool.services.Request`
- [ ] 2.4 Wire `SolimVideo.setVideoLoader(new RequestVideoLoader())` during mod initialization

## 3. Video Decoding and Frame Pipeline

- [ ] 3.1 Implement `YuvToRgbaConverter` converting JCodec `Picture` YUV420p planes directly to direct `ByteBuffer` without AWT
- [ ] 3.2 Implement `VideoPlayer` managing asynchronous background frame decoding and thread-safe double buffering
- [ ] 3.3 Implement presentation timestamp (PTS) pacing with game delta time and OpenGL texture uploads via `glTexSubImage2D`
- [ ] 3.4 Implement seeking and seamless stream looping in `VideoPlayer`

## 4. Solim UI Component and Overlay Controls

- [ ] 4.1 Implement `SolimVideo` leaf component supporting reactive URLs, autoplay, loop, and rounded-corner masks
- [ ] 4.2 Implement auto-hide overlay control bar with play/pause button, progress scrubber, timestamp readout, and replay button
- [ ] 4.3 Add configurable controls toggle (`.controls(boolean)` and `.controls(Readable<Boolean>)`) to show or completely omit controls
- [ ] 4.4 Expose `video(String)` and `video(Readable<String>)` declarative builder methods in `solim.UI`
- [ ] 4.5 Ensure full resource disposal on component detachment (worker thread termination, texture disposal)

## 5. Testing and Verification

- [ ] 5.1 Add unit tests for `VideoLoader` disk caching and cache hit/miss behavior
- [ ] 5.2 Add tests for `VideoPlayer` playback states, pacing, and clean disposal
- [ ] 5.3 Add Solim UI tests for `SolimVideo` reactivity and controls visibility toggle
- [ ] 5.4 Run full verification (`./gradlew check checkNoFullyQualifiedNames jar`)
