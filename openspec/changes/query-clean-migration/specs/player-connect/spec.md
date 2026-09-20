## ADDED Requirements

### Requirement: Unified Room Query State
`PlayerConnectFeature` SHALL use `roomsQuery` as the single source of truth for rooms and SHALL NOT maintain a duplicate `rooms` signal or bridge effects.
- `getRooms()` SHALL expose a readable derived directly from `roomsQuery.data()`.
- Real-time room updates from SSE SHALL mutate `roomsQuery` directly via `roomsQuery.mutate(...)`.

#### Scenario: SSE update mutates roomsQuery
- **WHEN** a new room directory payload arrives via SSE
- **THEN** `PlayerConnectFeature` SHALL update `roomsQuery.mutate(rooms)` without updating a separate legacy signal

#### Scenario: getRooms derives from roomsQuery
- **WHEN** components observe `feature.getRooms()`
- **THEN** they SHALL receive updates directly reflecting `roomsQuery.data()`
