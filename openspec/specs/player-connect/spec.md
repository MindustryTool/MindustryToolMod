# player-connect Specification

## Purpose
Provides relay-based peer-to-peer multiplayer hosting and joining (CLA-J protocol v159), real-time room discovery via Server-Sent Events (SSE), declarative Solim hosting/management dialogs, join approval HUD banners, and mod compatibility inspection in the Mindustry join screen.
## Requirements
### Requirement: Relay Network Engine and Wire Compatibility
The system SHALL provide an encapsulated relay networking engine implementing the CLA-J protocol version 159. When hosting a game through a relay server, the engine SHALL wrap Mindustry's `NetProvider` with `ProxyProvider` and forward wrapped TCP and UDP packets through a single client connection to the relay server. Remote players connecting via the relay SHALL be managed as `VirtualConnection` instances. On the client side, joining via `player-connect://` URLs SHALL inject the custom `Serializer` and send a `RoomJoinPacket`. Relay server IP addresses SHALL be automatically unbanned to prevent accidental lockout of the proxy.

#### Scenario: Host creates room through relay server
- **WHEN** the host initiates room creation with a selected relay server IP and port
- **THEN** a `NetworkProxy` instance connects to the relay, wraps the vanilla `NetProvider`, transmits `RoomCreationRequestPacket`, and receives a unique `roomId`

#### Scenario: Client connects via player-connect URL
- **WHEN** a client joins using a valid `player-connect://host:port/roomId` link
- **THEN** client connects to the relay host and port, installs the packet `Serializer`, and sends `RoomJoinPacket(roomId, password)`

#### Scenario: Proxy IP unban on kick or ban
- **WHEN** a player is IP-banned while a relay room is active
- **THEN** the relay server's IP address is immediately unbanned in Mindustry's admin system to prevent disconnecting other guest players

### Requirement: Real-Time Room Directory and SSE Synchronization
The system SHALL synchronize the active room directory in real time by connecting to `GET /player-connect/sse` continuously in the background while the feature is enabled. Room updates streamed from the SSE connection SHALL update a reactive `Signal<List<PlayerConnectRoom>>`. Initial room state and fallback retrieval SHALL be supported via REST `GET /player-connect/rooms`. Relay server providers SHALL be retrieved via `GET /player-connect/providers`, with support for built-in `LocalHost` and persisted custom user-added providers.

#### Scenario: SSE stream delivers room updates
- **WHEN** the backend broadcasts updated room state via `GET /player-connect/sse`
- **THEN** the received JSON payload `{ rooms: [...] }` is parsed and published to the reactive room list signal without UI rebuilding

#### Scenario: SSE disconnection triggers reconnection
- **WHEN** the SSE stream disconnects or encounters an error
- **THEN** the system logs the error and attempts reconnection with exponential backoff, falling back to REST room fetch

#### Scenario: Custom relay providers persistence
- **WHEN** a user adds a custom relay provider address `ip:port`
- **THEN** the custom provider is validated, added to the provider list, and persisted in `Core.settings`

### Requirement: Declarative Solim Host and Manage Dialogs
The system SHALL provide declarative Solim dialogs for hosting and managing relay rooms. `HostRoomDialog` SHALL guide the user through setting the room name, password, max player limit, auto-accept toggle, and selecting a relay server provider with live ping checks. `ManageRoomDialog` SHALL display active room status, a button to copy the join link to the clipboard, and a button to close the room. A button in Mindustry's pause menu (`Vars.ui.paused`) SHALL dynamically open `HostRoomDialog` or `ManageRoomDialog` depending on whether a room is currently active.

#### Scenario: Host room dialog configuration
- **WHEN** the host opens `HostRoomDialog`
- **THEN** room configurations (name, password, max players, auto-accept) are bound to reactive inputs, and the provider list displays live ping status for each server

#### Scenario: Pause menu button state
- **WHEN** the game is paused while hosting a room
- **THEN** the pause menu displays a "Manage Room" button; **WHEN** not hosting, it displays a "Host Room" button

### Requirement: Non-Intrusive In-Game Join Approval HUD
When `autoAccept` is disabled and an incoming guest joins a hosted game, the guest player SHALL initially be placed into `Team.derelict` without a spawned unit. The system SHALL display a non-intrusive Solim HUD banner at the top of the host's screen containing the guest's name, an `[Accept]` button, and a `[Reject]` button. The host's gameplay, unit control, and camera movement SHALL NOT be interrupted or paused by a modal dialog.

#### Scenario: Host accepts joining player
- **WHEN** an incoming player joins with `autoAccept` false
- **THEN** the HUD banner appears at the top of the screen; when the host clicks `[Accept]`, the player is assigned to their intended team and spawned into the game

#### Scenario: Host rejects joining player
- **WHEN** the host clicks `[Reject]` on the join banner
- **THEN** the guest connection is cleanly closed with an informative rejection message, and the HUD banner disappears

### Requirement: Solim Room Browser and Mod Compatibility in Join Dialog
The system SHALL inject a Solim room browser section into Mindustry's `JoinDialog` (`Vars.ui.join`), displaying active PlayerConnect rooms reactively updated from the room signal. Each room card SHALL show the room name, map name, gamemode, player count, lock status, and mod compatibility. If mod differences exist between client and server, the system SHALL display badges indicating missing or unneeded mods and provide a `JoinWarningDialog` offering to disable unneeded mods before connecting.

#### Scenario: Room browser reactive display
- **WHEN** Mindustry's Join Game screen is opened
- **THEN** the PlayerConnect room section renders room cards that update automatically as the SSE room signal changes

#### Scenario: Mod mismatch warning on join
- **WHEN** a user clicks to join a room requiring different mods
- **THEN** a warning dialog lists missing and unneeded mods, with options to disable conflicting mods and proceed or cancel

