## MODIFIED Requirements

### Requirement: Interactive Map Coordinate Picker
The system SHALL support selecting world map coordinates by temporarily hiding the active dialog, capturing a world tile tap, and re-opening the dialog with the selected coordinates. The picker SHALL track at most one active listener at any time, cancel prior registrations before adding a new one, and defer event unregistration so `Events.remove` is never invoked synchronously during `Events.fire`.

#### Scenario: Select coordinate on map
- **WHEN** the player taps "Select on Map" in the unit spawner or core placement dialog
- **THEN** the dialog closes, the next map tile tap records world coordinates (X, Y), and the dialog re-opens with those coordinates filled in.

#### Scenario: Repeated picker requests do not accumulate duplicate listeners
- **WHEN** the player activates the coordinate picker multiple times without tapping a map tile
- **THEN** any previously registered picker listener is cancelled and removed, leaving exactly one active listener.

#### Scenario: Picker cancellation and deferred unregistration
- **WHEN** a tile tap is captured or `cancel()` is invoked
- **THEN** the picker listener is deactivated immediately and unregistration from `Events` is deferred to `Core.app.post`, ensuring `Events.fire` iteration is never corrupted.
