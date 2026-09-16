## MODIFIED Requirements

### Requirement: Quick Access Settings Configuration
The system SHALL provide a settings dialog to customize HUD opacity, scale, grid columns, individual feature visibility, and feature display order on the Quick Access HUD.

#### Scenario: Changing HUD parameters updates HUD reactively
- **WHEN** the user modifies opacity, scale, columns, or feature visibility in the Quick Access settings dialog
- **THEN** the HUD updates its styling and layout reactively and saves settings immediately.

#### Scenario: Declarative Settings Dialog Structure
- **WHEN** `QuickAccessSettingsDialog` is constructed
- **THEN** it configures slider and checkbox inputs without manual `.subscribe()` calls or temporary single-use local variables, utilizing declarative chained row layouts (`row().gap(...).children(...)`).

#### Scenario: Reordering features updates HUD reactively
- **WHEN** the user activates a row's up or down arrow in the Quick Access settings dialog
- **THEN** the stored display order swaps the adjacent entries, both the settings rows and the HUD re-render in the new order, and the change persists.
