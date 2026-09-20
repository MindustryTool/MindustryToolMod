## MODIFIED Requirements

### Requirement: Real-Time Room Directory and SSE Synchronization
The system SHALL synchronize the active room directory in real time by managing room state through a reactive `Query<List<PlayerConnectRoom>>` and connecting to `GET /player-connect/sse` continuously in the background while the feature is enabled. Room updates streamed from the SSE connection SHALL update the query data via mutation without UI rebuilding. Initial room state and manual/fallback retrieval SHALL be supported via the query's REST fetcher (`GET /player-connect/rooms`). Relay server providers SHALL be retrieved via `GET /player-connect/providers`, with support for built-in `LocalHost` and persisted custom user-added providers.

#### Scenario: SSE stream delivers room updates
- **WHEN** the backend broadcasts updated room state via `GET /player-connect/sse`
- **THEN** the received JSON payload `{ rooms: [...] }` is parsed and published to the reactive room query without UI rebuilding

#### Scenario: SSE disconnection triggers reconnection
- **WHEN** the SSE stream disconnects or encounters an error
- **THEN** the system logs the error and attempts reconnection with exponential backoff, falling back to REST room fetch via the room query

#### Scenario: Custom relay providers persistence
- **WHEN** a user adds a custom relay provider address `ip:port`
- **THEN** the custom provider is validated, added to the provider list, and persisted in `Core.settings`

### Requirement: Solim Room Browser and Mod Compatibility in Join Dialog
The system SHALL inject a Solim room browser section into Mindustry's `JoinDialog` (`Vars.ui.join`), displaying active PlayerConnect rooms reactively updated from the rooms query using `QueryView`. Each room card SHALL show the room name, map name, gamemode, player count, lock status, and mod compatibility. If mod differences exist between client and server, the system SHALL display badges indicating missing or unneeded mods and provide a `JoinWarningDialog` offering to disable unneeded mods before connecting.

#### Scenario: Room browser reactive display
- **WHEN** Mindustry's Join Game screen is opened
- **THEN** the PlayerConnect room section renders room cards using `QueryView` that update automatically as the query data changes

#### Scenario: Mod mismatch warning on join
- **WHEN** a user clicks to join a room requiring different mods
- **THEN** a warning dialog lists missing and unneeded mods, with options to disable conflicting mods and proceed or cancel
