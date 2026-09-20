## Why

The chat user list fails to load when a user first opens the chat menu, remaining empty ("No members online.") until the user manually switches to a different channel. This bug is caused by relying on imperative `.subscribe()` listeners—which do not fire on initial mount—and maintaining duplicate UI state synchronized through manual `.subscribe()` bridges. Migrating `ChatMembers` to a declarative `Query` backed by `QueryCache` and adopting `ConfigValue` as the single source of truth for panel state eliminates these fragile subscription chains and guarantees that channel members load automatically whenever the view mounts or the active channel changes.

## What Changes

- **Query-Backed Member Roster**: Refactor `ChatMembers` from manual `HashMap` state and imperative `service.loadUsers(...)` calls to encapsulate a declarative `Query<List<ChatUser>>` that reactively tracks `activeChannelId.get()`.
- **Declarative ChatUserListView**: Replace nested manual `dynamic()` loading and error fallback trees in `ChatUserListView` with declarative `query(store.members().query())` (`QueryView`), leveraging automatic mount-time `ensureFresh()` checks.
- **ConfigValue as Single Source of Truth**: Remove duplicate `channelsCollapsed` and `usersCollapsed` signals from `ChatUiState` and eliminate the 4 bidirectional `.subscribe()` bridging blocks in `ChatFeature`, binding UI controls directly to `ConfigValue` signals.
- **Elimination of Imperative Subscription Callers**: Remove `activeId.subscribe(...)` from `ChatService` and manual uncollapse hooks from `ChatFeature`, relying purely on reactive query dependency tracking and view mount triggers.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `chat`: Member roster state management transitions from imperative `ChatService.loadUsers()` and manual `HashMap` state to a declarative `Query<List<ChatUser>>` in `ChatMembers`, rendered via `QueryView` in `ChatUserListView` with automatic mount-time freshness checks.

## Impact

- Modified files in `mod`:
  - `ChatMembers.java`: Encapsulates `Query<List<ChatUser>>` tracking `activeChannelId`.
  - `ChatUserListView.java`: Uses declarative `query(...)` instead of nested `dynamic()` trees.
  - `ChatUiState.java`: Uses `ConfigValue<Boolean>` / underlying signals directly instead of duplicate state.
  - `ChatFeature.java`: Removes 4 `.subscribe()` sync blocks for panel collapse configs.
  - `ChatService.java`: Removes imperative `activeId.subscribe(...)` and delegates member fetching to `ChatMembers` query.
  - Unit tests in `ChatChannelsAndMembersTest.java`.
- No breaking API changes to other features.
