## 1. Configuration & Internationalization

- [x] 1.1 Add translatable strings to `assets/bundles/bundle.properties` for media selection, video volume, scaling modes, and unsupported codec errors
- [x] 1.2 Add `videoVolumeConfig` (int 0–100, default 100) and `scalingConfig` (string "crop"|"stretch", default "crop") to `BackgroundFeature`

## 2. Video Rendering & Scaling Engine

- [x] 2.1 Refactor `BackgroundFeature` to detect media format (`.mp4` vs static image) and route to appropriate renderer
- [x] 2.2 Implement `VideoMenuRenderer` wrapping `VideoPlayer` with continuous looping, delta time frame updating, and GL texture drawing
- [x] 2.3 Implement proportional "Crop to Fill" and "Stretch" rendering logic for both video and static textures based on `scalingConfig`

## 3. Audio & Menu Music Coordination

- [x] 3.1 Integrate Mindustry menu music suppression when an MP4 with active audio is playing on the main menu
- [x] 3.2 Bind `videoVolumeConfig` to `VideoPlayer.setVolume()` and restore menu music when volume is 0 or muted

## 4. Game Lifecycle & Resource Management

- [x] 4.1 Implement game state listeners (`EventType.StateChangeEvent` / `Trigger.update`) to fully dispose `VideoPlayer` when entering a game or editor (`!Vars.state.isMenu()`)
- [x] 4.2 Re-instantiate and restart background playback upon returning to the main menu (`Vars.state.isMenu()`)
- [x] 4.3 Ensure `onDisable()` and `resetBackground()` properly clean up video player, audio state, and restored original renderer

## 5. Settings UI

- [x] 5.1 Expand file chooser in `BackgroundSettingsView` to accept `png`, `jpg`, `jpeg`, and `mp4`
- [x] 5.2 Add scaling mode selector ("Crop to Fill" vs "Stretch") in `BackgroundSettingsView`
- [x] 5.3 Add video audio volume slider and media format indicators to `BackgroundSettingsView`

## 6. Testing & Quality Verification

- [x] 6.1 Create unit tests for background media detection, scaling geometry, and lifecycle teardown
- [x] 6.2 Verify encapsulation guard test (`SolimEncapsulationGuardTest`) and zero-allocation frame rules pass
- [x] 6.3 Run Gradle build and test suite to ensure clean compile and test pass

## 7. Native Decoder Path

- [x] 7.1 Extract video decoding behind `VideoFrameDecoder` and add `VideoDecoders` selection with `AUTO`/`SOFTWARE` preference and `solim.video.decoder` override
- [x] 7.2 Move rounded-corner masking into `RgbaCorners` and reduce `YuvToRgbaConverter` to `convert(Picture, ByteBuffer)` with `allocateRgba`
- [x] 7.3 Make `VideoPlayer` fail over from a native decoder to the JCodec decoder at the current position when native decoding fails mid-stream
- [x] 7.4 Implement `MediaFoundationVideoDecoder` (Windows) on Media Foundation's source reader through the Foreign Function & Memory API, decoding NV12 and converting to RGBA through the bounded-lifetime `Nv12ToRgbaConverter` pool
- [x] 7.5 Verify native and software decoders agree on geometry, frame count and seek positions, and that the native path is preferred
- [x] 7.6 Benchmark native and software decoding at 1440p and document the measured bottleneck
- [ ] 7.7 Implement the Android MediaCodec decoder provider (Phase 2) — deferred until a machine with an Android SDK is available; Android currently uses the JCodec fallback
- [x] 7.8 Add the reflective `ForeignRuntime`/`ForeignMemory` bridge over `java.lang.foreign` and port `MediaFoundationVideoDecoder` off JNA, which cannot initialize in Mindustry's jlink runtime because it requires `java.util.logging`

