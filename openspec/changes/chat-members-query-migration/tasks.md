## 1. UI State Single Source of Truth

- [x] 1.1 Refactor `ChatUiState` to bind directly to `ConfigValue<Boolean>` signals for `channelsCollapsed` and `usersCollapsed`
- [x] 1.2 Remove the 4 bidirectional `.subscribe()` bridging listeners between `ConfigValue`s and `ChatUiState` in `ChatFeature.java`

## 2. Query-Backed ChatMembers State

- [x] 2.1 Refactor `ChatMembers.java` to replace manual `HashMap` state with a declarative `Query<List<ChatUser>>` tracking `activeChannelId.get()`
- [x] 2.2 Expose `query()`, `currentActive()`, and reactive helper methods on `ChatMembers`

## 3. ChatService & Subscription Cleanup

- [x] 3.1 Remove imperative `activeId.subscribe(...)` and `loadUsers(channelId)` from `ChatService.java`
- [x] 3.2 Update `ChatService.refresh(channelId)` to trigger `store.members().query().refetch()` instead of imperative fetching

## 4. Declarative ChatUserListView

- [x] 4.1 Refactor `ChatUserListView.java` to replace nested `dynamic()` trees with declarative `query(store.members().query())` (`QueryView`)
- [x] 4.2 Render loading, retryable error, and empty roster states ("No members online.") declaratively within `QueryView`

## 5. Verification & Tests

- [x] 5.1 Update unit tests in `ChatChannelsAndMembersTest.java` to validate `ChatMembers` query reactivity and state transitions
- [x] 5.2 Run automated tests (`./gradlew test`) to verify regressions and ensure all test suites pass
