# chat-overlay Specification

## Purpose
Provides a declarative Solim HUD overlay for in-game chat with multi-pane desktop layout, tabbed mobile view, and collapsed draggable badge mode.
## Requirements
### Requirement: Declarative Solim HUD Overlay
The system SHALL provide a ChatOverlayHudView implemented exclusively using declarative Solim components (solim.ui.Ui.*), with zero direct Arc scene widgets (Table, Label, Button, Cell, Stack).

#### Scenario: Overlay rendering
- **WHEN** the chat overlay builds its view hierarchy
- **THEN** it produces a root Hud component with reactive bindings for opacity, position, scale, and visibility

#### Scenario: Visibility synchronization
- **WHEN** the game HUD is hidden or not visible
- **THEN** the chat overlay automatically hides without tearing down reactive bindings

### Requirement: Collapsed Badge Mode
The system SHALL provide a collapsed floating badge display when collapsedConfig is true, showing a single draggable pill button that also expands the chat on click, plus a floating (zero-layout-space) connection status indicator.

#### Scenario: Single button drags and expands
- **WHEN** the user drags the collapsed badge
- **THEN** the entire badge moves with the pointer via the draggable binding without opening the chat

#### Scenario: Expanding from collapsed badge
- **WHEN** the user clicks (without dragging) the collapsed badge
- **THEN** collapsedConfig is set to false and the expanded chat window is displayed

#### Scenario: Connection status dot is floating
- **WHEN** the collapsed badge is rendered
- **THEN** the connection-status indicator is positioned as a floating overlay on the chat icon and contributes zero width and zero height to the row layout

#### Scenario: No separate drag-handle icon
- **WHEN** the collapsed badge is displayed
- **THEN** only one icon (chat icon) is visible and no separate move/drag handle icon is rendered

#### Scenario: Unread badge indicator display
- **WHEN** any channel has persistent unread messages while the chat is collapsed
- **THEN** a scarlet unread circle dot is displayed on the top-right corner of the collapsed badge

#### Scenario: Unread badge count update
- **WHEN** unread messages arrive while collapsed
- **THEN** the badge label updates reactively to reflect the current unread count

### Requirement: Expanded Chat Window
The system SHALL provide an expanded chat view consisting of a header action bar, channel navigation, message feed, user roster, and composer input area. The action bar header SHALL render with a white background (`Tex.whiteui`) and SHALL be draggable across its entire area (including title text, status indicator, and spacer background) while keeping settings and collapse buttons fully clickable. The message feed SHALL wrap all message text cleanly within the message card width without horizontal overflow. The expanded window SHALL use a dark three-pane visual style: the selected channel row is highlighted, message rows show avatar with username + timestamp headers, member rows show presence dots, and the composer renders as a rounded input bar. The expanded window SHALL stay within the visible viewport: its width SHALL NOT exceed 95% of viewport width and its height SHALL NOT exceed 95% of viewport height, its preferred size SHALL reactively track viewport changes including phone rotation, and its minimum sizes SHALL never force overflow on small viewports.

#### Scenario: Collapsing the chat window
- **WHEN** the user clicks the collapse button or presses the Escape key
- **THEN** the overlay transitions to the collapsed badge mode

#### Scenario: Sending a chat message
- **WHEN** the user enters message text in ChatInputView and clicks send or presses Enter
- **THEN** MindustryTool.sendChatMessage() is executed, the input is cleared, and the message appears in the feed

#### Scenario: Dragging from any non-button area of the action bar
- **WHEN** the user touches down and drags on the action bar background, title text, or spacer
- **THEN** the entire chat window moves with the drag gesture and updates the position signals

#### Scenario: Clicking action buttons on the white action bar
- **WHEN** the user clicks the settings button or collapse button on the white action bar
- **THEN** the respective button action fires (opens settings or collapses chat) and no drag is initiated

#### Scenario: Long text messages wrap without horizontal overflow
- **WHEN** a message with long unbroken text or lengthy paragraphs is rendered in the message feed
- **THEN** the text wraps cleanly within the bounds of the message list and does not expand the card or scroll pane horizontally

#### Scenario: Selected channel is visually highlighted
- **WHEN** a channel is the active channel
- **THEN** its row renders with a highlighted background distinct from unselected rows

#### Scenario: Message rows show avatar, username and timestamp
- **WHEN** a first-in-group message is rendered
- **THEN** the row shows the author avatar, the role-colored username and the gray timestamp on a single header line with the content below

#### Scenario: Expanded window stays within viewport on rotation
- **WHEN** the device rotates (or the window resizes) while the expanded chat window is open
- **THEN** the card width does not exceed 95% of the new viewport width and the card height does not exceed 95% of the new viewport height, with no part of the card rendered off-screen beyond repositioning

#### Scenario: Preferred size tracks viewport and ratio configs
- **WHEN** the viewport size or the width/height ratio configs change
- **THEN** the preferred window size recomputes as `viewport * clamped ratio`, clamped to 95% of the viewport

