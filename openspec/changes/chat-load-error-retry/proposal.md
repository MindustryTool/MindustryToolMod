## Why

When fetching the chat channel list, initial chat messages, or member roster fails due to network or server issues, the chat interface currently treats failures as empty results without recording or displaying error states. Users see misleading empty placeholders ("No channels available.", "No messages yet.", "No members online.") with zero visual feedback that a failure occurred and no mechanism to retry loading. On mobile devices where the chat HUD defaults to the Messages tab, failed channel initialization leaves the user on a blank screen with no indication that channels failed to load.

## What Changes

- Add reactive `loading` and `error` state tracking to `ChatChannels`, `ChatMessages`, and `ChatMembers`.
- Update `ChatService` to set loading/error signals across `refreshChannels()`, `loadMessages()`, and `loadUsers()`, ensuring all state updates are posted to the main thread (`Core.app.post`).
- Introduce dedicated Error & Retry UI states across `ChatChannelListView`, `ChatMessageListView`, and `ChatUserListView` with a centered warning icon, localized error message, technical cause subtext, and a Retry button (`WebStyles.secondary()`).
- Display `Loader.centered()` during active fetch operations.
- Preserve existing visible messages and members on subsequent background/manual refresh failures, showing a non-intrusive retry banner instead of blanking out active conversation.
- Wire cross-state awareness on mobile: when no channel is active because channels failed to load, the Messages and Members views show a channel failure notice with a direct "Retry Channels" button.
- Enhance the top-bar refresh button to reload channels if none are selected, or refresh active messages, members, and stream when a channel is active.
- Add translatable bundle keys with descriptive comments to `assets/bundles/bundle.properties`.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `chat`: Add loading indicators, error feedback with technical details, and retry actions for channels, initial messages, and member roster.

## Impact

- `mindustrytool.features.chat.state`: `ChatChannels`, `ChatMessages`, `ChatMembers`
- `mindustrytool.features.chat`: `ChatService`, `ChatChannelListView`, `ChatMessageListView`, `ChatUserListView`, `ChatOverlayHudView`
- `assets/bundles/bundle.properties`: new i18n keys for chat error and retry states.
