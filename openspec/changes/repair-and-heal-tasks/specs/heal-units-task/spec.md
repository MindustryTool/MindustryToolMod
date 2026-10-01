## ADDED Requirements

### Requirement: Heal Allied Units Task
The Heal Units task MUST evaluate healing capabilities and navigate to heal damaged mobile allied units.
- Units lacking healing weapons (`unit.type.canHeal`), repair beam weapons, and repair field abilities MUST yield immediately.
- The task MUST search for the closest damaged allied unit on the same team within detection range, excluding the player unit itself and dead units.
- WHEN a damaged allied unit is found THEN the task MUST activate, set status to healing, and navigate toward the target unit.
- WHEN approaching the target unit THEN the task MUST maintain safe follow distance and continuously orient toward the target via `unit.lookAt(target)`.
- WHEN in weapon range THEN the task MUST aim at the allied unit and trigger healing weapons via `unit.controlWeapons(true)`.
- WHEN no damaged allied units exist within range THEN the task MUST yield to lower-priority tasks.

#### Scenario: Damaged allied unit detected nearby
- **WHEN** a damaged friendly mobile unit is within detection range and the player unit has healing capability
- **THEN** HealUnitsTask activates, navigates toward the allied unit, and shoots healing projectiles to restore its health

#### Scenario: Unit lacks healing capability
- **WHEN** the player unit has no healing weapons or repair abilities
- **THEN** HealUnitsTask yields immediately with status indicating it cannot heal

#### Scenario: All allied units at full health
- **WHEN** all nearby friendly units are fully repaired
- **THEN** HealUnitsTask yields to the next autoplay task in priority order
