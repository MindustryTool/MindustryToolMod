## ADDED Requirements

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

### Requirement: Customizable destination directory
The screenshot feature SHALL allow the player to choose the target directory where screenshots are saved. The setting SHALL default to Mindustry's standard screenshot directory (`Vars.screenshotDirectory`). If a custom directory path is configured, it SHALL be validated and automatically created if it does not already exist.

#### Scenario: Default directory used when no custom path is configured
- **WHEN** the player has not specified a custom save path
- **THEN** screenshots are saved into the default Mindustry screenshots directory

#### Scenario: Custom destination directory is respected
- **WHEN** the player selects or enters a valid custom directory path in the settings
- **THEN** subsequently captured screenshots are saved into the custom directory

#### Scenario: Non-existent directory is created automatically
- **WHEN** the configured destination directory does not exist on disk
- **THEN** the feature creates the directory hierarchy before saving the image file

### Requirement: Customizable file naming
The screenshot feature SHALL support customizable file naming. The player SHALL be able to configure a filename prefix and timestamp pattern. In addition, the feature SHALL provide an option to prompt the player for a custom filename before saving.

#### Scenario: Automatic timestamp filename generation
- **WHEN** a screenshot is captured with filename prompt disabled
- **THEN** the file is saved with the configured prefix and current timestamp (e.g. `screenshot-2026-09-17_19-30-00.png`)

#### Scenario: Filename collision handling
- **WHEN** a file with the generated or entered name already exists in the destination directory
- **THEN** the feature appends an incremental numeric suffix (e.g. `_1.png`) to prevent overwriting existing captures

#### Scenario: Interactive filename prompt when enabled
- **WHEN** the player has enabled the "Prompt for filename" setting and takes a screenshot
- **THEN** a text input dialog appears allowing the player to input or confirm the filename before the file is written to disk

### Requirement: Asynchronous image encoding and user notifications
The feature SHALL encode and save the captured `Pixmap` asynchronously off the main render thread to prevent frame drops or stutter. Upon successful save, a toast notification SHALL be presented to the player displaying the filename and providing an action to open the containing folder.

#### Scenario: Off-thread file encoding
- **WHEN** a screenshot pixmap is grabbed from the framebuffer
- **THEN** the PNG compression and disk I/O are performed on a background thread, immediately returning control to the game loop

#### Scenario: Success notification with directory open action
- **WHEN** the screenshot file is successfully written to disk
- **THEN** a toast notification appears confirming the save, and tapping the notification opens the destination folder in the system file manager

#### Scenario: Write failure gracefully handled
- **WHEN** disk writing fails due to permission issues or insufficient disk space
- **THEN** the error is caught, logged, and a user-friendly error notification is displayed without crashing the game

### Requirement: Quick Access and Solim Settings Dialog
The feature SHALL integrate with `FeatureManager` and `QuickAccessFeature`. Tapping the Quick Access icon SHALL immediately trigger a screenshot according to the current settings. Long-tapping the Quick Access icon SHALL open the `ScreenshotSettingsDialog`. The settings dialog SHALL follow Solim declarative conventions and allow configuring capture mode, directory, naming template, prompt toggle, and notification options.

#### Scenario: Quick Access click triggers capture
- **WHEN** the player clicks the Screenshot icon in the Quick Access bar
- **THEN** a screenshot is captured immediately using the active configuration

#### Scenario: Quick Access long-click opens settings
- **WHEN** the player long-clicks the Screenshot icon in the Quick Access bar
- **THEN** the Solim `ScreenshotSettingsDialog` is displayed

#### Scenario: Settings are persisted reactively
- **WHEN** the player modifies any setting in `ScreenshotSettingsDialog`
- **THEN** the setting is updated and persisted immediately via `ConfigGroup` / `ConfigValue`
