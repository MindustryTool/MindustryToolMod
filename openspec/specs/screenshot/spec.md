# screenshot Specification

## Purpose

In-game screen capture with toggleable UI presence, fixed timestamped naming in the standard screenshots directory, quiet toast feedback, and Quick Access integration with a tiny Solim settings dialog. Created by archiving change screenshot-feature.

## Requirements

**Source: screenshot-feature**

### Requirement: Screen capture with or without UI
The screenshot feature SHALL allow the player to capture the game screen either with all UI elements visible or without any UI elements. When capturing without UI, all HUD elements, overlays, and dialogs SHALL be omitted from the captured image. The capture trigger UI itself (dialogs, tooltips, or popups) SHALL never appear in the captured screenshot in either mode.

#### Scenario: Capture without UI produces clean canvas
- **WHEN** the player triggers a screenshot with the "Include UI" option disabled
- **THEN** the framebuffer is captured prior to UI rendering (at `uiDrawBegin`) or with UI elements hidden, producing an image of the game world with no HUD, dialogs, or mod overlays

#### Scenario: Capture with UI includes visible game interface
- **WHEN** the player triggers a screenshot with the "Include UI" option enabled
- **THEN** the framebuffer is captured after UI rendering, including the in-game HUD, minimap, and active interface elements

#### Scenario: Capture mechanism hides itself during capture
- **WHEN** a screenshot is triggered from a dialog or button
- **THEN** the screenshot UI dialog or modal is temporarily hidden before capture so that it does not appear in the resulting image

### Requirement: Fixed destination directory
The screenshot feature SHALL always save screenshots into Mindustry's standard screenshot directory (`Vars.screenshotDirectory`). There is no custom directory setting.

#### Scenario: Screenshots land in the standard directory
- **WHEN** the player triggers a screenshot
- **THEN** the file is saved into the default Mindustry screenshots directory

### Requirement: Fixed file naming
The screenshot feature SHALL use a fixed timestamped file naming format `screenshot-yyyy-MM-dd_HH-mm-ss.png`. There is no filename pattern setting and no filename prompt.

#### Scenario: Automatic timestamp filename generation
- **WHEN** a screenshot is captured
- **THEN** the file is saved with the current timestamp (e.g. `screenshot-2026-09-17_19-30-00.png`)

#### Scenario: Filename collision handling
- **WHEN** a file with the generated name already exists in the destination directory
- **THEN** the feature appends an incremental numeric suffix (e.g. `_1.png`) to prevent overwriting existing captures

### Requirement: Asynchronous image encoding and quiet notifications
The feature SHALL encode and save the captured `Pixmap` asynchronously off the main render thread to prevent frame drops or stutter. Upon successful save, a quiet toast notification SHALL always be presented displaying the filename. There is no notification toggle and no open-folder action.

#### Scenario: Off-thread file encoding
- **WHEN** a screenshot pixmap is grabbed from the framebuffer
- **THEN** the PNG compression and disk I/O are performed on a background thread, immediately returning control to the game loop

#### Scenario: Success notification
- **WHEN** the screenshot file is successfully written to disk
- **THEN** a quiet toast notification appears confirming the save with the filename

#### Scenario: Write failure gracefully handled
- **WHEN** disk writing fails due to permission issues or insufficient disk space
- **THEN** the error is caught, logged, and a user-friendly error notification is displayed without crashing the game

### Requirement: Quick Access and Solim Settings Dialog
The feature SHALL integrate with `FeatureManager` and `QuickAccessFeature`. Tapping the Quick Access icon SHALL immediately trigger a screenshot according to the current settings. Long-tapping the Quick Access icon SHALL open the `ScreenshotSettingsDialog`. The settings dialog SHALL follow Solim declarative conventions and allow configuring capture mode only, plus a button that opens the standard screenshots directory in the system file manager.

#### Scenario: Quick Access click triggers capture
- **WHEN** the player clicks the Screenshot icon in the Quick Access bar
- **THEN** a screenshot is captured immediately using the active configuration

#### Scenario: Quick Access long-click opens settings
- **WHEN** the player long-clicks the Screenshot icon in the Quick Access bar
- **THEN** the Solim `ScreenshotSettingsDialog` is displayed

#### Scenario: Settings are persisted reactively
- **WHEN** the player modifies the capture mode setting in `ScreenshotSettingsDialog`
- **THEN** the setting is updated and persisted immediately via `ConfigGroup` / `ConfigValue`
