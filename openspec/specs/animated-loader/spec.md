# animated-loader Specification

## Purpose
Reusable animated circular loader component for mod dialogs.

## Requirements
### Requirement: Animated Circle Loader Component
The system SHALL provide a reusable `Loader` Solim component that renders `loader-circle.png` and continuously animates its rotation around its center point.

#### Scenario: Continuous center-origin rotation
- **WHEN** the `Loader` component is mounted and rendered on screen
- **THEN** it SHALL update its rotation each frame using `Time.delta` and keep its origin centered to prevent visual wobble

#### Scenario: Screen and dialog centering
- **WHEN** `Loader.centered()` is invoked in a container
- **THEN** it SHALL return a layout container that expands (`grow()`) and centers the spinner vertically and horizontally

### Requirement: Dialog Loading State Integration
The system SHALL display the animated circular loader instead of static loading text across all primary mod dialogs.

#### Scenario: Schematic browser loading
- **WHEN** the schematic browser is fetching data from the API
- **THEN** the dialog SHALL display `Loader.centered()` in place of static loading text

#### Scenario: Map browser loading
- **WHEN** the map browser is fetching data from the API
- **THEN** the dialog SHALL display `Loader.centered()` in place of static loading text

#### Scenario: Auth login loading
- **WHEN** the login dialog is generating the authentication URL
- **THEN** the dialog SHALL display the animated circular loader centered in the dialog content area
