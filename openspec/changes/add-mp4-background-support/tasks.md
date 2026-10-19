## 1. Configuration & Internationalization

- [ ] 1.1 Add translatable strings to `assets/bundles/bundle.properties` for media selection, video volume, scaling modes, and unsupported codec errors
- [ ] 1.2 Add `videoVolumeConfig` (int 0–100, default 100) and `scalingConfig` (string "crop"|"stretch", default "crop") to `BackgroundFeature`

## 2. Video Rendering & Scaling Engine

- [ ] 2.1 Refactor `BackgroundFeature` to detect media format (`.mp4` vs static image) and route to appropriate renderer
- [ ] 2.2 Implement `VideoMenuRenderer` wrapping `VideoPlayer` with continuous looping, delta time frame updating, and GL texture drawing
- [ ] 2.3 Implement proportional "Crop to Fill" and "Stretch" rendering logic for both video and static textures based on `scalingConfig`

## 3. Audio & Menu Music Coordination

- [ ] 3.1 Integrate Mindustry menu music suppression when an MP4 with active audio is playing on the main menu
- [ ] 3.2 Bind `videoVolumeConfig` to `VideoPlayer.setVolume()` and restore menu music when volume is 0 or muted

## 4. Game Lifecycle & Resource Management

- [ ] 4.1 Implement game state listeners (`EventType.StateChangeEvent` / `Trigger.update`) to fully dispose `VideoPlayer` when entering a game or editor (`!Vars.state.isMenu()`)
- [ ] 4.2 Re-instantiate and restart background playback upon returning to the main menu (`Vars.state.isMenu()`)
- [ ] 4.3 Ensure `onDisable()` and `resetBackground()` properly clean up video player, audio state, and restored original renderer

## 5. Settings UI

- [ ] 5.1 Expand file chooser in `BackgroundSettingsView` to accept `png`, `jpg`, `jpeg`, and `mp4`
- [ ] 5.2 Add scaling mode selector ("Crop to Fill" vs "Stretch") in `BackgroundSettingsView`
- [ ] 5.3 Add video audio volume slider and media format indicators to `BackgroundSettingsView`

## 6. Testing & Quality Verification

- [ ] 6.1 Create unit tests for background media detection, scaling geometry, and lifecycle teardown
- [ ] 6.2 Verify encapsulation guard test (`SolimEncapsulationGuardTest`) and zero-allocation frame rules pass
- [ ] 6.3 Run Gradle build and test suite to ensure clean compile and test pass
