## ADDED Requirements

### Requirement: PlayerConnect room invite card rendering and direct join
The chat system SHALL render typed `RoomInviteMessage` entries as a dedicated interactive card with a fixed 108px layout height, resolving room metadata reactively from `PlayerConnectFeature.getRooms()`. When room data is available, the card SHALL display the room title, security status, map name, gamemode, player count, and compatibility status, with a "Join" action and "Copy Link" action. When room data is unavailable or unlisted, the card SHALL display the link string and offline status with a "Try Connect" action and "Copy Link" action.

#### Scenario: Rendering live room data from PlayerConnectFeature
- **WHEN** a `RoomInviteMessage` is displayed and its link matches an active room in `PlayerConnectFeature.getRooms()`
- **THEN** the card renders the room name, lock icon if secured, map name, gamemode, player count, and a primary "Join" button within a 108px card height

#### Scenario: Joining a password-protected room from chat
- **WHEN** the user clicks "Join" on a secured room invite card
- **THEN** a password input dialog is displayed prior to initiating the connection

#### Scenario: Joining a room with mod mismatches from chat
- **WHEN** the user clicks "Join" on a room invite card that has missing or unneeded mods
- **THEN** a `JoinWarningDialog` is displayed showing the mod differences before connecting

#### Scenario: Rendering fallback card for unlisted or offline room
- **WHEN** a `RoomInviteMessage` is displayed and no matching room exists in `PlayerConnectFeature.getRooms()`
- **THEN** the card renders the raw connect link, an offline/unlisted status label, a "Try Connect" button, and a "Copy Link" button, preserving the exact 108px card height

## MODIFIED Requirements

### Requirement: Pre-rendering grouping and height caching
The chat system SHALL group consecutive messages from the same author before rendering and calculate layout heights using static dimensions for fixed components (`INVITE_CARD_HEIGHT = 108f`, `SCHEMATIC_CARD_HEIGHT = 212f`, `IMAGE_CARD_HEIGHT = 140f`, `TOOL_LINK_CARD_HEIGHT = 70f`) and `GlyphLayout` for wrapped text, caching heights keyed by container width.

#### Scenario: Grouping consecutive author messages
- **WHEN** multiple consecutive messages in the active channel share the same author ID
- **THEN** the first message is marked with header and avatar layout, while subsequent messages are marked with text indentation and zero header height

#### Scenario: Cached height calculation by container width
- **WHEN** message layout heights are requested for a given container width
- **THEN** static components use fixed heights, text is measured once against available width using `GlyphLayout`, and the computed height is cached until container width changes

#### Scenario: Uniform invite card height calculation
- **WHEN** `ChatMessageHeightCalculator.calculateHeight()` processes a `RoomInviteMessage`
- **THEN** it calculates `INVITE_CARD_HEIGHT` at 108px scaled height regardless of whether live room metadata has arrived
