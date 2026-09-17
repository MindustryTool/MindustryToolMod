## ADDED Requirements

### Requirement: Channel List Loading and Error Handling
The system SHALL track loading and error state when fetching chat channels and display a loading spinner, an error message with technical exception details, and a Retry button when channel loading fails.

#### Scenario: Channel loading indicator
- **WHEN** the chat system initiates fetching chat channels
- **THEN** ChatChannels sets loading to true and ChatChannelListView displays a centered animated loader

#### Scenario: Channel loading failure and error view
- **WHEN** fetching chat channels fails due to a network or server error
- **THEN** ChatChannels sets loading to false and records the error message, and ChatChannelListView displays a centered error state showing the localized error message, technical cause, and a Retry button

#### Scenario: Retrying channel fetch
- **WHEN** the user clicks the Retry button on the channel error view
- **THEN** ChatService initiates a new channel fetch, sets loading to true, and clears previous errors

### Requirement: Initial Chat Messages Loading and Error Handling
The system SHALL track per-channel initial message loading and error states, displaying a loading spinner during initial message fetch, a full-screen error with a Retry button if the initial fetch fails, and a non-intrusive warning with retry if a subsequent refresh fails while messages are already present.

#### Scenario: Initial message loading indicator
- **WHEN** an active channel is selected and messages have not yet loaded
- **THEN** ChatMessages marks initial loading as true for that channel and ChatMessageListView displays a centered loader

#### Scenario: Initial message loading failure
- **WHEN** the initial message request for a channel fails and no messages are currently cached for that channel
- **THEN** ChatMessageListView renders a centered error state with a localized error message, technical cause, and a Retry button

#### Scenario: Retrying initial message fetch
- **WHEN** the user clicks the Retry button in ChatMessageListView
- **THEN** ChatService triggers loadMessages for the active channel, clears the channel error, and sets loading to true

#### Scenario: Preserving existing messages on subsequent refresh failure
- **WHEN** messages are already present in the active channel and a subsequent manual or background refresh fails
- **THEN** ChatMessageListView preserves the existing visible message list and displays a non-intrusive error banner with a Retry action

### Requirement: Chat Member Roster Loading and Error Handling
The system SHALL track per-channel member roster loading and error states, displaying a loading spinner during member fetch, a full-screen error with a Retry button if the fetch fails with an empty roster, and preserving existing members if a subsequent refresh fails.

#### Scenario: Member roster loading indicator
- **WHEN** an active channel is selected and member fetch begins
- **THEN** ChatMembers marks loading as true for that channel and ChatUserListView displays a centered loader

#### Scenario: Member roster loading failure
- **WHEN** fetching members for a channel fails and no members are cached for that channel
- **THEN** ChatUserListView renders a centered error state with a localized error message, technical cause, and a Retry button

#### Scenario: Retrying member roster fetch
- **WHEN** the user clicks the Retry button in ChatUserListView
- **THEN** ChatService triggers loadUsers for the active channel, clears the channel error, and sets loading to true

### Requirement: Mobile Default Tab Cross-State Error Awareness
The system SHALL display channel error details and a Retry Channels action directly in the Messages and Members views when no channel is active due to channel loading failure.

#### Scenario: Channel failure presented in Messages tab
- **WHEN** the chat overlay is opened on mobile (defaulting to the Messages tab) and channels failed to load on startup
- **THEN** the Messages view displays an error notice stating that channels failed to load along with a Retry Channels button

#### Scenario: Successful retry from Messages tab
- **WHEN** the user clicks the Retry Channels button from the Messages view
- **THEN** channels are re-fetched and upon success the first channel is selected and its messages are loaded automatically

### Requirement: Top-Bar Refresh Scope
The system SHALL allow the top-bar refresh button to recover from missing channel state by refreshing channels if none is active, or refreshing active messages and members if an active channel is present.

#### Scenario: Refresh with no active channel
- **WHEN** the top-bar refresh button is clicked while no active channel is selected
- **THEN** ChatService executes refreshChannels() to attempt channel discovery

#### Scenario: Refresh with active channel
- **WHEN** the top-bar refresh button is clicked while a channel is active
- **THEN** ChatService refreshes both messages and members for the active channel and reconnects the live stream if disconnected
