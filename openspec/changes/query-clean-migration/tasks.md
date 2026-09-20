## 1. Mount-Aware Query Primitives

- [x] 1.1 Add `isStale()`, `isError()`, and `ensureFresh()` to `Query.java`
- [x] 1.2 Update `QueryView.java` to invoke `query.ensureFresh()` on mount in `build()`
- [x] 1.3 Add unit tests in `solim-core` verifying `ensureFresh()` and mount refetching behavior in `QueryTest.java` and `QueryViewTest.java`

## 2. Chat Feature Clean Migration

- [x] 2.1 Remove duplicate `channels` signal from `ChatChannels.java` and make `channelsQuery` the single source of truth
- [x] 2.2 Implement auto-selection of the first channel when channels load in `ChatChannels.java`
- [x] 2.3 Update `ChatChannelListView.java` to iterate directly over query data instead of empty legacy signals
- [x] 2.4 Verify `ChatMessageListView.java` properly displays messages when the first channel is auto-selected

## 3. PlayerConnect Clean Migration

- [x] 3.1 Remove duplicate `rooms` signal and `Effect.of` bridge from `PlayerConnectFeature.java`
- [x] 3.2 Update `getRooms()` and SSE room sync to operate directly on `roomsQuery`
- [x] 3.3 Verify `RoomBrowserView.java` renders rooms without requiring manual refresh

## 4. Browser State & Dialogs Refactor

- [x] 4.1 Remove duplicate `items`, `loading`, and `error` signals and bridge effects from `BrowserState.java`
- [x] 4.2 Expose `Query<List<T>>` directly from `BrowserState.java`
- [x] 4.3 Refactor `SchematicBrowserDialog.java` to use declarative `query(state.query())`
- [x] 4.4 Refactor `MapBrowserDialog.java` to use declarative `query(state.query())`

## 5. Verification & Testing

- [x] 5.1 Run all Solim core tests (`QueryTest`, `QueryViewTest`)
- [x] 5.2 Run mod test suite (`./gradlew test`) across all modules to verify zero regressions
