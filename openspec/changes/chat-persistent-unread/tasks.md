## 1. State & Unread Evaluation

- [x] 1.1 Add UUIDv7 comparison utility method to determine chronological order between message IDs
- [x] 1.2 Update `ChatUnread` to manage persistent last-read message IDs via `Core.settings` (`mindustrytool.chat.lastread.<channelId>`) and maintain reactive unread state
- [x] 1.3 Add unit tests in `ChatUnreadTest` covering UUIDv7 comparison, fresh launch without stored read ID, incoming newer messages, and marking channel as read

## 2. Store & Service Integration

- [ ] 2.1 Update `ChatService.refreshChannels()` to register each channel's `lastMessageId` with `ChatUnread` upon receiving `ChannelDto` list
- [ ] 2.2 Update `ChatService` stream event handler to update `ChatUnread` on incoming messages, marking as read if the channel is currently active and chat window is open
- [ ] 2.3 Update `ChatStore.selectChannel()` and window expansion to mark the active channel's latest known message as read

## 3. UI Bindings & Verification

- [ ] 3.1 Verify `ChatChannelListView` reflects the persistent unread indicator dot for channels with unread messages
- [ ] 3.2 Verify `ChatOverlayHudView` collapsed badge reflects the scarlet dot when any channel has unread messages
- [ ] 3.3 Run `./gradlew test` to verify all unit tests pass without regression
