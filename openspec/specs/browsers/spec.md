# browsers Specification

## Purpose

Mechanical merge of 3 specs per change `spec-domain-merge` (stage 1 pilot, concat-then-dedupe). Sources: browser-common, map-browser, schematic-browser. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work.
## Requirements

**Source: browser-common**

TBD - created by archiving change rewrite-browser-features. Update Purpose after archive.

### Requirement: Reactive Browser State Management
The system SHALL maintain a reactive BrowserState<T> holding search query, selected tags, sort option, current page index, total items, dynamic page size, loading flag, and error message. BrowserState SHALL internally use `Query<T>` for its fetch lifecycle — delegating loading state, error handling, thread marshaling, and stale-while-revalidate behavior to the Query primitive. BrowserState's public API (signal accessors, pagination methods) SHALL remain unchanged.

#### Scenario: Update search query triggers reload
- **WHEN** user modifies the search query text field
- **THEN** the state SHALL debounce the change and the internal Query SHALL auto-refetch due to reactive dependency tracking on the query signal

#### Scenario: Select or unselect tag
- **WHEN** user toggles a tag category filter
- **THEN** the state SHALL reset to page 0 and the internal Query SHALL auto-refetch due to reactive dependency tracking on the selectedTags signal

#### Scenario: Dynamic page size update
- **WHEN** the browser viewport capacity is determined or changes
- **THEN** the state SHALL update its pageSize signal clamped between 20 and 100 and reset the page to 0, and the internal Query SHALL auto-refetch

#### Scenario: Handle API failure
- **WHEN** network request fails or returns non-200
- **THEN** the internal Query SHALL manage error state and retry with backoff, and BrowserState's `error()` signal SHALL reflect the Query's error state

#### Scenario: BrowserState start/stop lifecycle
- **WHEN** `start()` is called on BrowserState
- **THEN** the internal Query SHALL be activated (enabled), and when `stop()` is called, the Query SHALL be deactivated

#### Scenario: Stale data during refetch
- **WHEN** a filter parameter changes while data is already loaded
- **THEN** BrowserState's `items()` SHALL retain the previous data while the internal Query refetches, and `loading()` SHALL remain false while `fetching()` is true

### Requirement: Responsive Card Grid Layout
The system SHALL dynamically compute column count, row count, content width, card sizing, and viewport capacity based on viewport dimensions (`dvw`, `dvh`), budgeting symmetrical scrollbar gutters on both sides of the card grid to ensure the scrollbar never clips cards or obscures action buttons, and SHALL center the content column on the horizontal X-axis matching the grid width.

#### Scenario: Screen orientation change on mobile
- **WHEN** mobile screen orientation changes from portrait to landscape
- **THEN** the grid SHALL recalculate column count from 1-2 columns to 2-3 columns without rebuilding unaffected card elements

#### Scenario: Centered layout on horizontal axis
- **WHEN** the browser dialog is displayed on wide or high-resolution viewports
- **THEN** the content container holding the search header, scrollable grid, and footer SHALL have width equal to `cols * cardWidth + (cols - 1) * gap` and be horizontally centered within the full-screen dialog

#### Scenario: Viewport capacity determines query page size
- **WHEN** the browser dialog is displayed on screen
- **THEN** the system SHALL compute total card capacity from columns multiplied by rows and set query page size clamped between 20 and 100

#### Scenario: Touch-friendly targets on mobile
- **WHEN** rendered on mobile devices
- **THEN** all clickable buttons and card action triggers SHALL have a minimum touch target size of 40 units

#### Scenario: Symmetrical scrollbar gutter budgeting
- **WHEN** column count and content width are calculated for the browser grid
- **THEN** available width SHALL deduct both horizontal padding and symmetrical scrollbar gutters (`SCROLLBAR_GUTTER * 2`), and the rightmost column action buttons SHALL remain unobscured by the scrollbar track and knob

#### Scenario: Safe vertical overhead prevents spurious scrollbar
- **WHEN** available height is calculated for capacity and page sizing
- **THEN** the system SHALL subtract a safe vertical overhead of `unit(50f)` (200px), ensuring single-page item capacity does not exceed viewport height

### Requirement: Image-First Schematic Hero Card
`SchematicCard` SHALL prioritize the schematic preview image as the hero visual element across the top of the card inside a preview card container with fixed square dimensions (`unit(58f)` / 232px, 1:1 aspect ratio). The schematic title SHALL be rendered inside a translucent dark overlay centered along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with four compact interactive buttons using vanilla Mindustry icons with stat counts:
1. Like button with like count (heart icon + count)
2. Comment button with comment count (`Icon.chatSmall` + count)
3. Download/save button with download count (`Icon.downloadSmall` + count)
4. Copy string button (`Icon.copy`)
The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Schematic card visual presentation
- **WHEN** `SchematicCard` is rendered
- **THEN** the preview container occupies the upper area with a fixed square preview (`unit(58f)` / 232px) and title centered along the bottom, followed by a 4-button action row

