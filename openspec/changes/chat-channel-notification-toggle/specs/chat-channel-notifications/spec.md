# chat-channel-notifications Specification

## ADDED Requirements

### Requirement: Per-channel notification mute state
The system SHALL maintain a reactive state indicating whether notifications are enabled or muted for each chat channel, persisted across game sessions in settings.

#### Scenario: Channel notification toggle
- **WHEN** a player clicks the notification toggle button on a selected channel item
- **THEN** the channel's mute state toggles between muted and unmuted, the reactive state updates, and the preference is persisted to `Core.settings`

#### Scenario: Channel mute state persistence across restart
- **WHEN** a channel is marked as muted and the game or mod restarts
- **THEN** the channel remains muted upon loading settings during initialization

### Requirement: Selected channel notification toggle button
The `ChatChannelListView` component SHALL display an interactive notification toggle button within the channel item row when that channel is currently selected.

#### Scenario: Selected unmuted channel displays mute action
- **WHEN** a channel is currently selected and notifications are enabled
- **THEN** the channel row displays a button with a bell icon and a tooltip indicating that clicking will mute notifications

#### Scenario: Selected muted channel displays unmute action
- **WHEN** a channel is currently selected and notifications are muted
- **THEN** the channel row displays a button with a muted bell icon and a tooltip indicating that clicking will unmute notifications

### Requirement: Unselected muted channel indicator
The `ChatChannelListView` component SHALL display a subtle non-interactive muted indicator on unselected channels that are currently muted.

#### Scenario: Unselected muted channel shows muted indicator
- **WHEN** a channel is muted and is not the currently active selected channel
- **THEN** the channel row displays a subtle muted bell icon indicating its muted status

#### Scenario: Unselected unmuted channel shows no indicator
- **WHEN** a channel is unmuted and is not the currently active selected channel
- **THEN** the channel row displays no bell icon
