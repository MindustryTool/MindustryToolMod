# chat Specification

## Purpose

Mechanical merge of 6 specs per change `spec-domain-merge` (stage 1 pilot, concat-then-dedupe). Sources: chat-overlay, chat-feature, chat-input-rounded-border, chat-message-group-layout, chat-settings, optimistic-message-send. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work.
## Requirements

**Source: chat-overlay**

Provides a declarative Solim HUD overlay for in-game chat with multi-pane desktop layout, tabbed mobile view, and collapsed draggable badge mode.

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
The chat system SHALL group consecutive messages from the same author before rendering and calculate layout heights using static dimensions for fixed components (`INVITE_CARD_HEIGHT = 108f`, `SCHEMATIC_CARD_HEIGHT = 212f`, `IMAGE_CARD_HEIGHT = 140f`, `TOOL_LINK_CARD_HEIGHT = 70f`) and `GlyphLayout` for wrapped text, caching heights keyed by container width.

#### Scenario: Grouping consecutive author messages
- **WHEN** multiple consecutive messages in the active channel share the same author ID
- **THEN** the first message is marked with header and avatar layout, while subsequent messages are marked with text indentation and zero header height

#### Scenario: Cached height calculation by container width
- **WHEN** message layout heights are requested for a given container width
- **THEN** static components use fixed heights, text is measured once against available width using `GlyphLayout`, and the computed height is cached until container width changes

#### Scenario: Uniform invite card height calculation
- **WHEN** `ChatMessageHeightCalculator.calculateHeight()` processes a `RoomInviteMessage`
- **THEN** it calculates `INVITE_CARD_HEIGHT` at 108px scaled height regardless of whether live room metadata has arrived

### Requirement: PlayerConnect room invite card rendering and direct join
The chat system SHALL render typed `RoomInviteMessage` entries as a dedicated interactive card with a fixed 108px layout height, resolving room metadata reactively from `PlayerConnectFeature.getRooms()`. When room data is available, the card SHALL display the room title, security status, map name, gamemode, player count, and compatibility status, with a "Join" action and "Copy Link" action. When room data is unavailable or unlisted, the card SHALL display the link string and offline status with a "Try Connect" action and "Copy Link" action.

#### Scenario: Rendering live room data from PlayerConnectFeature
- **WHEN** a `RoomInviteMessage` is displayed and its link matches an active room in `PlayerConnectFeature.getRooms()`
- **THEN** the card renders the room name, lock icon if secured, map name, gamemode, player count, and a primary "Join" button within a 108px card height

#### Scenario: Joining a password-protected room from chat
- **WHEN** the user clicks "Join" on a secured room invite card
- **THEN** a password input dialog is displayed prior to initiating the connection

#### Scenario: Joining a room with mod mismatches from chat
- **WHEN** the user clicks "Join" on a room invite card that has missing or unneeded mods
- **THEN** a `JoinWarningDialog` is displayed showing the mod differences before connecting

#### Scenario: Rendering fallback card for unlisted or offline room
- **WHEN** a `RoomInviteMessage` is displayed and no matching room exists in `PlayerConnectFeature.getRooms()`
- **THEN** the card renders the raw connect link, an offline/unlisted status label, a "Try Connect" button, and a "Copy Link" button, preserving the exact 108px card height

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

**Source: chat-feature**

Provides the lifecycle, persistent configurations, reactive state store, and MindustryTool network integration for the in-game chat feature.

### Requirement: Feature Lifecycle and Registration
The system SHALL provide a ChatFeature extending mindustrytool.features.Feature with metadata (id: "chat", name, description, icon, enabled by default, quick-access enabled), managing lifecycle activation and deactivation cleanly.

#### Scenario: Feature enabled
- **WHEN** ChatFeature.onEnable() is called
- **THEN** the chat overlay HUD is instantiated and added to the scene, and network connections/streams are initialized

#### Scenario: Feature disabled
- **WHEN** ChatFeature.onDisable() is called
- **THEN** the chat overlay HUD is removed and disposed, and network connections/streams are disconnected

### Requirement: Configuration via ConfigValue
The system SHALL manage all persistent chat settings using ConfigGroup and ConfigValue<T> instances, exposing reactive signals for each setting.

