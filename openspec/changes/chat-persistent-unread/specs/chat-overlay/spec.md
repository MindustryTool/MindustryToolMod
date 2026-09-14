## MODIFIED Requirements

### Requirement: Collapsed Badge Mode
The system SHALL provide a collapsed floating badge display when collapsedConfig is true, showing a single draggable pill button that also expands the chat on click, plus a floating (zero-layout-space) connection status indicator and an unread notification dot.

#### Scenario: Single button drags and expands
- **WHEN** the user drags the collapsed badge
- **THEN** the entire badge moves with the pointer via the draggable binding without opening the chat

#### Scenario: Expanding from collapsed badge
- **WHEN** the user clicks (without dragging) the collapsed badge
- **THEN** collapsedConfig is set to false and the expanded chat window is displayed

#### Scenario: Connection status dot is floating
- **WHEN** the collapsed badge is rendered
- **THEN** the connection-status indicator is positioned as a floating overlay on the chat icon and contributes zero width and zero height to the row layout

#### Scenario: No separate drag-handle icon
- **WHEN** the collapsed badge is displayed
- **THEN** only one icon (chat icon) is visible and no separate move/drag handle icon is rendered

#### Scenario: Unread badge indicator display
- **WHEN** any channel has persistent unread messages while the chat is collapsed
- **THEN** a scarlet unread circle dot is displayed on the top-right corner of the collapsed badge

## ADDED Requirements

### Requirement: Channel Unread Circle Indicator
The system SHALL display an unread indicator circle on each channel row in the channel list when that channel has unread messages, derived reactively from persistent read tracking.

#### Scenario: Channel with unread messages
- **WHEN** a channel has a latest message ID newer than the local last-read ID
- **THEN** a white circle indicator is visible next to the channel name in ChatChannelListView

#### Scenario: Channel read
- **WHEN** the user switches to or views the channel
- **THEN** the unread indicator circle for that channel becomes hidden
