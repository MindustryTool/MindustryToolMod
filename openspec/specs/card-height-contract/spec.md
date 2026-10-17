# card-height-contract Specification

## Purpose

Probe-verified contract for `Card.height()` through parent layout, including order-independence and content-size interaction. Established by change `card-height-contract` to resolve the in-game `49 vs 70` observation for `COMMAND_CARD_HEIGHT`.
## Requirements
### Requirement: Card height probes through parent layout

The system SHALL provide headless probes asserting that `Card.height(float)` through a parent `Row`/`Column` is a fixed slot: the parent cell receives `min = max = Scl.scl(v)` and the laid-out `card.table().getHeight()` equals `Scl.scl(v)` for small, exact, and large content and for both call orders.

#### Scenario: Height before attach with small content

- **WHEN** a `Card.height(70f)` is configured before `children()` with 20px fixed content inside a `Row(growX)` parent and the parent is validated and laid out
- **THEN** `card.table().getHeight()` equals `Scl.scl(70f)` and the parent cell `height` constraint is `min = max = Scl.scl(70f)`

#### Scenario: Height after attach with small content

- **WHEN** the same `Card` calls `height(70f)` after `children()` and attach inside a `Row(growX)` parent and the parent is validated and laid out
- **THEN** the measured height and cell constraint equal the before-attach case, proving no order dependence

#### Scenario: Height with large content

- **WHEN** a `Card.height(70f)` contains 120px fixed content inside a `Row(growX)` parent and the parent is validated and laid out
- **THEN** the laid-out height stays `Scl.scl(70f)` rather than expanding to the content, confirming fixed-slot semantics

#### Scenario: Column parent sanity

- **WHEN** a `Card.height(70f)` with small content is placed inside a `Column` parent instead of `Row` and laid out
- **THEN** the measured height is `Scl.scl(70f)`, ruling out `Row.ATTACHER`-specific behavior

#### Scenario: Scaled height documentation

- **WHEN** raw `70f` is compared against `Scl.scl(70f)` in the probe notes
- **THEN** the divergence explains the in-game `49 vs 70` observation and records that `height()` takes design units, not raw scene pixels