#### Scenario: Reading and updating opacity configuration
- **WHEN** opacityConfig value is changed
- **THEN** the underlying setting is persisted and its reactive signal notifies observers with the new value

#### Scenario: Collapsed state persistence
- **WHEN** the chat is collapsed or expanded
- **THEN** collapsedConfig updates and persists the boolean state

### Requirement: Reactive Chat State Management
The system SHALL maintain single-source-of-truth chat state composed within ChatStore via dedicated domain state modules (ChatSession, ChatChannels, ChatMessages, ChatMessageDelivery, ChatMembers, ChatUsers, ChatUnread, ChatTranslations, ChatUiState), with per-channel unread status evaluated against persistent read state and deriving total unread presence reactively.

#### Scenario: New message received in inactive channel
- **WHEN** a message is received for a channel that is not currently active
- **THEN** the message is appended to that channel's message list in ChatMessages and that channel's unread status in ChatUnread is updated to unread

#### Scenario: Selecting an active channel
- **WHEN** a channel is selected as active via ChatStore.selectChannel()
- **THEN** active channel signal in ChatChannels updates, that channel's latest known message ID is persisted to Core.settings as read, unread status for that channel in ChatUnread is cleared, and active selection UI state in ChatUiState is reset

#### Scenario: Channel history fully loaded
- **WHEN** older messages are requested for a channel and the returned list is empty or smaller than the requested page size
- **THEN** the channel is marked as fully loaded in ChatMessages, preventing further fetch requests

#### Scenario: Pending message tracking
- **WHEN** a temporary message is created for optimistic display
- **THEN** its ID is marked as PENDING in ChatMessageDelivery

#### Scenario: Pending message cleared on confirmation
- **WHEN** a pending message is confirmed by server response
- **THEN** its status is cleared from ChatMessageDelivery and replaced or reconciled in ChatMessages

#### Scenario: Failed message tracking
- **WHEN** a send request fails for a pending message
- **THEN** its status transitions to FAILED in ChatMessageDelivery

#### Scenario: Failed message cleared on retry
- **WHEN** a failed message is retried
- **THEN** its status transitions from FAILED back to PENDING in ChatMessageDelivery

### Requirement: Persistent Last-Read Message Tracking via UUIDv7
The system SHALL persist the last-read message ID per channel in `Core.settings` (`mindustrytool.chat.lastread.<channelId>`), and determine channel unread status by comparing the latest known message ID (from `ChannelDto.lastMessageId` or incoming stream messages) against the stored ID using lexicographical UUIDv7 comparison.

#### Scenario: Unread evaluated from channel metadata without messages loaded
- **WHEN** channel list is fetched and a channel has a lastMessageId newer than the stored last-read ID in Core.settings (or no stored ID exists)
- **THEN** that channel is marked as unread in ChatUnread even if message history has not been fetched

#### Scenario: Marking channel as read on selection or active view
- **WHEN** the user selects a channel or has the channel active while the chat window is open
- **THEN** the channel's latest known message ID is persisted in Core.settings and unread status for that channel becomes false

#### Scenario: Real-time message arrival in active vs inactive channel
- **WHEN** a new message arrives via stream
- **THEN** if the chat window is open and the message belongs to the active channel, the stored last-read ID is immediately updated to that message's ID; if the window is closed or the message is for an inactive channel, the channel is marked as unread

### Requirement: MindustryTool Service Integration
The system SHALL interact with chat REST endpoints and SSE event streams via mindustrytool.services.MindustryTool and marshal state updates to the main thread via Core.app.post().

#### Scenario: Initializing chat data
- **WHEN** ChatService.init() is invoked
- **THEN** channels and initial messages are fetched via MindustryTool.getChatChannels() and MindustryTool.getChatMessages(), and the live SSE stream is connected

### Requirement: Composer send gating on validity
The chat composer send button SHALL be enabled only while no send is in flight and the message input is valid per its configured validator. The imperative validity guard in the send handler SHALL be retained as defense in depth.

#### Scenario: Send disabled on invalid input
- **WHEN** the composer input is empty or fails validation while no send is in flight
- **THEN** the send button is disabled

