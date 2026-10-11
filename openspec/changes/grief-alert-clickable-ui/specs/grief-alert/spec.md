## MODIFIED Requirements

### Requirement: Alert presentation

The system SHALL present grief alerts as an interactive, clickable Solim HUD banner at the top-center of the screen rather than sending text messages to chat or standard toasts, and SHALL allow the player to jump the camera to the offending placement by clicking the banner.

#### Scenario: Interactive banner presentation
- **WHEN** an explosive reactor placement begins near a core
- **THEN** an interactive HUD banner appears at the top-center of the screen displaying the builder name, block name, and coordinates without posting to chat or showing a standard toast

#### Scenario: Camera jump on banner click
- **WHEN** the player clicks the grief alert banner
- **THEN** the camera centers immediately onto the world coordinates of the target block, camera panning is retained so it does not snap back, and the alert banner is dismissed

#### Scenario: Manual dismissal
- **WHEN** the player clicks the dismiss [X] button on the alert banner
- **THEN** the banner is dismissed without altering the camera position

#### Scenario: Auto-dismiss timeout
- **WHEN** an alert banner is displayed and not clicked or closed for 7 seconds
- **THEN** the alert banner automatically closes

## ADDED Requirements

### Requirement: Offending placement visual highlight

The system SHALL render a temporary visual pulsing ring marker at the offending tile's world position for 3 seconds when the camera jumps to the target block.

#### Scenario: Highlight rendered on camera jump
- **WHEN** the camera jumps to the target placement from the alert banner
- **THEN** a pulsing highlight ring is rendered at the target tile's world coordinates for 3 seconds