#### Scenario: Direct action interaction
- **WHEN** user clicks the download button on a card
- **THEN** it executes the save action directly without requiring the detail dialog

#### Scenario: Square aspect ratio preservation
- **WHEN** the schematic card is rendered in the grid or rescaled on small viewports
- **THEN** the preview card container height SHALL match the card width to maintain a 1:1 square aspect ratio

### Requirement: Image-First Map Hero Card
`MapCard` SHALL prioritize the map terrain preview image as the hero visual element across the top of the card inside a preview card container with fixed square dimensions (`unit(58f)` / 232px, 1:1 aspect ratio). The map title SHALL be rendered inside a translucent dark overlay centered along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with compact interactive buttons using vanilla Mindustry icons with stat counts (heart icon + likes opening details, `Icon.chatSmall` + comments opening details, `Icon.downloadSmall` + downloads triggering download, `Icon.play` triggering play). The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Map card visual presentation
- **WHEN** `MapCard` is rendered
- **THEN** the preview container occupies the upper area with a fixed square preview (`unit(58f)` / 232px) and title centered along the bottom, followed by a compact action button row

#### Scenario: Square aspect ratio preservation
- **WHEN** the map card is rendered in the grid or rescaled on small viewports
- **THEN** the preview card container height SHALL match the card width to maintain a 1:1 square aspect ratio

### Requirement: Paged Navigation Controls
The system SHALL provide balanced 3-section paged footer navigation with a custom Close/Back button on the left, Previous, Next, and Direct Page Jump controls centered, and an Upload action on the right, all styled with WebStyles.

#### Scenario: Previous page boundary
- **WHEN** user is on page 1
- **THEN** the Previous button SHALL be disabled with muted WebStyles styling

#### Scenario: Direct page jump
- **WHEN** user clicks on the page indicator
- **THEN** a numeric prompt dialog SHALL allow jumping directly to any valid page number

#### Scenario: Upload shortcut
- **WHEN** user clicks the upload button
- **THEN** the system SHALL open the external upload web portal URL in the default browser

#### Scenario: Custom close action
- **WHEN** user clicks the Close/Back button in the footer
- **THEN** the browser dialog SHALL be closed

### Requirement: Tag & Category Filtering Modal
The system SHALL display an interactive Solim modal dialog showing sort options and categorized tags fetched dynamically from MindustryTool.getTags(). The dialog SHALL reactively filter visible tags when search input or planet selection changes, and wrap tag buttons into responsive flow rows using `Wrap` with per-item width computed from `GlyphLayout` text measurement. All toggle buttons SHALL use `WebStyles.filterChipText()` styling. The "Clear all" button SHALL use `WebStyles.clearFiltersText()` styling. Tag and planet data SHALL be cached in static signals that persist across dialog open/close instances, fetching from the API only on first open.

#### Scenario: Render tag categories
- **WHEN** filter dialog is opened
- **THEN** it SHALL display categories as distinct visual groups with toggleable `WebStyles.filterChipText()` badge buttons wrapped into responsive flow rows using `Wrap`, where each chip width is `GlyphLayout.textWidth(Fonts.def, label) + 8f padding + 4f safety`

#### Scenario: Live search filter update
- **WHEN** user types into the filter dialog search field
- **THEN** the visible tags within each category SHALL update reactively without requiring dialog reopen or severed signal snapshots

#### Scenario: Planet selection filter update
- **WHEN** user toggles a planet filter in the map filter dialog
- **THEN** only tags matching the selected planets SHALL be visible across categories

#### Scenario: Clear active filters
- **WHEN** user clicks the "Clear all filters" button
- **THEN** all selected tags, sort option, planet selection, and filter text SHALL be reset

#### Scenario: Sort options use flow wrap
- **WHEN** sort options are rendered in the filter dialog
- **THEN** each sort button SHALL use `WebStyles.filterChipText()` with reactive `.checked()` binding, arranged in a `Wrap` flow layout where each chip width is computed from its label text width

#### Scenario: Persistent tag and planet cache
- **WHEN** the filter dialog is opened for the first time in a session
- **THEN** tag categories and planet data SHALL be fetched from the API and stored in static signals
- **WHEN** the filter dialog is opened on subsequent occasions
- **THEN** the cached data SHALL be read directly without additional network requests

