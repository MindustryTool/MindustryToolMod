## MODIFIED Requirements

### Requirement: Chat Member Roster Loading and Error Handling
The system SHALL track per-channel member roster loading, error, and data states using a declarative query in `ChatMembers`, displaying a centered loading spinner during member fetch, rendering a retryable error state if the fetch fails with an empty roster, and automatically ensuring fresh data on mount and active channel changes without requiring imperative subscription hooks.

#### Scenario: Member roster loading on mount or channel switch
- **WHEN** ChatUserListView mounts or the active channel changes and members data is missing or stale
- **THEN** ChatMembers initiates a query fetch for the active channel and ChatUserListView displays a centered loader

#### Scenario: Member roster loading failure
- **WHEN** fetching members for a channel fails and no members are cached
- **THEN** ChatUserListView renders a centered error state with a localized error message, technical cause, and a Retry button

#### Scenario: Retrying member roster fetch
- **WHEN** the user clicks the Retry button in ChatUserListView
- **THEN** the members query refetches data for the active channel, setting loading to true
