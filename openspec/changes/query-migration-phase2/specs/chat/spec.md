## MODIFIED Requirements

### Requirement: MindustryTool Service Integration
The system SHALL interact with chat REST endpoints and SSE event streams via mindustrytool.services.MindustryTool, leverage `QueryCache` for user profile batch lookups, and marshal state updates to the main thread via Core.app.post().

#### Scenario: Initializing chat data
- **WHEN** ChatService.init() is invoked
- **THEN** channels and initial messages are fetched via MindustryTool.getChatChannels() and MindustryTool.getChatMessages(), and the live SSE stream is connected

#### Scenario: User batch lookup uses QueryCache
- **WHEN** missing author profiles are requested
- **THEN** un-cached user profiles are requested via `MindustryTool.getUserBatch()` and the results are populated into `QueryCache` with `QueryKey.of("user", userId)` to prevent redundant profile requests

## ADDED Requirements

### Requirement: Declarative Query-Driven Chat Views
The system SHALL render chat channels and message loading and error states using declarative `QueryView` (`solim.UI.query(...)`) instead of manual nested dynamic signal checks.

#### Scenario: Channel list rendered via QueryView
- **WHEN** `ChatChannelListView` builds its channel list UI
- **THEN** it binds to the channels query using `solim.UI.query()`, rendering loading spinners, error retry messages, and channel items declaratively
