## 1. Translation Migration

- [x] 1.1 Cache translations in `TranslationFeature` using `QueryCache.fetchOrJoin` with `QueryKey.of("translation", providerId, targetLang, cleanText)`
- [x] 1.2 Update `ChatActionPopup` to use cached translation
- [x] 1.3 Add tests verifying translation caching and deduplication

## 2. PlayerConnect Migration

- [x] 2.1 Migrate room fetching in `PlayerConnectFeature` to `Query<List<PlayerConnectRoom>>` with SWR, mutate on SSE updates, and refetch on demand
- [x] 2.2 Refactor `RoomBrowserView` to render room list, loading state, and error handling via declarative `QueryView`
- [x] 2.3 Verify `PlayerConnect` compilation and behavior

## 3. Chat Migration

- [x] 3.1 Refactor `ChatChannelListView` to render channel list via declarative `query(channelsQuery)` and `QueryView`
- [x] 3.2 Refactor `ChatMessageListView` loading and error states to use `QueryView`
- [x] 3.3 Integrate `ChatService` user batch lookups with `QueryCache` (`QueryKey.of("user", userId)`)
- [x] 3.4 Verify `Chat` compilation and behavior

## 4. Verification & Testing

- [x] 4.1 Run full gradle test suite across all modules
- [x] 4.2 Verify compliance with AGENTS.md rules (i18n, Java 8 compatibility, no FQCN, no solim-runtime dependencies)