#### Scenario: Send disabled while sending
- **WHEN** a send request is in flight
- **THEN** the send button is disabled regardless of input validity

#### Scenario: Send enabled on valid idle input
- **WHEN** no send is in flight and the input passes validation
- **THEN** the send button is enabled

**Source: chat-input-rounded-border**

Styles the chat composer message input with subtle rounded corners and a dark gray outline, including while focused, so it reads as a distinct modern input.

### Requirement: Rounded bordered chat input field
The chat composer message input SHALL render with 12px rounded corners and a 1.5px dark gray border.

#### Scenario: Composer input shows rounded bordered styling
- **WHEN** the chat composer is displayed while logged in
- **THEN** the message text field has 12px rounded corners with a 1.5px dark gray outline

#### Scenario: Rounding persists while typing
- **WHEN** the message text field gains focus
- **THEN** the field keeps the same 12px rounded corners and border instead of reverting to a square background

**Source: chat-message-group-layout**

Groups consecutive chat messages from the same author into visual message groups sharing a single 48px avatar, with stacked message rows and distinct inter-group spacing in the virtualized chat list.

### Requirement: Consecutive message grouping into MessageGroup
The system SHALL group consecutive chat messages from the same author into a composite `MessageGroup` object containing the author ID, the creation time of the first message, and the list of ordered messages.

#### Scenario: Consecutive messages from the same author are grouped
- **WHEN** multiple messages are received consecutively from author A
- **THEN** they are aggregated into a single `MessageGroup` containing author A's messages in order

#### Scenario: Message from different author starts a new group
- **WHEN** a message is received from author B after messages from author A
- **THEN** a new `MessageGroup` is started for author B

### Requirement: Group-level avatar and stacked message flow
The system SHALL render a fixed 48px avatar (`unit(12)`) at the top-left of each `MessageGroup` and stack all messages from that author in a right-hand vertical column with a uniform 3px gap (`unit(0.75f)`), allowing subsequent messages to flow naturally beside the avatar without internal voids.

#### Scenario: Multi-message group flows beside the avatar
- **WHEN** a group contains multiple messages from the same author
- **THEN** the avatar sits on the left while the messages stack tightly in the right column, with the second message appearing directly below the first

#### Scenario: Single-line first message does not create a dead void
- **WHEN** the first message in a group has a short single-line text and a second message follows
- **THEN** the second message is positioned immediately below the first message with a 3px gap beside the 48px avatar

### Requirement: Inter-group spacing in virtualized list
The system SHALL separate adjacent `MessageGroup` components in the virtual list with a distinct visual gap of 3px (`unit(0.75f)`).

#### Scenario: Adjacent groups separated by virtual list gap
- **WHEN** messages from different authors are rendered in the virtual list
- **THEN** a 3px gap separates the bottom of one author's group and the top of the next author's group

### Requirement: MessageGroup layout height calculation
The layout height calculator SHALL compute the height of a MessageGroup with 100% precision matching the rendered element tree height. The calculation takes the maximum of the avatar height (AVATAR_SIZE = 48px) and the right column height (header height HEADER_HEIGHT = 24px + header gap HEADER_GAP = 2px + sum of message heights + internal message gaps MESSAGE_GAP = 3px) plus vertical padding (UNIT_1 * 2 = 8px). For text messages, the calculation SHALL accurately measure wrapped text matching Arc's Label preferred height calculations, reply preview heights (REPLY_PREVIEW_HEIGHT = 24px), and vertical message card padding (MESSAGE_CARD_PADDING = 8px).

#### Scenario: Group height reflects accumulated message contents
- **WHEN** the height of a MessageGroup is calculated for a given container width
- **THEN** the result accounts for the header, all contained messages, and internal gaps, with a minimum height bound by the 48px avatar

#### Scenario: 100% height parity with rendered element tree
- **WHEN** a MessageGroupView element is created, added to the UI tree, and laid out at container width W
- **THEN** its measured layout height (getPrefHeight()) matches ChatMessageHeightCalculator.calculateHeight(group, W) across all text message conditions

