## Context

Currently, the chat feature manages unread status purely in memory using `ChatUnread` with integer counters. When Mindustry restarts, all unread counters reset to 0. When channels are retrieved via `MindustryTool.getChatChannels()`, each `ChannelDto` contains `lastMessageId`, which is a UUIDv7 generated on the server representing the most recent message in that channel.

Because UUIDv7 embeds a Unix millisecond timestamp in the most significant 48 bits, chronological ordering can be determined by comparing two UUIDv7 values.

## Goals / Non-Goals

**Goals:**
- Persist the last-read message ID for each channel in `Core.settings`.
- Determine unread status per channel by comparing the latest known message ID (`ChannelDto.lastMessageId` or incoming stream message ID) against the stored last-read message ID.
- Enable unread indicators immediately after loading channels without waiting for full message history to be fetched.
- Provide reactive unread signals for individual channels (used by `ChatChannelListView`) and total unread presence (used by `ChatOverlayHudView` collapsed badge).
- Update the stored last-read ID in `Core.settings` whenever the user views the channel (opens chat window on active channel, or selects the channel while open).

**Non-Goals:**
- Server-side read receipts (syncing read status back to the backend API).
- Storing full message history locally in a database or file (messages remain dynamically loaded on demand).
- Viewport-position-based reading (requiring users to scroll to bottom); having the channel active while the window is open marks all current messages as read.

## Decisions

### 1. Lexicographical String Comparison for UUIDv7
- **Choice**: Use standard Java string comparison: `serverLastId.compareTo(localLastReadId) > 0`.
- **Rationale**: RFC 9562 guarantees that canonical lowercase UUIDv7 strings are chronologically ordered when sorted lexicographically. This avoids unnecessary `UUID` object instantiation, parsing overhead, or 64-bit signed `long` overflow edge cases.
- **Alternative considered**: Parsing into `UUID` or manual bit extraction. String comparison is zero-allocation and straightforward.

### 2. Per-Channel Setting Keys in `Core.settings`
- **Choice**: Store each channel's last-read message ID under the key `mindustrytool.chat.lastread.<channelId>`.
- **Rationale**: Direct, atomic reads and writes via `Core.settings.getString(...)` and `Core.settings.put(...)`. Avoids the overhead of serializing and deserializing a JSON map every time a message is read.
- **Alternative considered**: A single JSON-encoded map in one settings key. Rejected because modifying one channel would require parsing and re-serializing the entire dictionary.

### 3. Read Trigger Lifecycle
- **Choice**: When the chat window is expanded and a channel is active, that channel's latest known message ID is written to `Core.settings`. If new messages arrive via stream while the window is open on that active channel, the stored ID is immediately updated to the incoming message's ID.
- **Rationale**: Matches user expectation that viewing a channel marks its messages as read.
- **Alternative considered**: Requiring scrolling to the exact bottom. In a virtualized list, this can cause missed read triggers if content is short or padding is varied.

### 4. First-Time Launch Behavior
- **Choice**: If no stored read ID exists for a channel, and the channel has a non-null `lastMessageId`, the channel is marked as unread.
- **Rationale**: Confirmed with user. Ensures users see new/existing messages upon clean installation.

## Risks / Trade-offs

- **[Risk] Null or invalid UUID strings** → **Mitigation**: Guard against null, empty strings, and temporary client IDs (e.g. optimistic send IDs). If either string is null or empty, handle safely: if `latestId` is null/empty, not unread; if `storedReadId` is null/empty and `latestId` exists, treat as unread.
- **[Risk] Race between channel refresh and live stream** → **Mitigation**: Track the latest known message ID per channel in memory (initialized from `ChannelDto.lastMessageId` and updated by incoming messages), ensuring the comparator always evaluates the newest known ID.
