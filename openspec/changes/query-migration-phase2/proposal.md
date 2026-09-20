## Why

Phase 1 of the data-fetching migration successfully unified `SchematicBrowser`, `MapBrowser`, `ServerBrowser`, and `ReleaseChecker` onto `Query`, `QueryCache`, and `QueryView`. However, several core mod features—specifically `PlayerConnect`, `Translation`, and `Chat`—still rely on legacy imperative data-fetching patterns, manual `Signal<Boolean>` loading states, unmemoized REST calls, and custom retry logic.

Migrating these remaining features to `Query`, `QueryCache`, and `QueryView` completes the mod-wide transition to declarative, reactive, cached data fetching, eliminating redundant network calls and boilerplate UI handling.

## What Changes

- **PlayerConnect Query Migration**:
  - Replace imperative `roomsSignal` and `isFetching` signal in `PlayerConnectFeature` with a declarative `Query<List<PlayerConnectRoom>>` using `QueryKey.of("playerconnect", "rooms")`.
  - Maintain SSE real-time updates by feeding streamed room list updates into the query's cache/state via `Query.mutate()`.
  - Refactor `RoomBrowserView` to render room cards, loading spinners, and error banners using declarative `query(roomsQuery)` and `QueryView`.
- **Translation Caching via QueryCache**:
  - Migrate `TranslationFeature.translate(text, targetLang)` to deduplicate and cache translations via `QueryCache.getInstance().fetchOrJoin()` with `QueryKey.of("translation", providerId, targetLang, text)`.
  - Update `ChatActionPopup` and in-game chat translation actions to consume the cached translation query/results, avoiding duplicate API calls for identical phrases.
- **Chat Query & View Modernization**:
  - Refactor `ChatChannelListView` from manual nested `dynamic(channels.loading())` / `dynamic(channels.error())` trees to declarative `query(channelsQuery)` using `QueryView`.
  - Modernize `ChatMessageListView` initial loading, error fallback, and retry affordances with `QueryView`.
  - Leverage `QueryCache` for user profile batch lookups (`getUserBatch`) in `ChatService` with `QueryKey.of("user", userId)` to share cached profiles between chat author headers, user roster, and profile dialogs.

## Capabilities

### New Capabilities
- `translation`: Caching and asynchronous fetching for text translations via `QueryCache` and `QueryKey`.

### Modified Capabilities
- `player-connect`: Migrate room directory fetching to `Query<List<PlayerConnectRoom>>` with SWR and declarative `QueryView` in `RoomBrowserView`.
- `chat`: Migrate channel list rendering, message feed error/loading states, and author user batch lookups to `Query`, `QueryView`, and `QueryCache`.

## Impact

- Affected Code:
  - `mindustrytool.features.playerconnect.PlayerConnectFeature`
  - `mindustrytool.features.playerconnect.views.RoomBrowserView`
  - `mindustrytool.features.translation.TranslationFeature`
  - `mindustrytool.features.chat.ChatService`
  - `mindustrytool.features.chat.views.ChatChannelListView`
  - `mindustrytool.features.chat.views.ChatMessageListView`
  - `mindustrytool.features.chat.views.popups.ChatActionPopup`
- Dependencies: Relies on `solim.core.query.*` (`Query`, `QueryCache`, `QueryKey`, `QueryView`, `UI.query()`) established in Phase 1.
- Breaking Changes: None. All public feature contracts, SSE streaming, and user-facing dialogs remain intact.
