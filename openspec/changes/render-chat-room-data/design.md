## Context

In MindustryTool, chat allows players to communicate and share game items, including PlayerConnect room invites formatted as `player-connect://<host>:<port>/<roomId>`. Currently, `ChatMessageListView.buildMessageBody()` displays a generic card for `RoomInviteMessage` with only the raw link text and a copy button. 

Meanwhile, `PlayerConnectFeature` actively maintains a reactive `Signal<List<PlayerConnectRoom>>` via server-sent events (SSE) and polling, containing rich metadata (room name, map name, gamemode, player count, security lock status, and active mod lists).

Additionally, the chat message feed uses `VirtualList`, which relies on `ChatMessageHeightCalculator.calculateHeight()` to calculate item positions. `ChatMessageHeightCalculator` currently uses a hardcoded legacy value of `80f` for `INVITE_CARD_HEIGHT`. To ensure smooth scrolling and avoid overlapping elements in `VirtualList`, the visual layout and the calculated height must be in exact alignment.

## Goals / Non-Goals

**Goals:**
- Render rich room information (title, map, mode, player count, mod compatibility) in `ChatMessageListView.buildMessageBody()` whenever room data is available in `PlayerConnectFeature.getRooms()`.
- Provide direct join capability: when clicking "Join", prompt for password if secured, show `JoinWarningDialog` if mod mismatches exist, or directly connect via `PlayerConnectClient.join(...)`.
- Provide fallback rendering with a "Try Connect" button and a "Copy Link" button when a room is not found in `PlayerConnectFeature.getRooms()` or if `PlayerConnectFeature` is disabled.
- Enforce a fixed uniform height of 108px (`unit(27)`) across both active and offline/fallback states using single-line truncated text with `.ellipsis()`, preventing cumulative layout shifts (CLS) when room data arrives asynchronously.
- Update `ChatMessageHeightCalculator.INVITE_CARD_HEIGHT` to `108f` to match the exact card height.

**Non-Goals:**
- Modifying the underlying SSE protocol or `PlayerConnectFeature` room discovery architecture.
- Full mod download manager integration directly inline in the chat bubble (delegated to `JoinWarningDialog`).
- Inline chat player list expansion (detailed player lists belong in the dedicated room browser).

## Decisions

### 1. Fixed Uniform Card Height (108px) across Active & Offline States
* **Decision**: Both active room cards and offline/fallback cards share an identical 4-row layout structure with an overall card height of 108px (`unit(27)`), and all text rows use `.ellipsis()` to prevent multiline wrapping.
* **Rationale**: When the chat opens, `PlayerConnectFeature.getRooms()` may be empty for several hundred milliseconds while SSE establishes. If active and offline cards had different heights, the initial calculation would cache the offline height in `ChatMessageHeightCalculator.HEIGHT_CACHE`, and once SSE data loaded, the card would expand and overflow the container or cause visual layout jumping (CLS). Uniform height guarantees zero layout shift and 100% stable `VirtualList` scrolling.
* **Alternatives Considered**: Dynamic variable heights requiring cache invalidation and `VirtualList.recalculateHeights()` on every SSE update. Rejected because it causes noticeable scroll jumping while users are reading chat.

### 2. Reactive Room Resolution via `PlayerConnectFeature.getRooms()`
* **Decision**: In `ChatMessageListView`, resolve room metadata by obtaining `PlayerConnectFeature` from `FeatureManager.getFeature(PlayerConnectFeature.class)` and creating a computed signal:
  `Readable<PlayerConnectRoom> roomSignal = (pc != null) ? pc.getRooms().map(rooms -> findRoom(rooms, link)) : Signal.of(null);`
  The card's interior is bound via `dynamic(roomSignal, room -> ...)`.
* **Rationale**: Automatic reactive updates without polling or manual event subscription. If a room closes or player count changes while viewing chat, the card updates smoothly in place.
* **Alternatives Considered**: Static one-time lookup at parse time. Rejected because `ChatMessageParser` runs before rooms are fetched and message models are immutable/cached.

### 3. Join Action Flow Matching `RoomCard.java`
* **Decision**: Mirror the interaction pattern in `RoomCard.java`:
  1. If protocol version mismatches: disable the button (`[scarlet]Incompatible`).
  2. If `data.isSecured()`: open password text input dialog.
  3. If missing or unneeded mods exist: open `JoinWarningDialog`.
  4. Otherwise: initiate join via `PlayerConnectClient.join(link, password, () -> {})`.
* **Rationale**: Provides consistent user experience between Room Browser and Chat feed while reusing existing battle-tested dialogs.

## Risks / Trade-offs

- **[Risk]** Room invite link posted with different formatting (e.g. trailing slashes or IP aliases).  
  → **Mitigation**: `findRoom` matches by exact link string first, and falls back to comparing `roomId` parsed via `PlayerConnectLink.fromString(link)`.

- **[Risk]** `PlayerConnectFeature` disabled or uninitialized in tests or user settings.  
  → **Mitigation**: `FeatureManager.getFeature(PlayerConnectFeature.class)` returns null or disabled; gracefully falls back to the 108px unlisted card with "Copy Link" and "Try Connect".

- **[Risk]** Extremely long room or map names pushing action buttons off screen.  
  → **Mitigation**: Apply `.ellipsis()` on all title and status labels so they truncate cleanly.
