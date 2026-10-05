# chat-unread-indicator Specification

## Requirements

### Requirement: Reactive channel unread count
The `ChatUnread` state container SHALL provide an active reactive `Readable<Integer>` representing the unread message count for any requested channel, without retaining disposed computeds across component lifecycles.

#### Scenario: Unread count updates after component recreation
- **WHEN** a channel item component is disposed and recreated, and an incoming message increments the channel unread count
- **THEN** the new channel item receives the incremented unread count update and displays the unread indicator dot

### Requirement: Feed visibility awareness for active channels
The system SHALL only automatically mark an incoming message as read if the chat window is expanded and the active channel's message feed is currently visible on screen.

#### Scenario: Message arrives while viewing channel list on mobile
- **WHEN** the chat window is open on a mobile layout, the current tab is the channel list (`tab == 0`), and a message arrives for the selected channel
- **THEN** the message is not auto-marked as read and the channel's unread count increments so the indicator dot is displayed

#### Scenario: Message arrives while viewing message feed
- **WHEN** the chat window is open, the message feed is visible on screen, and a message arrives for the active channel
- **THEN** the message is automatically marked as read