### Requirement: UI synchronization with height calculator constants
The MessageGroupView component SHALL explicitly configure its layout elements using the dimensional constants defined in ChatMessageHeightCalculator, including header height, message card padding, reply preview height, and container padding, ensuring that the visual component tree produces exact matching dimensions.

#### Scenario: Header row dimensions enforced
- **WHEN** a message group is rendered
- **THEN** the author and action header row has its height explicitly set to ChatMessageHeightCalculator.HEADER_HEIGHT

#### Scenario: Message item card padding and reply preview height enforced
- **WHEN** an individual message item or reply preview is rendered
- **THEN** the item card applies ChatMessageHeightCalculator.MESSAGE_CARD_PADDING and the reply preview row applies ChatMessageHeightCalculator.REPLY_PREVIEW_HEIGHT

### Requirement: Comprehensive text message height test coverage
The test suite SHALL include an automated test that creates the real MessageGroupView Solim component, attaches it to an Arc UI layout hierarchy, forces layout/validation at a specified container width, and verifies that the measured height equals the calculated height for all text message conditions.

#### Scenario: Single-line short text message condition
- **WHEN** a text message group contains a single short text message that does not exceed avatar height
- **THEN** the measured layout height in the element tree matches ChatMessageHeightCalculator.calculateHeight exactly, bound by avatar height

#### Scenario: Multi-line wrapped text message condition
- **WHEN** a text message group contains long text that wraps across multiple lines
- **THEN** the measured layout height in the element tree matches ChatMessageHeightCalculator.calculateHeight exactly, accounting for wrapped text lines

#### Scenario: Empty or whitespace-only text message condition
- **WHEN** a text message group contains an empty or whitespace-only message
- **THEN** the measured layout height in the element tree matches ChatMessageHeightCalculator.calculateHeight exactly

#### Scenario: Mentioned text message condition
- **WHEN** a text message group contains a message that mentions the current user
- **THEN** the measured layout height in the element tree matches ChatMessageHeightCalculator.calculateHeight exactly

#### Scenario: Text message with reply preview condition
- **WHEN** a text message group contains a message with a reply-to reference
- **THEN** the measured layout height in the element tree matches ChatMessageHeightCalculator.calculateHeight exactly, accounting for reply preview height

#### Scenario: Text message with reply preview and multi-line text condition
- **WHEN** a text message group contains a message with both a reply preview and multi-line wrapped text
- **THEN** the measured layout height in the element tree matches ChatMessageHeightCalculator.calculateHeight exactly

#### Scenario: Consecutive multi-message group condition
- **WHEN** a text message group contains multiple consecutive text messages from the same author
- **THEN** the measured layout height in the element tree matches ChatMessageHeightCalculator.calculateHeight exactly, accounting for multiple message cards and inter-message gaps

### Requirement: Individual message actions preserved in group view
The system SHALL preserve click handling on individual message elements within a `MessageGroup` to display a shared floating action popup menu (providing Copy, Reply, and Translate actions) anchored near the clicked message, without dimming the screen with a modal dialog. Selecting Translate SHALL trigger translation and update the message text in-place within the message bubble.

#### Scenario: Clicking a specific message opens shared floating action popup
- **WHEN** the user clicks on any message row inside a message group
- **THEN** a shared floating action popup menu opens anchored near that specific message containing Copy, Reply, and Translate actions

#### Scenario: Touching outside dismisses action popup
- **WHEN** the user clicks anywhere outside the open floating action popup
- **THEN** the action popup is dismissed

#### Scenario: Translating message in-place
- **WHEN** the user selects the Translate action from the popup
- **THEN** translation is requested and the translated text is displayed in-place within the message item upon completion

### Requirement: Interactive URL Link Detection and Confirmation Dialog
The chat message view SHALL detect valid HTTP/HTTPS URLs in text messages, highlight them using the design system primary color (`WebStyles.Colors.PRIMARY`), and prompt a confirmation dialog with translated text upon clicking before navigating externally via `Core.app.openURI(url)`.

#### Scenario: Message containing URL is highlighted
- **WHEN** a text message contains one or more HTTP/HTTPS URLs
- **THEN** the URL substrings are highlighted with the primary color and styled distinctly from plain text