### Requirement: Chat-Styled Search Header Input
The system SHALL display the search text field inside a card container with rounded corners, padding, and a dark border (`card(Styles.black5)` with `rounded(unit(5))` and `border(1.5f, Color.darkGray)`), matching the Chat input visual style while maintaining adjacent Refresh and Filter buttons styled with `WebStyles`.

#### Scenario: Render search field
- **WHEN** the browser search header is rendered
- **THEN** the search text field SHALL be enclosed inside a rounded dark container with border, and the Refresh and Filter buttons SHALL be adjacent standalone buttons styled with WebStyles

#### Scenario: Maintain focus styling without square corners
- **WHEN** user focuses the search text field
- **THEN** the field SHALL keep its chromeless per-instance `clearInput` style with no underline and no square focus ring, and the shared global textfield style SHALL remain unmodified

### Requirement: Standardized Fixed Header Height
The system SHALL lock the search bar card container and adjacent action buttons (`refresh`, `filter`) in `BrowserSearchHeader` to a standardized fixed height (`unit(10)` / 40px) with vertically centered icons and text field, ensuring pixel-perfect baseline alignment across the header bar.

#### Scenario: Uniform header control heights
- **WHEN** `BrowserSearchHeader` is rendered
- **THEN** both the search container and the adjacent buttons SHALL have equal height of `unit(10)`

### Requirement: Dynamic Filter Chip Bar Rendering
The system SHALL render the active filter chips row using `dynamic(...)` conditioned on active tag selection. When no tags are selected, the system SHALL emit no layout elements, preventing empty table space or residual padding.

#### Scenario: No active filters produces no empty space
- **WHEN** `state.selectedTags()` is empty
- **THEN** the filter chip bar SHALL NOT emit any table row or container element

#### Scenario: Active filters render chips
- **WHEN** tags are selected
- **THEN** the filter chip bar SHALL render chips with delete icons and a clear-all button

### Requirement: Chromeless Embedded Browser Fields
Textfields embedded inside custom containers SHALL use `WebStyles.clearInput()` instead of the default underline chrome: the `BrowserSearchHeader` search field, the `BrowserFilterDialog` filter field, the `FeatureSettingsView` search field, and the `ChatInputView` message field. Standalone labeled fields (API-key and test inputs) SHALL keep the default style. No code path SHALL mutate the shared global `TextFieldStyle`.

#### Scenario: Search field renders without underline
- **WHEN** `BrowserSearchHeader` is rendered in normal, focused, or disabled state
- **THEN** its textfield shows no underline chrome while other textfields in the mod still show the default underline

#### Scenario: No global style mutation exists
- **WHEN** the browser sources are inspected
- **THEN** no statement assigns into an object obtained from `getStyle()` on a textfield

**Source: map-browser**

TBD - created by archiving change rewrite-browser-features. Update Purpose after archive.

### Requirement: Browse Online Maps
The system SHALL provide a MapBrowserFeature and MapBrowserDialog with a solid black background displaying verified maps queried from MindustryTool.searchMaps(). The dialog SHALL render map cards as physical tiles with subtle borders, rounded corners, inner padding, and WebStyles action buttons, omitting vanilla Mindustry close buttons in favor of the aligned footer close action.

#### Scenario: Display map cards as physical tiles
- **WHEN** map browser dialog is opened
- **THEN** it SHALL render against an opaque black background with a grid of map cards having distinct rounded borders, inner padding, and WebStyles action buttons

#### Scenario: Card click opens map details
- **WHEN** user clicks on a map card
- **THEN** the system SHALL open MapDetailDialog for the clicked map

### Requirement: Download and Save Maps
The system SHALL allow players to download map .msav files directly into Mindustry's custom maps directory and import them into the active game maps registry.

#### Scenario: Direct map download
- **WHEN** user clicks the Download action button on a map card or detail dialog
- **THEN** the system SHALL download map bytes, save them into Vars.customMapDirectory, import them via Vars.maps.importMap(), and show a saved notification

### Requirement: Map Detail Inspection and Launching
The system SHALL provide a MapDetailDialog showing full map preview image, author, dimensions, description, tags, and shortcuts to save or host the map.

#### Scenario: Host or play map directly
- **WHEN** user clicks Host/Play button in the map detail dialog
- **THEN** the system SHALL ensure the map is downloaded and import-ready, opening Mindustry's host or custom game setup dialog

#### Scenario: Adaptive layout on mobile orientation
- **WHEN** opened on a mobile device in portrait orientation
- **THEN** the map preview image SHALL render above the scrollable details in a single-column stack
- **WHEN** opened on a mobile device in landscape orientation
- **THEN** the map preview image and details SHALL render side-by-side in a two-column row