#### Scenario: Minimum sizes never force overflow
- **WHEN** the viewport is so small that 95% of the viewport is below the 320x240 minimums
- **THEN** the window shrinks to fit the viewport instead of forcing the minimum size off-screen

### Requirement: Responsive Mobile and Desktop Layout
The system SHALL provide a multi-pane layout on desktop screens and a tabbed navigation interface (Channels, Messages, Members) on mobile devices (Vars.mobile).

#### Scenario: Switching tabs on mobile
- **WHEN** a user on a mobile device clicks a tab in the mobile tab bar
- **THEN** the corresponding view panel is displayed using the Solim Tabs component

### Requirement: Top-Left Alignment of Message List Items
The message list and all item contents SHALL align to the top-left rather than being centered.

#### Scenario: Rendering message feed items
- **WHEN** messages are displayed in `ChatMessageListView`
- **THEN** message cards, author headers, text bodies, cards, and action buttons align to the top and left edges of the viewport.

#### Scenario: Avatar vertical alignment
- **WHEN** a multi-line message is rendered in `ChatMessageListView`
- **THEN** the author avatar is aligned to the top-left of the message row and does not center vertically within the row.

### Requirement: Enforced Avatar Dimensions
User avatars in the message list and member list SHALL have fixed dimensions regardless of downloaded image resolution and SHALL render with continuous-curvature (L4 superellipse) rounded squircle corners. When no avatar image is available, a fallback badge with matching rounded corners SHALL show the user's first letter with a deterministic per-user color.

#### Scenario: Displaying avatars with network images
- **WHEN** user avatars are rendered in `ChatMessageListView` or `ChatUserListView`
- **THEN** the avatar widget maintains a fixed size (unit(8) in message list, unit(6) in user list) preventing layout shifting or resizing, and renders with anti-aliased continuous-curvature rounded corners.

#### Scenario: Avatar initial fallback
- **WHEN** a user has no avatar image URL or the image fails to load
- **THEN** the avatar slot renders the user's uppercase first letter on a deterministic per-user background color with continuous-curvature rounded corners at the same fixed size

### Requirement: Scroll Position Initialization and Preservation
The chat message list SHALL initialize scroll position at the bottom and preserve relative scroll position instantly when messages update, without slow animation drift. Additionally, the message list SHALL display an end-of-history banner when the channel has reached the beginning of its messages.

#### Scenario: Opening message list or switching channel
- **WHEN** a user opens the chat overlay or switches to a different channel
- **THEN** the scroll view is scrolled to the bottom instantly displaying the most recent messages.

#### Scenario: Older messages prepended
- **WHEN** older messages are loaded into the message feed
- **THEN** the scroll position adjusts instantly by the height of newly prepended content via forced scroll and visual scroll synchronization so the user's view remains anchored to their previous position without drift.

#### Scenario: New message received while at bottom
- **WHEN** a new incoming message is appended to the message feed and the user was scrolled near the bottom
- **THEN** the view instantly scrolls down to display the new message.

#### Scenario: Beginning of chat history reached
- **WHEN** the active channel is marked as fully loaded
- **THEN** an end-of-history notice is displayed at the top of the message feed and further older message fetches on reaching top are disabled.

### Requirement: Persistent Chat Input Focus
The chat composer input field SHALL maintain focus while typing and avoid unmounting on keypress.

#### Scenario: User types message text
- **WHEN** the user types characters into the chat text field
- **THEN** the input component remains continuously mounted and does not lose focus between keystrokes.

### Requirement: Reply Section Layout
The chat input composer SHALL render the reply-target row as a dynamic layout element that is visible and expands across the full composer width when a reply target is set, and occupies zero height and no consumed space when not replying.

#### Scenario: Reply row hidden when no reply target
- **WHEN** the store has no reply target (replyTarget is null)
- **THEN** the reply row collapses from layout, occupying zero vertical space in the composer and applying zero padding

#### Scenario: Reply row shown when replying
- **WHEN** the store has a non-null reply target
- **THEN** the reply row becomes visible, expands horizontally across the composer width (`growX`), and displays the target author name with a cancel button without overlapping the chat input card

#### Scenario: Cancelling a reply
- **WHEN** the user clicks the cancel button in the reply row
- **THEN** store.setReplyTarget(null) is called and the reply row collapses from layout

### Requirement: Channel Unread Indicator
Channel rows SHALL indicate unread activity so users can spot new messages without opening each channel.

#### Scenario: Unread channel shows indicator
- **WHEN** a non-active channel has an unread count greater than zero
- **THEN** its row displays an unread indicator dot alongside the channel name

#### Scenario: Active channel clears indicator
- **WHEN** a channel becomes the active channel
- **THEN** its unread indicator is cleared

### Requirement: Channel Unread Circle Indicator
The system SHALL display an unread indicator circle on each channel row in the channel list when that channel has unread messages, derived reactively from persistent read tracking.

