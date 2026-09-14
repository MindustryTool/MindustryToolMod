# chat-feature Specification

## Purpose
Provides the lifecycle, persistent configurations, reactive state store, and MindustryTool network integration for the in-game chat feature.

## Requirements
### Requirement: Feature Lifecycle and Registration
The system SHALL provide a ChatFeature extending mindustrytool.features.Feature with metadata (id: "chat", name, description, icon, enabled by default, quick-access enabled), managing lifecycle activation and deactivation cleanly.

#### Scenario: Feature enabled
- **WHEN** ChatFeature.onEnable() is called
- **THEN** the chat overlay HUD is instantiated and added to the scene, and network connections/streams are initialized

#### Scenario: Feature disabled
- **WHEN** ChatFeature.onDisable() is called
- **THEN** the chat overlay HUD is removed and disposed, and network connections/streams are disconnected

### Requirement: Configuration via ConfigValue
The system SHALL manage all persistent chat settings using ConfigGroup and ConfigValue<T> instances, exposing reactive signals for each setting.

#### Scenario: Reading and updating opacity configuration
- **WHEN** opacityConfig value is changed
- **THEN** the underlying setting is persisted and its reactive signal notifies observers with the new value

#### Scenario: Collapsed state persistence
- **WHEN** the chat is collapsed or expanded
- **THEN** collapsedConfig updates and persists the boolean state

### Requirement: Reactive Chat State Management
The system SHALL maintain single-source-of-truth chat state composed within ChatStore via dedicated domain state modules (ChatSession, ChatChannels, ChatMessages, ChatMessageDelivery, ChatMembers, ChatUsers, ChatUnread, ChatTranslations, ChatUiState), with per-channel unread status evaluated against persistent read state and deriving total unread presence reactively.

#### Scenario: New message received in inactive channel
- **WHEN** a message is received for a channel that is not currently active
- **THEN** the message is appended to that channel's message list in ChatMessages and that channel's unread status in ChatUnread is updated to unread

#### Scenario: Selecting an active channel
- **WHEN** a channel is selected as active via ChatStore.selectChannel()
- **THEN** active channel signal in ChatChannels updates, that channel's latest known message ID is persisted to Core.settings as read, unread status for that channel in ChatUnread is cleared, and active selection UI state in ChatUiState is reset

#### Scenario: Channel history fully loaded
- **WHEN** older messages are requested for a channel and the returned list is empty or smaller than the requested page size
- **THEN** the channel is marked as fully loaded in ChatMessages, preventing further fetch requests

#### Scenario: Pending message tracking
- **WHEN** a temporary message is created for optimistic display
- **THEN** its ID is marked as PENDING in ChatMessageDelivery

#### Scenario: Pending message cleared on confirmation
- **WHEN** a pending message is confirmed by server response
- **THEN** its status is cleared from ChatMessageDelivery and replaced or reconciled in ChatMessages

#### Scenario: Failed message tracking
- **WHEN** a send request fails for a pending message
- **THEN** its status transitions to FAILED in ChatMessageDelivery

#### Scenario: Failed message cleared on retry
- **WHEN** a failed message is retried
- **THEN** its status transitions from FAILED back to PENDING in ChatMessageDelivery

### Requirement: Persistent Last-Read Message Tracking via UUIDv7
The system SHALL persist the last-read message ID per channel in `Core.settings` (`mindustrytool.chat.lastread.<channelId>`), and determine channel unread status by comparing the latest known message ID (from `ChannelDto.lastMessageId` or incoming stream messages) against the stored ID using lexicographical UUIDv7 comparison.

#### Scenario: Unread evaluated from channel metadata without messages loaded
- **WHEN** channel list is fetched and a channel has a lastMessageId newer than the stored last-read ID in Core.settings (or no stored ID exists)
- **THEN** that channel is marked as unread in ChatUnread even if message history has not been fetched

#### Scenario: Marking channel as read on selection or active view
- **WHEN** the user selects a channel or has the channel active while the chat window is open
- **THEN** the channel's latest known message ID is persisted in Core.settings and unread status for that channel becomes false

#### Scenario: Real-time message arrival in active vs inactive channel
- **WHEN** a new message arrives via stream
- **THEN** if the chat window is open and the message belongs to the active channel, the stored last-read ID is immediately updated to that message's ID; if the window is closed or the message is for an inactive channel, the channel is marked as unread

### Requirement: MindustryTool Service Integration
The system SHALL interact with chat REST endpoints and SSE event streams via mindustrytool.services.MindustryTool and marshal state updates to the main thread via Core.app.post().

#### Scenario: Initializing chat data
- **WHEN** ChatService.init() is invoked
- **THEN** channels and initial messages are fetched via MindustryTool.getChatChannels() and MindustryTool.getChatMessages(), and the live SSE stream is connected
