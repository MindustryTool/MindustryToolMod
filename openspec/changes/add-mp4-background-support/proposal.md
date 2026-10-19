## Why

Currently, the custom background feature only supports static image files (PNG), rendering a single static OpenGL texture on the main menu. Players want to set dynamic, animated MP4 video backgrounds (such as animated wallpapers or cinematics) to customize their main menu experience.

## What Changes

- Add MP4 video playback support to `BackgroundFeature` using Solim's built-in `VideoPlayer` (H.264 / JCodec).
- Update the file chooser to accept MP4 files alongside standard image formats (`png`, `jpg`, `jpeg`, `mp4`).
- Support configurable video audio playback on the main menu, pausing Mindustry's ambient menu music when active video audio is playing, with volume and mute settings.
- Add aspect-ratio scaling modes (`crop` / "Crop to Fill" vs. `stretch` / "Stretch") so backgrounds look great on any display aspect ratio without distortion.
- Implement game-lifecycle hooks: fully dispose the active `VideoPlayer` and its decode threads/textures when entering a match or editor (`!Vars.state.isMenu()`) to guarantee zero CPU/GPU/memory overhead during gameplay, and re-instantiate it upon returning to the menu.
- Update `BackgroundSettingsView` to reflect media selection, audio volume controls, scaling options, and translatable guidance.

## Capabilities

### New Capabilities
- `video-background`: Dynamic MP4 video background playback for Mindustry's main menu, featuring H.264 decoding, audio/menu music management, crop/stretch scaling, and zero-footprint lifecycle disposal during active gameplay.

### Modified Capabilities
<!-- No existing spec requirements modified -->

## Impact

- `mindustrytool.features.background`:
  - `BackgroundFeature.java`: Media detection, `VideoPlayer` integration, menu music pause/resume coordination, in-game lifecycle listener.
  - `BackgroundSettingsView.java`: Media file chooser extensions, audio volume slider, scaling toggle, updated bundle strings.
- Assets:
  - `assets/bundles/bundle.properties`: New translatable strings for media selection, audio volume, scaling modes, and codec error messaging.
- Dependencies & Runtime:
  - Uses existing `:solim` / `:solim-core` `VideoPlayer` and JCodec engine; no new external dependencies or Java 8 violations.
