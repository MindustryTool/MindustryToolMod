# chat-unread-indicator Specification

## MODIFIED Requirements

### Requirement: Reactive channel unread count
The `ChatUnread` state container SHALL provide an active reactive `Readable<Integer>` representing the unread message count for any requested channel, without retaining disposed computeds across component lifecycles, and SHALL exclude unread messages belonging to muted channels from the total unread count calculation.

#### Scenario: Unread count updates after component recreation
- **WHEN** a channel item component is disposed and recreated, and an incoming message increments the channel unread count
- **THEN** the new channel item receives the incremented unread count update and displays the unread indicator dot if the channel is not muted

#### Scenario: Muted channel suppresses unread dot and total badge
- **WHEN** a channel is muted and receives new incoming messages
- **THEN** no unread indicator dot is displayed on the channel item in the channel list, and the messages do not increment the total unread badge count on the collapsed HUD
