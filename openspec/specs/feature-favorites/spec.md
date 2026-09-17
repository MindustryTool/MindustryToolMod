# feature-favorites Specification

## Purpose
TBD - created by archiving change feature-favorites. Update Purpose after archive.
## Requirements
### Requirement: Persistent Favorite Storage
The system SHALL persist the set of favorited feature IDs in mod settings using a persistent configuration under key `mindustrytool.settings.favorites`.

#### Scenario: Favorite persistence across restarts
- **WHEN** a feature is marked as favorite
- **THEN** its identifier is stored in settings and remains favorited after mod restart

### Requirement: Reactive Favorite State
The system SHALL provide a reactive signal for the favorited features so that changes propagate immediately to UI components without requiring a reload.

#### Scenario: Real-time UI synchronization
- **WHEN** the favorite status of a feature changes
- **THEN** listening UI components update immediately in response to the signal

### Requirement: FeatureCard Star Toggle
The system SHALL display an interactive star button in the header of each `FeatureCard` to toggle favorite status.

#### Scenario: Star button display
- **WHEN** a feature card is rendered
- **THEN** a star button is visible in the action button bar of the card header

### Requirement: Star Visual Differentiation and Tooltips
The system SHALL visually differentiate the star button based on favorite state, displaying an accent color with a removal tooltip when favorited, and a ghost/gray color with an add tooltip when unfavorited.

#### Scenario: Favorited star appearance
- **WHEN** a feature is in the favorited set
- **THEN** the star icon is displayed in accent color with the tooltip "Remove from favorites"

#### Scenario: Unfavorited star appearance
- **WHEN** a feature is not in the favorited set
- **THEN** the star icon is displayed in gray color with the tooltip "Add to favorites"

### Requirement: Toggling Favorite Status
The system SHALL allow players to click the star button on any feature card to toggle its favorite state.

#### Scenario: Clicking unfavorited star
- **WHEN** the player clicks an unfavorited star button
- **THEN** the feature is added to the favorited set and saved to settings

#### Scenario: Clicking favorited star
- **WHEN** the player clicks a favorited star button
- **THEN** the feature is removed from the favorited set and saved to settings

### Requirement: Dedicated Favorites Section
The system SHALL display a dedicated Favorites section at the top of the Feature Settings view whenever one or more features are favorited.

#### Scenario: Favorites section appears
- **WHEN** at least one feature is favorited
- **THEN** the Favorites section header and card grid are displayed at the top of the view

### Requirement: Exclusive Partitioning
The system SHALL exclusively display favorited features in the Favorites section and exclude them from the lower "Mod Features" section.

#### Scenario: Card moves exclusively to Favorites
- **WHEN** a feature in the Mod Features list is favorited
- **THEN** the feature card moves into the Favorites section and is no longer present in the Mod Features list

### Requirement: Auto-Hide When Empty
The system SHALL completely hide the Favorites section when no features are favorited, collapsing its layout without leaving extra space.

#### Scenario: No favorites active
- **WHEN** no features are favorited
- **THEN** the Favorites section is completely hidden and only the Mod Features section is shown

### Requirement: Order Preservation
The system SHALL preserve the global feature display order within both the Favorites section and the Mod Features section.

#### Scenario: Multiple favorites maintain order
- **WHEN** multiple features are favorited
- **THEN** they appear in the Favorites section ordered according to the global feature order

### Requirement: Search Interoperability
The system SHALL filter both the Favorites section and the Mod Features section simultaneously when a search query is entered.

#### Scenario: Searching features with active favorites
- **WHEN** a user enters a search query matching a favorited feature
- **THEN** that feature is shown in the Favorites section while non-matching features are filtered out

