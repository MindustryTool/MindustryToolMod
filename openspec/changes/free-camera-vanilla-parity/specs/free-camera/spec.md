## MODIFIED Requirements

### Requirement: Snapping to Player Unit
The system SHALL provide a `snapToPlayer()` method and bindable hotkey that immediately sets `Core.camera.position` to the player unit's coordinates `(Vars.player.x, Vars.player.y)` whenever the player is alive, resets camera panning state, and clears the vanilla `spectating` target so no stale target pulls the camera away afterwards.

#### Scenario: Snapping centers camera immediately
- **WHEN** `snapToPlayer()` is invoked while the player unit is alive
- **THEN** `Core.camera.position.x` equals `Vars.player.x` and `Core.camera.position.y` equals `Vars.player.y`

#### Scenario: Snapping clears spectate target
- **WHEN** `snapToPlayer()` is invoked while a vanilla `spectating` target is set
- **THEN** the `spectating` target is cleared and the camera remains on the player unit on subsequent frames

#### Scenario: Snapping while player dead
- **WHEN** `snapToPlayer()` is invoked while the player is dead or does not exist
- **THEN** the camera position remains unchanged without throwing an exception