#### Scenario: Clicking a link opens confirmation dialog
- **WHEN** the user clicks on a highlighted link in a chat message
- **THEN** a confirmation dialog opens showing the destination URL and asking for confirmation before opening in the browser

#### Scenario: Confirming link open
- **WHEN** the user confirms in the link confirmation dialog
- **THEN** the dialog closes and `Core.app.openURI(url)` is invoked

### Requirement: Schematic Message Card Layout and Height Parity
The schematic message card SHALL position action buttons (`Info`, `Export`, `Edit`, `Use`) in a dedicated row below the schematic preview image, and `ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT` SHALL be configured to 212px matching the rendered component tree height.

#### Scenario: Schematic card action buttons located below preview
- **WHEN** a schematic message card is rendered
- **THEN** the action buttons row is positioned underneath the schematic preview image

#### Scenario: Schematic card height calculation matches 212px
- **WHEN** the layout height of a schematic message item is computed
- **THEN** `ChatMessageHeightCalculator` returns 212px matching the rendered schematic card height

**Source: chat-settings**

Provides a declarative Solim dialog and view for adjusting chat overlay visual preferences.

### Requirement: Solim Settings View and Dialog
The system SHALL provide ChatSettingsDialog and ChatSettingsView built exclusively with declarative Solim components (solim.ui.Ui.*) to configure chat preferences.

#### Scenario: Opening settings dialog
- **WHEN** ChatFeature.getSettingDialog() is requested
- **THEN** a Solim dialog hosting ChatSettingsView is returned

### Requirement: Interactive Settings Controls
The system SHALL provide reactive sliders for adjusting chat opacity (0.2 - 1.0), scale (0.5 - 1.5), width (0.4 - 1.0), and height (0.4 - 1.0), directly bound to ConfigValue.signal().

#### Scenario: Adjusting opacity slider
- **WHEN** the user slides the opacity control in ChatSettingsView
- **THEN** the opacity configuration updates immediately and updates the chat overlay HUD

### Requirement: Reset Chat Preferences
The system SHALL provide a reset action to restore chat window position, dimensions, opacity, and scale back to default values.

#### Scenario: Triggering reset position and settings
- **WHEN** the user clicks the reset button in ChatSettingsView
- **THEN** default position and dimensions are applied to the configurations and reflected in the overlay

**Source: optimistic-message-send**

Provides optimistic message display with pending/failed states, temp-to-real message reconciliation, and retry on failure for the in-game chat feature.

### Requirement: Optimistic message display
The system SHALL display a temporary message in the chat list immediately when the user sends a message, before the server confirms delivery.

#### Scenario: Message appears instantly
- **WHEN** the user sends a message and the send request is in flight
- **THEN** a temporary message with the message content is appended to the active channel's message list at 50% opacity

#### Scenario: Temporary message has client-generated ID
- **WHEN** a temporary message is created for optimistic display
- **THEN** its ID SHALL be prefixed with `temp_` followed by a UUID, guaranteeing no collision with server-assigned IDs

### Requirement: Message confirmation on server response
The system SHALL replace the temporary message with the server-confirmed message when the HTTP response arrives.

#### Scenario: Successful send replaces temp message
- **WHEN** the server responds with a confirmed message and the temp message is still in the list
- **THEN** the temp message is replaced in-place with the real message and the opacity transitions to 100%

#### Scenario: SSE race — real message already delivered
- **WHEN** the server responds and the real message is already in the list (delivered via SSE)
- **THEN** the temp message is removed from the list without adding a duplicate

#### Scenario: Temp already removed (channel switch)
- **WHEN** the server responds but the temp message is no longer in the list (user switched channels)
- **THEN** the system performs no list modification

### Requirement: Send failure handling
The system SHALL keep the failed message visible with an error indication when the send request fails.

#### Scenario: Failed message stays visible
- **WHEN** the send request fails (network error, server error, rate limit)
- **THEN** the temp message remains in the list with 50% opacity and a red color tint

#### Scenario: Retry button on failed message
- **WHEN** a message has failed to send
- **THEN** a retry button is displayed on or near the failed message