#### Scenario: Channel with unread messages
- **WHEN** a channel has a latest message ID newer than the local last-read ID
- **THEN** a white circle indicator is visible next to the channel name in ChatChannelListView

#### Scenario: Channel read
- **WHEN** the user switches to or views the channel
- **THEN** the unread indicator circle for that channel becomes hidden

### Requirement: Member Presence and Online Count
The member sidebar SHALL show who is online via presence dots and an online/total header count, derived from existing roster signals.

#### Scenario: Online count header
- **WHEN** the member list is rendered
- **THEN** a header displays the online member count over the total roster count

#### Scenario: Presence dots on members
- **WHEN** a member row is rendered
- **THEN** it shows a green presence dot for online members and a gray dot for offline members

### Requirement: Mention Highlight
Messages that mention the current user SHALL stand out from regular messages.

#### Scenario: Mentioned message is highlighted
- **WHEN** a rendered message mentions the logged-in user
- **THEN** the message card renders with an accent highlight distinguishing it from regular messages

### Requirement: Rounded Composer Bar
The chat composer SHALL render its text field, attach button, and send button as a single rounded input bar with placeholder text.

#### Scenario: Composer bar layout
- **WHEN** a logged-in user views the composer
- **THEN** the input field, attach affordance, and accent send button appear in one rounded bar showing the message placeholder

#### Scenario: Composer behavior unchanged
- **WHEN** the user sends a message, replies, attaches content, or hits validation limits
- **THEN** the existing send/reply/attach/validation behavior works exactly as before the visual refresh

### Requirement: Mobile Frame Rate Stability and Viewport Windowing
The chat overlay SHALL maintain a minimum target of 55+ FPS on mobile and desktop devices when expanded, by rendering the active message feed in `ChatMessageListView` using a true virtualized list (`solim-virtual-list`) that mounts only messages currently intersecting the visible scroll viewport plus overscan.

#### Scenario: Expanding chat window on mobile
- **WHEN** the chat overlay transitions from collapsed to expanded on a mobile device (`Vars.mobile == true`)
- **THEN** framerate remains stable above 55 FPS and does not drop by half.

#### Scenario: Viewport windowing in message feed
- **WHEN** more than 30 messages are loaded in the active channel
- **THEN** only visible messages plus overscan buffer are actively mounted in the Scene2D hierarchy, preserving scroll offsets and pagination triggers without creating off-screen card widgets.

### Requirement: Early chat message typing and pre-parsing
The chat system SHALL parse incoming and loaded raw `ChatMessage` instances into typed domain models (`TextMessage`, `SchematicMessage`, `ImageMessage`, `RoomInviteMessage`, `MindustryToolLinkMessage`) before rendering, executing URL regexes and schematic base64 decoding once outside the UI build loop.

#### Scenario: Pre-parsing incoming message
- **WHEN** a raw `ChatMessage` is received or fetched
- **THEN** it is immediately parsed into a strongly-typed message model with pre-extracted metadata and cached for rendering

#### Scenario: Schematic base64 decoded once
- **WHEN** a message containing a Mindustry schematic base64 string is received
- **THEN** `Schematics.readBase64()` is executed during the parsing pass and the resulting `Schematic` object is retained in the model, preventing redundant decompression during UI passes

### Requirement: Pre-rendering grouping and height caching
The chat system SHALL group consecutive messages from the same author before rendering and calculate layout heights using static dimensions for fixed components and `GlyphLayout` for wrapped text, caching heights keyed by container width.

#### Scenario: Grouping consecutive author messages
- **WHEN** multiple consecutive messages in the active channel share the same author ID
- **THEN** the first message is marked with header and avatar layout, while subsequent messages are marked with text indentation and zero header height

#### Scenario: Cached height calculation by container width
- **WHEN** message layout heights are requested for a given container width
- **THEN** static components use fixed heights, text is measured once against available width using `GlyphLayout`, and the computed height is cached until container width changes

### Requirement: Message action popup triggered by ellipsis button
The chat message view SHALL display an action ellipsis button (`⋮`) on each message item, and clicking it SHALL open a popup dialog providing `Copy`, `Reply`, and `Translate` actions, without mutating the message item's inline height.

#### Scenario: Opening message action popup
- **WHEN** the user clicks the action ellipsis button on a message card
- **THEN** an action dialog opens displaying `Copy`, `Reply`, and `Translate` options, while the message card height in the list remains unchanged

#### Scenario: Translating via popup
- **WHEN** the user selects `Translate` from the message action dialog
- **THEN** translation is performed and displayed within a popup modal with a copy button, without inserting an expanding card below the message in the virtual list

### Requirement: Chat Layout Thrashing Prevention
The expanded chat overlay SHALL NOT trigger cyclic `invalidateHierarchy()` or size mutation during Scene `validate()` passes.

#### Scenario: Stable HUD root validation
- **WHEN** `ChatOverlayHudView` is rendered and position signals are updated
- **THEN** `HudRootTable.validate()` converges in a single pass without triggering re-entrant layout invalidations or setting position repeatedly.

