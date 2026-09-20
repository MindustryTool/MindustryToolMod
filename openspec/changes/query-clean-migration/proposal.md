## Why

Initial query adoption introduced the `Query` and `QueryView` primitives, but several features (Chat, PlayerConnect, BrowserState) bridged `Query` into duplicate legacy signals using `Effect.of(...)` rather than cleanly adopting `Query` as the single source of truth. This caused state desynchronization bugs—notably in Chat, where `ChatChannelListView` rendered an empty legacy signal despite a successful query fetch, and in PlayerConnect, where startup fetch failures left views empty until a manual refresh was clicked. Furthermore, `QueryView` lacked mount-time freshness checks, so stale or errored startup queries were never automatically re-fetched when a user opened a dialog.

## What Changes

- **Mount-Aware Auto-Fetch**: Add `ensureFresh()`, `isStale()`, and `isError()` to `Query`. Update `QueryView.build()` to invoke `query.ensureFresh()`, automatically fetching missing, stale, or errored data when a view mounts.
- **Chat State Unification**: Remove the duplicate `channels` signal and bridge effects in `ChatChannels`. Make `channelsQuery` the single source of truth. Auto-select the first channel when channel data arrives. Fix `ChatChannelListView` to render query data directly.
- **PlayerConnect State Unification**: Remove the duplicate `rooms` signal and bridge `Effect.of` in `PlayerConnectFeature`. Back `getRooms()` directly from `roomsQuery.data()` and update SSE handlers to mutate `roomsQuery` directly.
- **BrowserState & Browser Dialogs Refactor**: Remove duplicate `items`, `loading`, and `error` signals and bridge effects in `BrowserState`. Expose `Query<List<T>>` directly and refactor `SchematicBrowserDialog` and `MapBrowserDialog` to use declarative `query(state.query())` instead of 3-level nested `dynamic()` trees.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `query-primitive`: Add `ensureFresh()`, `isStale()`, and `isError()` methods to support mount-time refetching.
- `query-view`: Trigger `query.ensureFresh()` on component mount so views auto-fetch stale, missing, or errored data.
- `chat`: Unify channel state around `channelsQuery`, auto-select the first channel on data load, and render channel list directly from query data.
- `player-connect`: Unify room state around `roomsQuery` without duplicate `rooms` signal or bridge effects.
- `browsers`: Expose `Query<List<T>>` directly from `BrowserState` and use declarative `query(...)` in browser dialogs.

## Impact

- `solim-core`: `Query.java`, `QueryView.java`, and associated unit tests.
- `mod`: `ChatChannels.java`, `ChatChannelListView.java`, `ChatMessageListView.java`, `PlayerConnectFeature.java`, `RoomBrowserView.java`, `BrowserState.java`, `SchematicBrowserDialog.java`, `MapBrowserDialog.java`.
- No breaking external API changes; internal state representations are simplified and stabilized.
