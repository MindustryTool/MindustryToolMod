## ADDED Requirements

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

## MODIFIED Requirements

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
