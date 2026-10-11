## Context

In `mindustrytool.features.chat`, channels are listed in `ChatChannelListView` where each channel item is displayed as a button allowing the player to select the active channel. Incoming messages increment the unread count in `ChatUnread`, lighting up an unread dot on the channel item and incrementing `store.unread().total()`, which displays a red badge on the minimized floating chat HUD in `ChatOverlayHudView`.

Presently, players cannot mute high-traffic or irrelevant channels. A TODO comment exists in `ChatChannelListView`: `// TODO: Able to turn on/off notifications for each channel`.

## Goals / Non-Goals

**Goals:**
- Provide a clear, non-intrusive notification toggle inside the channel list item when that channel is selected.
- Display a muted bell icon on unselected channels when they are muted, providing clear visibility of channel mute status at a glance.
- Fully suppress unread indicators for muted channels: no unread dot on the channel item and no contribution of muted channel unread counts to the collapsed HUD total badge.
- Persist muted channel IDs across game restarts in `Core.settings`.
- Implement clean reactive bindings in Solim adhering strictly to `AGENTS.md` (no `signal.get()` in `build()`, single source of truth, Java 8 compatibility, proper i18n).

**Non-Goals:**
- Notification toggles in the top HUD window header (decided to keep toggle exclusively in the channel item row).
- Channel-specific audio pings or toast alerts (chat currently has no audio/toast alerts).
- Server-side notification subscription synchronization.

## Decisions

### Decision 1: Dedicated `ChatNotifications` Reactive State Container
- **Choice**: Create `mindustrytool.features.chat.state.ChatNotifications` owned by `ChatStore`.
- **State**: A reactive `Signal<Set<String>> mutedChannelIds` initialized from `Core.settings`.
- **API**:
  - `Readable<Boolean> isMuted(String channelId)`
  - `boolean isCurrentlyMuted(String channelId)`
  - `void toggleMuted(String channelId)`
  - `void setMuted(String channelId, boolean muted)`
- **Persistence**: Persisted under `mindustrytool.chat.muted_channels` as a comma-separated list of channel IDs in `Core.settings`.
- **Rationale**: Isolates notification concerns from channel fetching and unread counting, adheres to single source of truth, and allows direct reactive queries without polling.

### Decision 2: Unread Indicator Suppression
- **Choice**:
  - In `ChatUnread`: Update `total()` computed signal to ignore unread counts for channels present in `ChatNotifications.mutedChannelIds()`.
  - In `ChannelItem`: Channel unread dot is visible only when `hasUnread && !isMuted`.
- **Rationale**: Completely prevents muted channels from distracting the player while they play or browse channels, matching user requirements.

### Decision 3: Channel Item Layout & Hit Targets
- **Choice**: In `ChatChannelListView.ChannelItem`:
  - When selected: Show an interactive toggle button with `WebStyles.ghost()` inside the row containing `bell.png` (when notifications enabled) or `bell-off.png` (when muted).
  - Clicking the toggle button stops event propagation or is placed as a distinct cell so clicking it does not conflict with channel selection.
  - When unselected:
    - If muted: Render a subtle static icon (`bell-off.png`, muted gray color) to the right of the channel name.
    - If unmuted: Render standard channel row without extra icons.
- **Rationale**: Avoids clutter on unselected channels while keeping mute state visible, and provides immediate toggle access upon selection.

### Decision 4: Icon Assets & Localization
- **Choice**: Provide `bell.png` and `bell-off.png` in `assets/icons/` loaded via `FileIcon.of()`.
- **Bundle keys**: Add localized keys in `assets/bundles/bundle.properties` with descriptive comments:
  - `feature.chat.channel.notifications.mute` ("Mute notifications")
  - `feature.chat.channel.notifications.unmute` ("Unmute notifications")
  - `feature.chat.channel.notifications.muted` ("Muted")

## Risks / Trade-offs

- **[Risk] Nested Button click handling in Arc Scene2D** → In Arc/Solim, clicking a child button inside another button can trigger both handlers if not isolated.
  - *Mitigation*: Separate the channel row into a horizontal layout containing a main channel select button (`growX()`) and a trailing notification toggle button, or clear touch events on the toggle button so selecting channel and toggling notifications are distinct actions.
- **[Risk] Channel ID changes or deletions across server versions** → Persisted comma-separated IDs could accumulate obsolete channel IDs.
  - *Mitigation*: Negligible overhead for small strings; `ChatNotifications` operates on standard sets and cleans up gracefully.
