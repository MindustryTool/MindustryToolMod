## 1. Localization & Bundle Keys

- [x] 1.1 Add descriptive i18n translation keys to `assets/bundles/bundle.properties` for room invite card labels (e.g. `feature.chat.ui.try-connect`, `feature.chat.ui.unlisted-offline`, `feature.chat.ui.compatible`, `feature.chat.ui.mods-required`, `feature.chat.ui.incompatible-protocol`) with comments directly above each key.

## 2. Height Calculation & Constants

- [x] 2.1 Update `ChatMessageHeightCalculator.INVITE_CARD_HEIGHT` from `80f` to `108f` (`unit(27)`).
- [x] 2.2 Update unit tests in `ChatMessageGrouperAndHeightTest.java` to verify that `RoomInviteMessage` calculates the expected height with the new `INVITE_CARD_HEIGHT = 108f`.

## 3. Chat Room Card Rendering

- [x] 3.1 Implement room lookup helper to match `RoomInviteMessage.getConnectLink()` against `PlayerConnectRoom` list by exact link and parsed `roomId`.
- [x] 3.2 Update `ChatMessageListView.buildMessageBody()` for `RoomInviteMessage` to observe `PlayerConnectFeature.getRooms()` via `Readable<PlayerConnectRoom>`.
- [x] 3.3 Build the 4-row active room card displaying room title, lock icon if secured, copy button, map/gamemode, player count, mod compatibility badge, and primary "Join" button.
- [x] 3.4 Wire the "Join" button to validate protocol compatibility, prompt for password if secured, open `JoinWarningDialog` for mod conflicts, or connect via `PlayerConnectClient.join()`.
- [x] 3.5 Build the 4-row fallback card for unlisted/offline rooms or when `PlayerConnectFeature` is unavailable, displaying the connect link with ellipsis, offline status badge, "Try Connect" button, and "Copy Link" button.
- [x] 3.6 Ensure the card layout strictly enforces fixed 108px (`unit(27)`) height with `.ellipsis()` on text rows to prevent wrapping and layout shift.

## 4. Verification & Testing

- [x] 4.1 Run unit test suite (`./gradlew :mod:test`) to verify height calculations, parser tests, and grouping tests pass.
- [x] 4.2 Verify zero compilation errors, proper Solim reactive bindings, and adherence to AGENTS.md rules (no FQCNs, ternary preference, Java 8 runtime API compatibility).
