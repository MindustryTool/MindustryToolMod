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
