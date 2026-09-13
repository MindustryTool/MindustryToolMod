# chat-message-group-layout Specification

## Purpose
Groups consecutive chat messages from the same author into visual message groups sharing a single 48px avatar, with stacked message rows and distinct inter-group spacing in the virtualized chat list.

## Requirements
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

