# web-features Specification

## Purpose
Catalog of external web tools and interactive cards within the feature settings dialog, providing direct navigation to web services, clipboard fallback, and search filtering.

## Requirements

### Requirement: Web Feature Catalog
The system SHALL provide a catalog of default external web tools associated with the Mindustry Tool ecosystem. Each web feature MUST specify an identifier, localized name key, localized description key, relative URL constructed from `Config.WEB_URL`, and an icon.

#### Scenario: Catalog contains default tools
- **WHEN** the web features catalog is accessed
- **THEN** it contains the 8 standard web tools: Content Patches, Logic Editor, Logic Display Generator, Sorter Image Generator, Canvas Image Generator, Wiki, Posts, and Free Mindustry Server

#### Scenario: Clean URL paths
- **WHEN** a web feature URL is accessed
- **THEN** it resolves to `Config.WEB_URL` joined with the clean endpoint path without language prefix

### Requirement: Web Features Display in Feature Settings
`FeatureSettingsView` SHALL render a dedicated "Web Tools" section in its scrollable container beneath the game features list. Web features MUST be rendered using `WebFeatureCard` components adhering to Solim declarative UI patterns and matching the height (`unit(60)`) of in-game feature cards.

#### Scenario: Section visibility
- **WHEN** `FeatureSettingDialog` is opened from either the main menu or the in-game quick access HUD
- **THEN** the Web Tools section is displayed below the Mod Features section with its section header

#### Scenario: Card content presentation
- **WHEN** a web feature card is rendered
- **THEN** it displays the tool icon, localized title, localized description, a distinct web tool badge, and an external link button

### Requirement: External Link Navigation and Clipboard Fallback
The system SHALL attempt to open the web feature URL in the user's default browser upon card activation, falling back to clipboard copying if opening the browser fails.

#### Scenario: Successful browser launch
- **WHEN** the user clicks a web feature card or its external link button
- **THEN** `Core.app.openURI` is invoked with the target URL

#### Scenario: Browser launch fallback
- **WHEN** `Core.app.openURI` returns false or throws an exception
- **THEN** the URL is copied to the system clipboard and a fade notification confirming the copy is shown

### Requirement: Real-time Search Filtering
The search toolbar in `FeatureSettingsView` SHALL reactively filter both mod features and web features.

#### Scenario: Query matches web features
- **WHEN** the user enters search text matching a web feature title
- **THEN** the web features grid updates in real-time to display only matching web features

#### Scenario: Query matches no web features
- **WHEN** the search query does not match any web feature
- **THEN** the web features section and its header are hidden

### Requirement: Full Internationalization
All user-facing text for web features SHALL be translatable through `assets/bundles/bundle.properties`.

#### Scenario: Localized text lookup
- **WHEN** web feature cards and section headers are rendered
- **THEN** all labels, titles, descriptions, and tooltips are resolved via `Core.bundle`
