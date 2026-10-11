# settings-ui-modernization Specification

## Purpose

Standardize and modernize all settings dialog views across MindustryTool mod with a unified viewport scaffold, compact spacing system, card-based section layout, above-track background-less sliders, and declarative helper components.

## Requirements

### Requirement: Setting Dialog Viewport Alignment and Constrained Width
All feature setting dialogs SHALL align to the top on the Y-axis and center on the X-axis, with content width constrained to at most 500 units.

#### Scenario: Dialog alignment and max width
- **WHEN** a feature settings dialog is constructed and displayed
- **THEN** the dialog container constrains content to a maximum width of 500f and positions its content column with top Y-axis alignment and centered X-axis alignment.

### Requirement: Section Card Layout and Compact Spacing
All logical sections within a feature setting view SHALL be encapsulated inside cards with a compact spacing system.

#### Scenario: Card padding and inter-section spacing
- **WHEN** section cards are laid out in a settings view
- **THEN** each section card has `padding(unit(2.5f))` and inner `gap(unit(2))`, and the outer spacing between adjacent section cards is `gap(unit(3))`.

### Requirement: Standardized Header Card Structure
The first card in every feature settings view SHALL serve as the primary feature header, presenting feature identity, category badge, and description.

#### Scenario: Header card content hierarchy
- **WHEN** the settings view renders the initial header card
- **THEN** the first line displays the feature icon and feature name in accent color (`Pal.accent`) with the category badge aligned to the far right, and the second line displays the feature description wrapped in muted text (`WebStyles.Colors.GHOST_FG`).

### Requirement: Standardized Section Card Header
Every subsequent section card SHALL display a distinct header line identifying the section.

#### Scenario: Section card header display
- **WHEN** a section card is rendered after the initial header card
- **THEN** its first row displays a section icon and section name in accent color (`Pal.accent`).

### Requirement: Slider Header and Background-less Value Presentation
Sliders in setting views SHALL position their label and numerical/formatted value strictly above the slider track, and the value SHALL NOT have a background.

#### Scenario: Slider rendered above-track without badge background
- **WHEN** a slider setting is displayed in a section card
- **THEN** the title label is placed on the left and the reactive value readout is placed on the right in a header row above the slider track, the value is rendered as plain text without any badge or background, and the slider track spans the full width (`.growX()`).

### Requirement: WebStyles Variant Utilization
Setting views SHALL use `WebStyles` color tokens and button variants rather than default game button styles or raw ad-hoc colors.

#### Scenario: WebStyles applied to controls and panels
- **WHEN** interactive controls, buttons, cards, and text in settings views are rendered
- **THEN** cards use `WebStyles.Colors.SECTION_BG` and `WebStyles.Colors.SECTION_BORDER`, and buttons use `WebStyles` variants (`primary`, `outline`, `ghost`, `filterChip`, `cardAction`).

### Requirement: Setting UI Reusable Helper Components
The mod SHALL provide reusable setting UI helper primitives to standardize setting views and enforce the layout specifications.

#### Scenario: Constructing a setting view using setting helpers
- **WHEN** a feature setting view is built using `SettingScaffold`, `SettingCard`, and `SettingSlider`
- **THEN** the view automatically inherits the top-aligned scroll layout, 3-unit inter-card spacing, 2.5-unit card padding, 2-unit inner card gap, header card layout, and above-track background-less slider presentation.