### Requirement: Mindustry UI and Keybind Integration
The system SHALL inject a Browse button into Mindustry's vanilla schematics dialog (Vars.ui.schematics) and register a customizable keybinding.

#### Scenario: Schematics dialog button integration
- **WHEN** SchematicBrowserFeature is enabled
- **THEN** a Browse Online button SHALL be present in Vars.ui.schematics.buttons
- **WHEN** SchematicBrowserFeature is disabled
- **THEN** the button SHALL be removed cleanly

#### Scenario: Hotkey trigger
- **WHEN** user presses the configured schematic browser keybind and no text field is focused
- **THEN** the schematic browser dialog SHALL be displayed

### Requirement: Image-First Map Hero Card
`MapCard` SHALL prioritize the map terrain preview image as the hero visual element across the top of the card inside a preview card container whose height dynamically matches its width (1:1 square aspect ratio) via a reactive preview height signal. The map title SHALL be rendered inside a translucent dark overlay centered along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with compact interactive buttons using vanilla Mindustry icons with stat counts (heart icon + likes opening details, `Icon.chatSmall` + comments opening details, `Icon.downloadSmall` + downloads triggering download, `Icon.play` triggering play). The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Map card visual presentation
- **WHEN** `MapCard` is rendered
- **THEN** the preview container occupies the upper area with width and height equal (1:1 square ratio) and title centered along the bottom, followed by a compact action button row

#### Scenario: Dynamic square aspect ratio adjustment
- **WHEN** the parent grid cell width changes due to window resize or column count reflow
- **THEN** the preview card container height SHALL update to match the new width

### Requirement: Full-Screen Map Detail Dialog
`MapDetailDialog` SHALL expand to fill the entire screen viewport. In landscape orientation, the map preview image SHALL occupy 55% of the dialog width and 100% of the available dialog height with `Scaling.fit`. In portrait orientation, the map preview image SHALL occupy 45% of the screen height with details scrolling underneath.

#### Scenario: Maximized map viewing area
- **WHEN** a map detail dialog is opened in landscape mode
- **THEN** the map preview expands to fill 55% of dialog width and 100% of dialog height

### Requirement: Zero Layout Shift Map Loading
Map preview images in cards and detail dialogs SHALL be rendered within pre-allocated sized containers featuring dark placeholders and fallbacks.

#### Scenario: Map loads without reflow
- **WHEN** a map thumbnail or detail image is loading over the network
- **THEN** the container maintains its allocated dimensions and displays a placeholder without shifting adjacent elements upon load completion

**Source: schematic-browser**

TBD - created by archiving change rewrite-browser-features. Update Purpose after archive.

### Requirement: Browse Online Schematics
The system SHALL provide a SchematicBrowserFeature and SchematicBrowserDialog with a solid black background displaying verified schematics queried from MindustryTool.searchSchematics(). The dialog SHALL render schematic cards as physical tiles with subtle borders, rounded corners, inner padding, and WebStyles action buttons, omitting vanilla Mindustry close buttons in favor of the aligned footer close action.

#### Scenario: Display schematic cards as physical tiles
- **WHEN** schematic browser dialog is opened
- **THEN** it SHALL render against an opaque black background with a grid of schematic cards having distinct rounded borders, inner padding, and WebStyles action buttons

#### Scenario: Card click in active game match
- **WHEN** user clicks on a schematic card while in an active match and rules allow schematics
- **THEN** the system SHALL download the schematic and immediately attach it to the player's placement cursor

#### Scenario: Card click in main menu
- **WHEN** user clicks on a schematic card from the main menu
- **THEN** the system SHALL open SchematicDetailDialog for the clicked schematic

### Requirement: Copy and Save Schematics
The system SHALL allow players to copy schematic Base64 strings to the clipboard and save schematics directly into Mindustry's local library.

#### Scenario: Copy schematic to clipboard
- **WHEN** user clicks the Copy action button on a schematic card or detail dialog
- **THEN** the system SHALL download schematic data, encode it as a Base64 string, copy it to the clipboard, and show a success notification

#### Scenario: Save schematic to local library
- **WHEN** user clicks the Save action button on a schematic card or detail dialog
- **THEN** the system SHALL download schematic data, deserialize it as a Mindustry Schematic, add it to Vars.schematics, and show a saved notification

