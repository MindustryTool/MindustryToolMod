## ADDED Requirements

### Requirement: MP4 Background Video Playback
The system SHALL support selecting and playing local H.264 MP4 video files as the main menu background in continuous loop.

#### Scenario: User selects an MP4 video file
- **WHEN** user selects an MP4 video file in the background settings
- **THEN** system copies the video to the backgrounds directory, initializes a `VideoPlayer`, and renders the animated video frames in a loop on the main menu

#### Scenario: Unsupported video format selected
- **WHEN** user selects an MP4 file encoded with an unsupported codec (e.g., HEVC or AV1)
- **THEN** system catches the failure, displays a translatable error notification explaining H.264 requirement, and restores the default menu renderer without crashing

### Requirement: Video Audio and Menu Music Coordination
The system SHALL play audio from MP4 background videos when present and coordinate with Mindustry's ambient menu music.

#### Scenario: Video with audio plays on main menu
- **WHEN** an MP4 background with audio is active on the main menu and video volume is greater than 0%
- **THEN** system mutes or pauses Mindustry's default menu music and plays the video's audio track

#### Scenario: User adjusts video volume
- **WHEN** user changes the video volume slider in background settings
- **THEN** system applies the new volume immediately to the active video player

#### Scenario: Video has no audio or is muted
- **WHEN** the MP4 file has no audio track or video volume is set to 0%
- **THEN** system allows Mindustry's ambient menu music to play unhindered

### Requirement: Configurable Background Scaling
The system SHALL support both "Crop to Fill" and "Stretch" scaling modes for video and image backgrounds.

#### Scenario: Crop to fill mode active
- **WHEN** scaling mode is set to "crop"
- **THEN** system scales the background texture proportionally so that it fills the entire viewport width and height without aspect ratio distortion, centering the frame and cropping excess bounds

#### Scenario: Stretch mode active
- **WHEN** scaling mode is set to "stretch"
- **THEN** system scales the background texture directly to match the screen's width and height

### Requirement: In-Game Resource Lifecycle Teardown
The system SHALL completely dispose active video player decoders, audio streams, and textures when the player is not on the main menu.

#### Scenario: Player enters a game
- **WHEN** game state transitions from the main menu to an active world, match, or map editor
- **THEN** system stops and disposes the `VideoPlayer`, releasing GL texture and worker thread resources to ensure zero CPU/memory overhead during gameplay

#### Scenario: Player returns to main menu
- **WHEN** player leaves the game and returns to the main menu while background feature is enabled
- **THEN** system reloads the configured background video and resumes playback automatically

### Requirement: Media Selection UI in Settings
The system SHALL provide a unified media selection interface in `BackgroundSettingsView` supporting images and videos.

#### Scenario: Opening media chooser
- **WHEN** user clicks "Select Background Media" in background settings
- **THEN** system opens a file chooser accepting `png`, `jpg`, `jpeg`, and `mp4` file extensions

#### Scenario: Viewing settings for video background
- **WHEN** an MP4 file is currently selected as background
- **THEN** settings view displays video volume control, scaling mode selector, and opacity adjustment
