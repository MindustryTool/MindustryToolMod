## Why

When players share PlayerConnect room invite links (`player-connect://...`) in chat, the current UI only displays a plain, uninformative card with the raw link string and a copy button. Players cannot see the room name, map name, gamemode, player count, or join the room directly from chat. Furthermore, the chat item height calculation in `ChatMessageHeightCalculator` uses a hardcoded legacy value (`80f`) that does not reflect a proper rich room card, which can cause layout overlap or visual jitter in Solim's virtualized list.

## What Changes

- Render rich room information (room title, secured status, map name, gamemode, player count, and compatibility) for `player-connect://` invite messages in `ChatMessageListView.buildMessageBody()` when room metadata is available in `PlayerConnectFeature.getRooms()`.
- Provide direct action buttons on the room card:
  - When room data is found: a primary "Join" button (supporting password prompts and opening `JoinWarningDialog` for mod discrepancies) and a "Copy Link" action.
  - When room data is unlisted/offline: a "Try Connect" button and a "Copy Link" button.
- Unify the vertical card layout height across both active and offline/fallback states (fixed 108px / `unit(27)`) using single-line truncated text rows with ellipsis to prevent cumulative layout shifts (CLS) when rooms load asynchronously.
- Update `ChatMessageHeightCalculator` to use the accurate uniform card height (`INVITE_CARD_HEIGHT = 108f`), ensuring pixel-perfect `VirtualList` layout measurement and smooth scrolling.
- Gracefully handle cases where `PlayerConnectFeature` is disabled or not registered, falling back to the standard invite card.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `chat`: Extend chat message body rendering and height measurement for `RoomInviteMessage` to display live PlayerConnect room details, compatibility status, interactive join actions, and a unified 108px fixed card layout.

## Impact

- `mod/src/mindustrytool/features/chat/ChatMessageListView.java`: updates `buildMessageBody()` to observe `PlayerConnectFeature.getRooms()` and render the rich 4-row room card.
- `mod/src/mindustrytool/features/chat/ChatMessageHeightCalculator.java`: updates `INVITE_CARD_HEIGHT` constant from `80f` to `108f`.
- `assets/bundles/bundle.properties`: adds translation keys for room card labels (status, offline/unlisted, connect actions).
- `mod/src/test/java/mindustrytool/features/chat/ChatMessageGrouperAndHeightTest.java`: updates test expectations to match the new `INVITE_CARD_HEIGHT`.