#### Scenario: Retry transitions to pending
- **WHEN** the user taps the retry button on a failed message
- **THEN** the message transitions from failed state back to pending state (50% opacity, no red tint) and the send is re-attempted with the same content

#### Scenario: Retry success
- **WHEN** a retried send request succeeds
- **THEN** the message is confirmed (100% opacity) following the same replacement logic as a first-time send

### Requirement: One pending message at a time
The system SHALL prevent concurrent optimistic sends by disabling further sends while a message is pending or being retried.

#### Scenario: Send disabled while pending
- **WHEN** a message is currently in pending or failed state
- **THEN** the send button and enter-to-send are disabled until the current send completes or fails

### Requirement: Collapsible Desktop Chat Sidebars
The system SHALL support independently collapsing and expanding the channel list sidebar and user list sidebar in desktop mode. When either sidebar is collapsed, its column and adjacent divider SHALL be omitted from the layout, and the message view SHALL expand to fill the reclaimed horizontal space.

#### Scenario: Collapsing channel list sidebar
- **WHEN** the user clicks the channel toggle button while the channel list is expanded
- **THEN** the channel list sidebar and its divider are hidden, the message area expands to the left edge of the desktop body, and the toggle button icon updates to indicate expandability

#### Scenario: Expanding channel list sidebar
- **WHEN** the user clicks the channel toggle button while the channel list is collapsed
- **THEN** the channel list sidebar and its divider are restored to their standard width (`unit(80)`), and the toggle button icon updates to indicate collapsible state

#### Scenario: Collapsing user list sidebar
- **WHEN** the user clicks the user list toggle button while the user list is expanded
- **THEN** the user list sidebar and its divider are hidden, the message area expands to the right edge of the desktop body, and the user toggle button reflects the collapsed state

#### Scenario: Expanding user list sidebar
- **WHEN** the user clicks the user list toggle button while the user list is collapsed
- **THEN** the user list sidebar and its divider are restored to their standard width (`unit(80)`), and the user toggle button reflects the expanded state

### Requirement: Chat Message List Header Bar
The system SHALL provide a header bar at the top of `ChatMessageListView` in desktop mode containing a channel list toggle button on the left, the active channel name in the center/left, and a user list toggle button on the right.

#### Scenario: Displaying active channel name
- **WHEN** a channel is active
- **THEN** the header bar reactively displays `# <channel-name>` without blocking or snapshotting signals during build

#### Scenario: Toggle button icon states
- **WHEN** the channel list is expanded
- **THEN** the channel toggle button renders with a left-facing collapse indicator and tooltip for hiding channels
- **WHEN** the channel list is collapsed
- **THEN** the channel toggle button renders with a right-facing expand indicator and tooltip for showing channels

#### Scenario: Responsiveness on mobile screens
- **WHEN** the viewport width is below the desktop threshold (< 1200 units)
- **THEN** the sidebar collapse toggle buttons are hidden, and the view adapts to mobile tabs

### Requirement: Persistent Chat Sidebar Collapse Configurations
The system SHALL persist the collapsed states of the channel list and user list in `ChatFeature` configurations so that user preferences are preserved across application restarts.

#### Scenario: Persisting sidebar collapse state across restarts
- **WHEN** a user toggles the collapsed state of either sidebar
- **THEN** the new state is saved to the mod configuration and loaded on subsequent application launches

### Requirement: Image message fullscreen preview
The chat message view SHALL render each `ImageMessage` thumbnail as tappable, and tapping it SHALL open a near-fullscreen image preview dialog showing the image with a back/close button and back-navigation support, without triggering the message action popup. Tapping the message body outside the thumbnail SHALL continue to open the message action popup as before. The preview dialog SHALL dispose itself when hidden.

#### Scenario: Tapping an image thumbnail
- **WHEN** the user taps an `ImageMessage` thumbnail
- **THEN** a near-fullscreen preview dialog opens showing the image with a back button, and the message action popup does not open

#### Scenario: Closing the preview
- **WHEN** the user activates the back button or back navigation
- **THEN** the preview dialog closes and is disposed

#### Scenario: Tapping outside the thumbnail
- **WHEN** the user taps the message body outside the thumbnail
- **THEN** the existing message action popup opens as before

