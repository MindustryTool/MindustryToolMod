## Why

Currently, chat unread state is strictly in-memory and ephemeral (`ChatUnread`). When the user starts or restarts the game, all unread counters reset to zero, meaning players miss unread messages sent while they were offline. Furthermore, channels only show unread counters when live SSE messages arrive during the current session, ignoring the server-provided `ChannelDto.lastMessageId`.

Because chat message IDs are UUIDv7 (which encode chronological Unix epoch timestamps in their most significant bits), the client can determine whether unread messages exist by comparing the server's `lastMessageId` against the locally stored last-read message ID in `Core.settings`, without having to fetch or load the message history into memory.

## What Changes

- Store the last-read message ID per channel in `Core.settings` (`mindustrytool.chat.lastread.<channelId>`).
- Compare UUIDv7 message IDs lexicographically (`idA.compareTo(idB) > 0`) to determine whether a channel has newer unread messages.
- Compute channel unread status immediately upon fetching `ChannelDto` lists from the server, even if message history has not been downloaded.
- Display an unread indicator circle on channels with unread messages in the channel list, and on the collapsed floating chat badge when any channel has unread messages.
- Update the stored last-read message ID in `Core.settings` and clear the unread indicator when the user opens the chat window or switches to that channel while open.
- On first launch / when no stored message ID exists for a channel, treat the channel as unread if it has a non-null `lastMessageId`.

## Capabilities

### Modified Capabilities
- `chat-feature`: Persist last-read message IDs in `Core.settings`, compare UUIDv7 timestamps with channel metadata and stream messages to track unread status reactively, and update stored read IDs when channels are viewed.
- `chat-overlay`: Bind channel list items and the collapsed HUD badge to reactive persistent unread state, displaying unread circles and updating read status upon opening/viewing channels.

## Impact

- `mindustrytool.features.chat.state.ChatUnread`: Refactor/enhance to track persistent read state via `Core.settings` and evaluate unread status based on UUIDv7 comparison.
- `mindustrytool.features.chat.ChatStore` & `ChatService`: Coordinate channel loading, stream events, and channel selection with persistent read tracking.
- `mindustrytool.features.chat.ChatChannelListView` & `ChatOverlayHudView`: Reactively reflect persistent unread circles on channel rows and the collapsed badge.
- `Core.settings`: New keys under `mindustrytool.chat.lastread.<channelId>`.
