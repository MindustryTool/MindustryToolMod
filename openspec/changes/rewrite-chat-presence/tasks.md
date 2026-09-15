## 1. Spike & Contract Confirmation

- [ ] 1.1 Log `StateChangeEvent.from/to` while opening pause menu, host/join dialogs, and quitting to menu; confirm the transient-`menu` flicker source and adjust sticky rules if the source is `paused` instead
- [ ] 1.2 Confirm `PUT /chats/users/state` accepts the legacy string payloads via `MindustryTool.updateChatState` (manual check against backend)

## 2. Session Presence Signals

- [ ] 2.1 Add `presence` and `lastNonMenu` signals (`menu` init) plus accessors to `ChatSession`, following the existing `connected` signal pattern
- [ ] 2.2 Verify no existing `ChatStore` consumers break (composition untouched, signals default to `menu`)

## 3. PlayerConnect Room Events

- [ ] 3.1 Add immutable `PcRoomOpened` (resolved room name payload) and `PcRoomClosed` event classes in the playerconnect feature
- [ ] 3.2 Fire `PcRoomOpened` on room-host success and `PcRoomClosed` on room close in `PlayerConnectFeature`, with no imports from chat classes

## 4. Presence Sync Listener

- [ ] 4.1 Register one-time chat-side observers for `ClientServerConnectEvent`, `StateChangeEvent`, `WorldLoadEndEvent`, `PcRoomOpened`, `PcRoomClosed`, owned by `ChatFeature` but running independent of `onEnable`/`onDisable`
- [ ] 4.2 Implement sticky intent resolution (non-menu freshens both signals, menu stashes, playing-without-map restores, PC wins, skip menu intents while graphics hidden)
- [ ] 4.3 Implement 1s last-wins debounce with distinct-until-changed against last-sent value, publishing via `MindustryTool.updateChatState`
- [ ] 4.4 Add intent generation counter so stale `pingHost` resolutions are discarded
- [ ] 4.5 Add 5-minute heartbeat re-PUT of current presence while eligible
- [ ] 4.6 Gate all PUTs on logged-in + `share-presence` opt-in only; force re-PUT on login, opt-in, stream reconnect, and enable transitions; mark dirty and retry on failure

## 5. Opt-Out Setting & UI

- [ ] 5.1 Add default-on `share-presence` config value to the chat config group with a reactive signal
- [ ] 5.2 Add toggle row for the setting in `ChatSettingsView`
- [ ] 5.3 Add bundle keys with translator comments for the toggle label and description; reuse existing keys where applicable

## 6. Framework Tests

- [ ] 6.1 Add solim-module tests for sticky resolution rules (menu stash, restore, PC priority)
- [ ] 6.2 Add solim-module tests for debounce coalescing, distinct suppression, and generation-counter drops
- [ ] 6.3 Add solim-module tests for gating (logged-out and opted-out produce no PUTs; opt-in triggers re-PUT)

## 7. Verification

- [ ] 7.1 Run the full Java 8 compatibility, `Nullable`, import, ternary, and i18n checks per `AGENTS.md`
- [ ] 7.2 Manual end-to-end check with two clients (play, pause, server join, relay host, quit to menu, opt-out); stop Mindustry via MCP `stop` afterwards