### Requirement: Schematic Detail Inspection
The system SHALL provide a SchematicDetailDialog showing high-resolution preview image, author, dimensions, item requirements breakdown, tags, and description. All detail sections SHALL use `WebStyles.previewCard()` containers with consistent `gap(unit(1))` spacing. The preview image SHALL use viewport-relative sizing via `dvw()`/`dvh()` with values large enough for comfortable inspection (50% of screen height in portrait, 60% of dialog width in landscape). Action buttons SHALL use `WebStyles.cardActionText()` styling matching SchematicCard's stat buttons.

#### Scenario: View schematic requirements
- **WHEN** schematic detail dialog is presented
- **THEN** it SHALL display required construction items with item icons and amounts inside a `WebStyles.previewCard()` container with `gap(unit(1))` spacing

#### Scenario: Adaptive layout on mobile orientation
- **WHEN** opened on a mobile device in portrait orientation
- **THEN** the preview image SHALL render above the scrollable details in a single-column stack with height of 50% screen height via `dvh(50f)`
- **WHEN** opened on a mobile device in landscape orientation
- **THEN** the preview image and details SHALL render side-by-side in a two-column row with the image occupying 60% of dialog width via `dvw(60f)`

#### Scenario: Consistent section styling
- **WHEN** any detail section is rendered (author, stats, tags, requirements, description)
- **THEN** it SHALL be wrapped in a `WebStyles.previewCard()` styled container with `gap(unit(1))` internal spacing

#### Scenario: Action buttons match card style
- **WHEN** the Copy and Save action buttons are rendered in the detail dialog
- **THEN** they SHALL use `WebStyles.cardActionText()` styling with `height(unit(9))` and `growX()`, matching SchematicCard's stat button appearance

### Requirement: Mindustry UI and Keybind Integration
The system SHALL inject a Browse button into Mindustry's vanilla schematics dialog (Vars.ui.schematics) and register a customizable keybinding.

#### Scenario: Schematics dialog button integration
- **WHEN** SchematicBrowserFeature is enabled
- **THEN** a Browse Online button SHALL be present in Vars.ui.schematics.buttons
- **WHEN** SchematicBrowserFeature is disabled
- **THEN** the button SHALL be removed cleanly

#### Scenario: Hotkey trigger
- **WHEN** user presses the configured schematic browser keybind and no text field is focused
- **THEN** the schematic browser dialog SHALL be displayed

### Requirement: Image-First Schematic Hero Card
`SchematicCard` SHALL prioritize the schematic preview image as the hero visual element across the top of the card inside a preview card container whose height dynamically matches its width (1:1 square aspect ratio) via a reactive preview height signal. The schematic title SHALL be rendered inside a translucent dark overlay centered along the bottom edge of the image with text truncation. The card SHALL provide a single bottom action row with four compact interactive buttons using vanilla Mindustry icons with stat counts:
1. Like button with like count (heart icon + count)
2. Comment button with comment count (`Icon.chatSmall` + count)
3. Download/save button with download count (`Icon.downloadSmall` + count)
4. Copy string button (`Icon.copy`)
The card SHALL NOT render separate redundant non-interactive stat badge rows.

#### Scenario: Schematic card visual presentation
- **WHEN** `SchematicCard` is rendered
- **THEN** the preview container occupies the upper area with width and height equal (1:1 square ratio) and title centered along the bottom, followed by a 4-button action row

#### Scenario: Direct action interaction
- **WHEN** user clicks the download button on a card
- **THEN** it executes the save action directly without requiring the detail dialog

#### Scenario: Dynamic square aspect ratio adjustment
- **WHEN** the parent grid cell width changes due to window resize or column count reflow
- **THEN** the preview card container height SHALL update to match the new width

### Requirement: Full-Screen Schematic Detail Dialog
`SchematicDetailDialog` SHALL expand to fill the entire screen viewport. In landscape orientation, the preview image SHALL occupy 60% of the dialog width and 100% of the available dialog height with `Scaling.fit`. In portrait orientation, the preview image SHALL occupy 50% of the screen height with details scrolling underneath.

#### Scenario: Maximized viewing area in landscape
- **WHEN** a schematic detail dialog is opened in landscape mode
- **THEN** the preview image expands to fill 60% of the dialog width and full dialog height

#### Scenario: Large preview in portrait
- **WHEN** a schematic detail dialog is opened in portrait mode
- **THEN** the preview image occupies 50% of the screen height via `dvh(50f)`

### Requirement: Zero Layout Shift Image Loading
Schematic preview images in cards and detail dialogs SHALL be rendered within pre-allocated sized containers featuring dark placeholders and fallbacks.

#### Scenario: Image loads without reflow
- **WHEN** a schematic thumbnail or detail image is loading over the network
- **THEN** the container maintains its allocated dimensions and displays a placeholder without shifting adjacent elements upon load completion

