## MODIFIED Requirements

### Requirement: Task 4 - Repair
The Repair task MUST detect healing capabilities and repair damaged friendly structures/blocks across the map without stalling on in-progress constructions and without oscillating.
- Units lacking healing weapons (`unit.type.canHeal`), repair beam weapons, and repair field abilities MUST yield immediately.
- The task MUST search for damaged friendly buildings across the map via `Units.findDamagedTile(unit.team, unit.x, unit.y)` and fall back to searching local allied tiles via `Units.findAllyTile` when indexer has no entries, filtering out `ConstructBuild` instances.
- The task MUST focus exclusively on damaged completed buildings, leaving mobile unit healing to dedicated unit healing tasks.
- WHEN a target building is farther than 65% of engagement range (`0.65f * unit.type.range`) THEN the task MUST navigate toward the building via `moveTo(target, unit.type.range * 0.65f)`.
- WHEN a target building is within 65% of engagement range THEN the task MUST cease calling `moveTo` to prevent deceleration oscillation.
- The task MUST continuously rotate the unit to face the target building via `unit.lookAt(target)` every frame while a target is assigned, enabling fixed-mount weapons with narrow firing cones to aim accurately.
- WHEN a target building is within weapon engagement range (`target.within(unit, unit.type.range)`) THEN the task MUST aim at the target and enable weapon control via `unit.controlWeapons(true)`.
- WHEN the target is fully repaired, destroyed, or out of range THEN the task MUST reset weapon firing via `unit.controlWeapons(false)`.

#### Scenario: Damaged buildings exist alongside construction sites
- **WHEN** incomplete construction sites and damaged completed buildings exist on the map
- **THEN** RepairTask bypasses the construction sites and navigates to repair the damaged completed buildings

#### Scenario: Approaching and shooting damaged building with fixed weapon
- **WHEN** a unit with fixed healing weapons (such as Poly) targets a damaged friendly building
- **THEN** RepairTask approaches to 65% of weapon range, continuously rotates to face the building within its shoot cone, and fires healing weapons without counter-thrust jitter

#### Scenario: Local building fallback when indexer is unpopulated
- **WHEN** indexer damaged tiles are empty (such as in multiplayer or pre-damaged loaded maps) but damaged friendly buildings exist nearby
- **THEN** RepairTask locates the damaged building via local ally tile search and repairs it
