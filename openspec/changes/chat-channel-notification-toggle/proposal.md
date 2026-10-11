## Why

Players active in Mindustry chat may participate in high-traffic channels that cause frequent visual distraction through unread indicator dots and the minimized HUD badge red dot. Currently, there is no way to mute or toggle notifications per channel. Enabling players to toggle notifications on/off directly from selected channels allows them to silence noisy channels while keeping important ones unmuted.

## What Changes

- Add a per-channel notification toggle button to the channel item row in `ChatChannelListView` when the channel is selected.
- Display a subtle muted icon on unselected channels when they are muted so their mute status is identifiable at a glance.
- Completely suppress unread indicators (both the channel's unread dot and its contribution to the global collapsed HUD badge) for muted channels.
- Persist muted channel preferences per-channel across game sessions via `Core.settings`.
- Provide localized tooltips and labels in `bundle.properties` following `AGENTS.md` guidelines.

## Capabilities

### New Capabilities
- `chat-channel-notifications`: Per-channel notification state management, persistence, and interactive toggle control in the channel list item.

### Modified Capabilities
- `chat-unread-indicator`: Suppress unread indicators and exclude unread counts from the global unread total for muted channels.

## Impact

- `mindustrytool.features.chat.ChatChannelListView`: Updated channel item row layout to include the notification toggle button when selected and static muted icon when unselected.
- `mindustrytool.features.chat.ChatStore` & `mindustrytool.features.chat.state`: Added reactive notification state container (`ChatNotifications`) and integrated with `ChatUnread`.
- `mindustrytool.features.chat.state.ChatUnread`: Updated unread visibility/calculation to account for muted channels.
- `assets/bundles/bundle.properties`: Added localized translation keys and comments.
- `assets/icons/`: Added `bell.png` and `bell-off.png` icon drawables.
