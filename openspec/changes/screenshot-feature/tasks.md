## 1. Core Capture & Storage Service

- [ ] 1.1 Implement `ScreenshotNaming` utility for timestamp formatting, variable replacement, and collision index incrementing
- [ ] 1.2 Implement `ScreenshotService` for scheduling render-thread frame captures (`Trigger.uiDrawBegin` vs `Trigger.postDraw`)
- [ ] 1.3 Implement asynchronous image writer on background daemon thread with `PixmapIO.writePng` and memory cleanup
- [ ] 1.4 Add directory resolution with validation and fallback to `Vars.screenshotDirectory`

## 2. Feature & State Management

- [ ] 2.1 Create `ScreenshotFeature` extending `Feature` with metadata, icon, and quick access support
- [ ] 2.2 Configure `ConfigGroup` with settings for UI inclusion, custom folder, filename pattern, filename prompt, and notifications
- [ ] 2.3 Wire Quick Access click to execute capture and long-click to open settings dialog
- [ ] 2.4 Register `ScreenshotFeature` into `Main.java` and `FeatureManager`

## 3. Solim Settings & Input Dialogs

- [ ] 3.1 Build `ScreenshotSettingsView` using Solim declarative layouts constrained to `maxWidth(500f)`
- [ ] 3.2 Add UI controls for toggling UI presence, selecting destination directory via file chooser, and configuring filename format
- [ ] 3.3 Create `ScreenshotSettingsDialog` wrapping the settings view
- [ ] 3.4 Implement prompt dialog for custom filename entry when prompt mode is active

## 4. Internationalization & Notifications

- [ ] 4.1 Add all translation keys and descriptive comments to `assets/bundles/bundle.properties`
- [ ] 4.2 Implement capture success toast notification with shortcut to open directory in system file explorer
- [ ] 4.3 Implement error toast notification for write or directory access failures

## 5. Verification & Testing

- [ ] 5.1 Add unit tests for `ScreenshotNaming` and destination directory path handling
- [ ] 5.2 Verify project builds with `./gradlew check` and no checkstyle or Java 8 compatibility violations
