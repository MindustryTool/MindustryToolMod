## ADDED Requirements

### Requirement: SSE Heartbeat Watchdog and Health Management
The system SHALL monitor the real-time SSE chat stream with a 45-second heartbeat watchdog timer, resetting the timer on every incoming event or heartbeat line, and terminating stalled connections to trigger reconnection if no data arrives within the timeout window.

#### Scenario: Incoming data or heartbeat resets watchdog
- **WHEN** any line (heartbeat `:heartbeat`, `event: heartbeat`, or data payload) is received by `ChatService`
- **THEN** the watchdog timer is reset to the current timestamp

#### Scenario: Stream stall triggers reconnection
- **WHEN** no line has been received from the server for more than 45 seconds while streaming is enabled
- **THEN** the active stream request is cancelled, connection state transitions to disconnected (`setConnected(false)`), and automatic reconnection is scheduled

#### Scenario: Connection state reflects live handshake
- **WHEN** the stream HTTP request is initiated
- **THEN** connection state (`store.session().connected()`) only becomes `true` after the server sends a successful handshake (`Connected`), heartbeat, or event payload

### Requirement: Catch-Up Synchronization on Reconnect
The system SHALL automatically perform catch-up synchronization with the backend whenever the SSE stream connection is established or restored.

#### Scenario: Reconnection syncs active channel messages
- **WHEN** `ChatService` establishes or re-establishes a live stream connection
- **THEN** it executes a catch-up fetch for the active channel via `loadMessages(activeChannelId)` and refreshes channel metadata via `refreshChannels()`

### Requirement: Silent Background Catch-Up on Uncollapse
The system SHALL automatically synchronize the active channel in the background when transitioning from collapsed state to expanded state.

#### Scenario: Expanding chat triggers background refresh
- **WHEN** `collapsedConfig` transitions from `true` to `false`
- **THEN** the system verifies stream health (reconnecting if disconnected) and silently fetches recent messages for the active channel without displaying a blocking full-page loading indicator

### Requirement: Resilient Manual Refresh
The system SHALL perform a full health check on manual refresh, verifying the live stream state and reconnecting if stalled or disconnected.

#### Scenario: Manual refresh revives dead or stalled stream
- **WHEN** the user triggers manual refresh in `ChatOverlayHudView`
- **THEN** `ChatService` reloads active channel messages and users, checks whether the stream is active, and forces a stream reconnect if disconnected or stalled
