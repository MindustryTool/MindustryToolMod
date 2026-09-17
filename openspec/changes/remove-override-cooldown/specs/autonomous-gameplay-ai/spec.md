## MODIFIED Requirements

### Requirement: Player Manual Override
When player manual input (touch or key press) is detected, Autoplay MUST immediately yield control to the player. As soon as manual input ceases, Autoplay MUST resume on the next frame with no timed lockout. There is no configurable override-cooldown setting.

#### Scenario: Manual input yields control instantly
- **WHEN** player manual input (touch or key press) is detected
- **THEN** Autoplay MUST immediately yield control to the player, reset transient unit states, and clear the active task

#### Scenario: No lockout after input ceases
- **WHEN** player manual input ceases
- **THEN** Autoplay MUST resume task evaluation on the next frame without any cooldown delay
