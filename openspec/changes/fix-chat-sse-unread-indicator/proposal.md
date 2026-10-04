## Why

When new chat messages arrive in real-time over the Server-Sent Events (SSE) connection, the channel unread indicator dot in the room/channel list (`ChatChannelListView`) fails to display. This occurs because:
1. `ChatUnread.forChannel()` cached `Computed<Integer>` instances created inside a component's build scope, causing ambient `OwnershipContext` to dispose the computed when that component unmounted, permanently severing reactivity for all future renders of that channel item.
2. In `ChatService.onEvent()`, incoming messages were automatically marked as read whenever the chat window was open and the message belonged to `store.channels().currentActiveId()`, ignoring mobile views where the user is looking at the channels list (`mobileTab == 0`) and not the message feed.

## What Changes

- Modify `ChatUnread.forChannel(channelId)` to derive a fresh `unreads.map(...)` without caching disposed computeds in a long-lived map.
- Update `ChatService` and `ChatFeature` to check actual message feed visibility (`isFeedVisible(channelId)`) rather than just raw window open state, so messages are only auto-marked as read if the feed is actively visible.
- Add unit tests validating that unread reactive streams remain active across component rebuilds and properly reflect SSE message increments.

## Capabilities

### New Capabilities
- `chat-unread-indicator`: Channel unread count reactive propagation and visibility tracking for incoming chat messages.

### Modified Capabilities

None.

## Impact

- `mindustrytool.features.chat.state.ChatUnread`: Removed `channelComputeds` cache in favor of clean per-component derivation.
- `mindustrytool.features.chat.ChatService`: Accepts a feed visibility predicate instead of a plain window-open supplier.
- `mindustrytool.features.chat.ChatFeature`: Implements feed visibility check factoring in mobile tab state and collapse status.
- `mindustrytool.features.chat.ChatChannelListView`: Retains full reactivity when channels list items mount and unmount.
