# chat-presence Specification

## Purpose

Outbound game-presence publishing: the client publishes the player's current game activity (menu, server, relay room, campaign, editor, custom game) to the backend chat service so other users see accurate presence.

## Requirements

### Requirement: Sticky presence state in ChatSession
The system SHALL hold outbound presence in `ChatSession` as a current-presence signal and a last-non-menu signal, both initialized to `menu`, using the legacy wire strings verbatim (`menu`, `server: <name>`, `player-connect: <name>`, `campaign: <map>`, `editing: <map>`, `custom-game`).

#### Scenario: Non-menu intent updates both signals
- **WHEN** a non-menu presence intent arrives
- **THEN** the current-presence candidate and the last-non-menu signal are both set to the intent value

#### Scenario: Menu intent stashes previous presence
- **WHEN** a `menu` intent arrives while current presence is non-menu
- **THEN** the last-non-menu signal retains the pre-menu presence and the current-presence candidate becomes `menu`

#### Scenario: Playing-without-map restores sticky presence
- **WHEN** a playing-state intent arrives before world details are known
- **THEN** the current-presence candidate resolves to the last-non-menu signal value rather than `menu` or empty

#### Scenario: PlayerConnect presence takes priority
- **WHEN** the player hosts a relay room while in a local game
- **THEN** the resolved presence is the `player-connect: <name>` intent, not the local game mode

### Requirement: Event-driven presence intents
The system SHALL derive presence intents from Mindustry lifecycle events (`ClientServerConnectEvent`, `StateChangeEvent`, `WorldLoadEndEvent`) and from PlayerConnect room events, with `PlayerConnectFeature` firing immutable `PcRoomOpened` (carrying the resolved room name) and `PcRoomClosed` events observed by the chat-side sync without cross-feature imports.

#### Scenario: Opening a relay room publishes its name
- **WHEN** `PlayerConnectFeature` successfully opens a room named `MyRelay`
- **THEN** a `PcRoomOpened` event carrying `MyRelay` is fired for the presence sync to observe

#### Scenario: Closing a relay room ends its presence
- **WHEN** the active relay room closes
- **THEN** a `PcRoomClosed` event is fired and subsequent presence resolves from game state rather than the room name

#### Scenario: Minimized client does not report menu
- **WHEN** a menu state change arrives while graphics are hidden
- **THEN** no menu intent is produced and presence is unchanged

### Requirement: Debounced presence sync with heartbeat
The system SHALL publish presence via `MindustryTool.updateChatState`, coalescing rapid intents with a 1-second last-write-wins debounce, skipping PUTs whose value equals the last-sent value, re-PUTting current presence every 5 minutes, and applying asynchronous server-name resolutions only when their intent generation is still the latest.

#### Scenario: Rapid flicker produces a single PUT
- **WHEN** intents change multiple times within the debounce window
- **THEN** only one PUT is sent carrying the latest intent value

#### Scenario: Unchanged presence sends nothing
- **WHEN** the debounced presence equals the last-sent value
- **THEN** no PUT is sent, except for the 5-minute heartbeat re-PUT

#### Scenario: Stale server-name reply is dropped
- **WHEN** a server-name resolution returns after a newer connect intent arrived
- **THEN** the stale resolution is discarded and presence reflects the newer intent

#### Scenario: Heartbeat heals drift
- **WHEN** 5 minutes pass with no presence change while eligible to sync
- **THEN** the current presence is re-PUT

### Requirement: Always-on sync with opt-out setting
The system SHALL run presence sync whenever the user is logged in and the `share-presence` chat setting (enabled by default) is on, independent of chat overlay state, stream connectivity, and Chat feature enablement; when the setting is off or the user is logged out, the system SHALL stop sending presence PUTs without transmitting a final state.

#### Scenario: Sync works without open chat or stream
- **WHEN** the user is logged in with sharing enabled while the chat overlay is closed and the stream is disconnected
- **THEN** presence intents are still published via PUT

#### Scenario: Opting out stops sync silently
- **WHEN** the user turns off the sharing setting
- **THEN** no further presence PUTs are sent and no final state is transmitted

#### Scenario: Sharing resumes on opt-in
- **WHEN** the user re-enables the sharing setting while logged in
- **THEN** the current presence is published promptly

#### Scenario: Logout stops sync silently
- **WHEN** the user logs out
- **THEN** no further presence PUTs are sent and no final state is transmitted
