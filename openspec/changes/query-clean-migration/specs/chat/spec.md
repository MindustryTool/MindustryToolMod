## ADDED Requirements

### Requirement: Single Source of Truth for Channels
`ChatChannels` SHALL use `channelsQuery` as the single source of truth for channel data and SHALL NOT maintain a duplicate `channels` signal or bridge effects.
- `all()` SHALL return a readable derived directly from `channelsQuery.data()`.
- When channel data arrives, `ChatChannels` SHALL automatically set `activeChannelId` to the first channel if the current active channel ID is null or not found in the loaded channel list.
- `ChatChannelListView` SHALL render channel rows directly from the data supplied by `QueryView` rather than an empty unpopulated signal.

#### Scenario: Auto-selecting first channel on load
- **WHEN** channel data finishes loading and `activeChannelId` is null
- **THEN** `ChatChannels` SHALL automatically select the first channel in the list

#### Scenario: Rendering channels directly from query data
- **WHEN** `ChatChannelListView` mounts and `channelsQuery` resolves with channels
- **THEN** it SHALL render `ChannelItem` components for all items in the query result without requiring a manual refresh
