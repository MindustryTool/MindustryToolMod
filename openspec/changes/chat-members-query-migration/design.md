## Context

The chat feature previously managed channel members through a legacy imperative pattern:
1. `ChatMembers` maintained manual `HashMap` signals for `members`, `loading`, and `errors`.
2. `ChatService` listened to channel changes via `store.channels().activeId().subscribe(...)` and imperatively invoked `loadUsers(channelId)`.
3. Because `Signal.subscribe()` does not emit the current value on subscription, restoring a persisted `activeChannelConfig` at startup resulted in no channel transition and therefore `loadUsers` was never called on launch.
4. When uncollapsing the chat window, `ChatFeature` called `syncActiveChannelSilently(activeId)` which only fetched messages, leaving members unpopulated.
5. In addition, `ChatUiState` maintained duplicate `channelsCollapsed` and `usersCollapsed` signals synchronized with `ConfigValue` via four manual `.subscribe()` callbacks, violating single-source-of-truth guidelines.

## Goals / Non-Goals

**Goals:**
- Make `ChatMembers` encapsulate a declarative `Query<List<ChatUser>>` tracking `activeChannelId.get()` as the single source of truth for member roster data.
- Refactor `ChatUserListView` to use declarative `query(...)` (`QueryView`), leveraging automatic mount-time `ensureFresh()` behavior.
- Bind `ChatUiState` directly to `ConfigValue` signals for panel collapse state, eliminating all manual `.subscribe()` sync bridges.
- Eliminate imperative `activeId.subscribe(...)` and `loadUsers(...)` orchestration from `ChatService` and `ChatFeature`.

**Non-Goals:**
- Changing server endpoints (`/chats/users`) or payload contracts.
- Modifying message stream (SSE) or optimistic message sending.
- Changing mobile tab navigation structure (outside of using QueryView for the Members tab).

## Decisions

### Decision 1: Query-Backed Member Roster in `ChatMembers`
- **Choice**: `ChatMembers` encapsulates a `Query<List<ChatUser>>` with a 30-second stale time:
  ```java
  public final class ChatMembers {
      private final Query<List<ChatUser>> query;

      public ChatMembers(Readable<String> activeChannelId) {
          this.query = Query.of(
              QueryKey.of("chat", "members"),
              () -> {
                  String channelId = activeChannelId.get();
                  if (channelId == null || channelId.isEmpty()) {
                      return CompletableFuture.completedFuture(Collections.emptyList());
                  }
                  return MindustryTool.getChatUsers(channelId);
              }
          ).staleTime(Duration.ofSeconds(30));
      }

      public Query<List<ChatUser>> query() {
          return query;
      }
  }
  ```
- **Rationale**: `Query` tracks any reactive dependency read inside its fetcher. When `activeChannelId.get()` changes, the query automatically re-evaluates and fetches.
- **Alternatives considered**:
  - *Keep HashMap and call `loadUsers` on uncollapse*: Leaves the imperative `.subscribe()` architecture intact and prone to missed lifecycle triggers.

### Decision 2: Declarative `QueryView` in `ChatUserListView`
- **Choice**: Replace manual `dynamic()` cascades in `ChatUserListView` with:
  ```java
  query(store.members().query())
      .loading(Loader::centered)
      .error(err -> renderError(err))
      .data(users -> renderUserList(users));
  ```
- **Rationale**: When `ChatUserListView` mounts (whether on initial overlay expand or switching to the Members tab on mobile), `QueryView.build()` invokes `query.ensureFresh()`. If members data has not yet been loaded or is stale, it fetches automatically.
- **Alternatives considered**:
  - *Manual lifecycle hooks (`shown(...)`)*: Adds imperative UI code and breaks declarative Solim component model.

### Decision 3: `ConfigValue` as Single Source of Truth in `ChatUiState`
- **Choice**: `ChatUiState` accepts or binds directly to `ConfigValue<Boolean>` signals for `channelsCollapsed` and `usersCollapsed`.
- **Rationale**: Eliminates the 4 duplicate `.subscribe()` callbacks in `ChatFeature` that mirrored values between `ConfigValue` and `ChatUiState`.
- **Alternatives considered**:
  - *Keep separate signals*: Perpetuates desynchronization and potential infinite update loops.

### Decision 4: Remove Imperative User Fetching from `ChatService`
- **Choice**: Remove `activeId.subscribe(...)` and `loadUsers(channelId)` from `ChatService`. Expose `refresh()` which calls `query.refetch()` on the members query.
- **Rationale**: Centralizes data fetching responsibility in `Query` and removes race conditions between service subscriptions and UI mounting.

## Risks / Trade-offs

- **[Risk]** Channel switching in rapid succession could trigger multiple API queries.
  - **Mitigation**: `Query` handles concurrency with `fetchGeneration` sequence tracking and joins duplicate requests via `QueryCache`.
- **[Risk]** Unit tests in `ChatChannelsAndMembersTest` may rely on removed imperative methods (`setLoading`, `replace`).
  - **Mitigation**: Refactor tests to verify `Query` properties, cached data, and error state transitions.