### Requirement: Channel List Loading and Error Handling
The system SHALL track loading and error state when fetching chat channels and display a loading spinner, an error message with technical exception details, and a Retry button when channel loading fails.

#### Scenario: Channel loading indicator
- **WHEN** the chat system initiates fetching chat channels
- **THEN** ChatChannels sets loading to true and ChatChannelListView displays a centered animated loader

#### Scenario: Channel loading failure and error view
- **WHEN** fetching chat channels fails due to a network or server error
- **THEN** ChatChannels sets loading to false and records the error message, and ChatChannelListView displays a centered error state showing the localized error message, technical cause, and a Retry button

#### Scenario: Retrying channel fetch
- **WHEN** the user clicks the Retry button on the channel error view
- **THEN** ChatService initiates a new channel fetch, sets loading to true, and clears previous errors

### Requirement: Initial Chat Messages Loading and Error Handling
The system SHALL track per-channel initial message loading and error states, displaying a loading spinner during initial message fetch, a full-screen error with a Retry button if the initial fetch fails, and a non-intrusive warning with retry if a subsequent refresh fails while messages are already present.

#### Scenario: Initial message loading indicator
- **WHEN** an active channel is selected and messages have not yet loaded
- **THEN** ChatMessages marks initial loading as true for that channel and ChatMessageListView displays a centered loader

#### Scenario: Initial message loading failure
- **WHEN** the initial message request for a channel fails and no messages are currently cached for that channel
- **THEN** ChatMessageListView renders a centered error state with a localized error message, technical cause, and a Retry button

#### Scenario: Retrying initial message fetch
- **WHEN** the user clicks the Retry button in ChatMessageListView
- **THEN** ChatService triggers loadMessages for the active channel, clears the channel error, and sets loading to true

#### Scenario: Preserving existing messages on subsequent refresh failure
- **WHEN** messages are already present in the active channel and a subsequent manual or background refresh fails
- **THEN** ChatMessageListView preserves the existing visible message list and displays a non-intrusive error banner with a Retry action

### Requirement: Chat Member Roster Loading and Error Handling
The system SHALL track per-channel member roster loading and error states, displaying a loading spinner during member fetch, a full-screen error with a Retry button if the fetch fails with an empty roster, and preserving existing members if a subsequent refresh fails.

#### Scenario: Member roster loading indicator
- **WHEN** an active channel is selected and member fetch begins
- **THEN** ChatMembers marks loading as true for that channel and ChatUserListView displays a centered loader

#### Scenario: Member roster loading failure
- **WHEN** fetching members for a channel fails and no members are cached for that channel
- **THEN** ChatUserListView renders a centered error state with a localized error message, technical cause, and a Retry button

#### Scenario: Retrying member roster fetch
- **WHEN** the user clicks the Retry button in ChatUserListView
- **THEN** ChatService triggers loadUsers for the active channel, clears the channel error, and sets loading to true

### Requirement: Mobile Default Tab Cross-State Error Awareness
The system SHALL display channel error details and a Retry Channels action directly in the Messages and Members views when no channel is active due to channel loading failure.

#### Scenario: Channel failure presented in Messages tab
- **WHEN** the chat overlay is opened on mobile (defaulting to the Messages tab) and channels failed to load on startup
- **THEN** the Messages view displays an error notice stating that channels failed to load along with a Retry Channels button

#### Scenario: Successful retry from Messages tab
- **WHEN** the user clicks the Retry Channels button from the Messages view
- **THEN** channels are re-fetched and upon success the first channel is selected and its messages are loaded automatically

### Requirement: Top-Bar Refresh Scope
The system SHALL allow the top-bar refresh button to recover from missing channel state by refreshing channels if none is active, or refreshing active messages and members if an active channel is present.

#### Scenario: Refresh with no active channel
- **WHEN** the top-bar refresh button is clicked while no active channel is selected
- **THEN** ChatService executes refreshChannels() to attempt channel discovery

#### Scenario: Refresh with active channel
- **WHEN** the top-bar refresh button is clicked while a channel is active
- **THEN** ChatService refreshes both messages and members for the active channel and reconnects the live stream if disconnected

