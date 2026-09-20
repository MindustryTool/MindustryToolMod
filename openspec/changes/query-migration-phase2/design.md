## Context

In Phase 1 of the query migration, `Query`, `QueryCache`, `QueryKey`, and `QueryView` were introduced to Solim and successfully applied to `SchematicBrowser`, `MapBrowser`, `ServerBrowser`, and `ReleaseChecker`.

Phase 2 targets the remaining asynchronous data-fetching workflows in the mod:
1. `TranslationFeature` & `ChatActionPopup`: Text translation via external APIs (Google Translate, LibreTranslate, DeepL, etc.).
2. `PlayerConnectFeature` & `RoomBrowserView`: Relay multiplayer room discovery via REST and SSE.
3. `ChatService`, `ChatChannelListView`, `ChatMessageListView`: In-game chat channels, messages, and user profile batch fetching.

## Goals / Non-Goals

**Goals:**
- Eliminate redundant network requests for identical translation queries by caching translations in `QueryCache`.
- Replace manual `isFetching` / `roomsSignal` state tracking in `PlayerConnectFeature` with a declarative `Query<List<PlayerConnectRoom>>` while keeping real-time SSE stream mutation support via `query.mutate()`.
- Modernize `ChatChannelListView` and `ChatMessageListView` UI loading and error handling using declarative `QueryView` (`solim.UI.query(...)`).
- Integrate `ChatService` user profile batch fetching with `QueryCache` (`QueryKey.of("user", userId)`) for cross-feature profile deduplication.
- Ensure strict Java 8 runtime compatibility and thread safety (all UI updates on `Core.app.post()`).

**Non-Goals:**
- Rewriting the underlying CLA-J relay networking protocol or SSE stream connection logic.
- Altering the chat virtual list (`solim-virtual-list`) windowing or height calculation engines.
- Changing translation provider interfaces or adding new third-party translation providers.

## Decisions

### 1. Translation Caching with `QueryCache.fetchOrJoin`
- **Decision**: In `TranslationFeature.translate(String text, String targetLang)`, generate a deterministic `QueryKey`:
  `QueryKey.of("translation", provider.getId(), targetLang, cleanText)`
  and call:
  `QueryCache.getInstance().fetchOrJoin(key, () -> provider.translate(cleanText, targetLang))`
- **Rationale**: Chat messages frequently contain common words, greetings, or repeated sentences ("gg", "hi", "help", "gl hf"). Without caching, every translation button click or incoming message auto-translate hits the external provider API, wasting bandwidth and triggering rate limits. `fetchOrJoin` guarantees in-flight deduplication and long-term memory caching.
- **Alternatives Considered**:
  - In-memory `HashMap<String, String>` inside `TranslationFeature`: Lacks thread safety, eviction, shared cache invalidation, and deduplication of concurrent requests for the same string.

### 2. PlayerConnect Room Query with SSE Mutation
- **Decision**: Create a `Query<List<PlayerConnectRoom>>` in `PlayerConnectFeature` using key `QueryKey.of("playerconnect", "rooms")` and fetcher `MindustryTool::getPlayerConnectRooms`.
  - The query provides SWR (Stale-While-Revalidate) with a 15-second stale time.
  - When SSE broadcasts a room update, call `roomsQuery.mutate(newRooms)` (or `QueryCache.getInstance().put(...)`) to immediately update the query's data signal without triggering a network fetch.
  - Expose `rooms()` returning `Readable<List<PlayerConnectRoom>>` pointing to `roomsQuery.data()`, preserving backward compatibility with existing views.
  - In `RoomBrowserView`, render the room list using `query(feature.getRoomsQuery())` with declarative `.data()`, `.loading()`, and `.error()` handlers.
- **Rationale**: `Query` natively handles loading, error, refresh, and SWR. `mutate()` allows seamless integration with pushing SSE streams.
- **Alternatives Considered**:
  - Keeping separate `roomsSignal` and polling timer: Redundant with `Query` capabilities and duplicates state management.

### 3. Chat Channels & Messages Declarative Views
- **Decision**:
  - In `ChatService` / `ChatStore`, expose `Query<List<ChannelDto>> channelsQuery` backed by `QueryKey.of("chat", "channels")`.
  - In `ChatChannelListView`, replace nested `dynamic(channels.loading(), ...)` and `dynamic(channels.error(), ...)` with `query(store.channels().channelsQuery())`.
  - In `ChatMessageListView`, use `QueryView` or query-backed signals for channel message state.
  - In `ChatService.fetchMissingAuthors()`, check `QueryCache` for existing cached user profiles (`QueryKey.of("user", authorId)`) before requesting batches via `MindustryTool.getUserBatch(missingIds)`. Store newly returned users in `QueryCache`.
- **Rationale**: Replaces fragile manual nested conditional trees in Solim with standardized, declarative `QueryView` components. Enables cross-component caching of user profiles.

## Risks / Trade-offs

- **[Risk]** SSE room updates arriving while REST query is in flight could be overwritten.
  - **Mitigation**: `Query.mutate()` updates data immediately. If a REST query completes later with older data, timestamp or version checking can be used, or SSE updates can be treated as authoritative by invalidating stale query tags.
- **[Risk]** Large translation cache growing unbounded.
  - **Mitigation**: `QueryCache` retains queries; if needed, stale time or cache eviction can be applied, but text strings in Mindustry chat are lightweight (a few KB total for hundreds of translations).
