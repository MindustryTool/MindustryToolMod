## 1. Assets & Localization

- [ ] 1.1 Add `bell.png` and `bell-off.png` icon drawables to `assets/icons/`
- [ ] 1.2 Add translation keys and descriptive comments to `assets/bundles/bundle.properties` for channel mute/unmute tooltips and status labels

## 2. Reactive State & Persistence

- [ ] 2.1 Implement `ChatNotifications` state container in `mindustrytool.features.chat.state` with `Signal<Set<String>>` initialized and persisted via `Core.settings`
- [ ] 2.2 Expose `ChatNotifications` on `ChatStore` and provide clean reactive query methods (`isMuted(channelId)`, `toggleMuted(channelId)`)
- [ ] 2.3 Update `ChatUnread.total()` to filter out unread counts from muted channels

## 3. UI Implementation

- [ ] 3.1 Refactor `ChatChannelListView.ChannelItem` to display the interactive notification toggle button when selected
- [ ] 3.2 Render a subtle static muted icon in `ChatChannelListView.ChannelItem` when unselected and muted
- [ ] 3.3 Ensure the toggle button click action operates cleanly without interfering with outer channel selection

## 4. Testing & Verification

- [ ] 4.1 Add unit tests for `ChatNotifications` verifying toggling, state updates, and settings persistence
- [ ] 4.2 Add unit tests for `ChatUnread` verifying muted channels are excluded from `total()`
- [ ] 4.3 Run existing chat test suite to verify no regressions
