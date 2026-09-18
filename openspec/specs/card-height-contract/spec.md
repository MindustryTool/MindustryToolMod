# card-height-contract Specification

## Purpose

Probe-verified contract for `Card.height()` through parent layout, including order-independence and content-size interaction. Established by change `card-height-contract` to resolve the in-game `49 vs 70` observation for `COMMAND_CARD_HEIGHT`.

## Requirements

### Requirement: Card height probes through parent layout

The system SHALL provide headless probes proving how `Card.height(float)` behaves through a parent `Row`/`Column` after `validate()` + `layout()`, covering small, exact, and large content and both call orders.

#### Scenario: Height before attach with small content

- **WHEN** a `Card.height(70f)` is configured before `children()` with 20px fixed content inside a `Row(growX)` parent and the parent is validated and laid out
- **THEN** the probe records `card.table().getHeight()` and the parent cell `height` constraint for diagnosis (no fixed assertion until semantics are decided)

#### Scenario: Height after attach with small content

- **WHEN** the same `Card` calls `height(70f)` after `children()` and attach inside a `Row(growX)` parent and the parent is validated and laid out
- **THEN** the probe records the same measurements so before-vs-after order dependence is visible

#### Scenario: Height with large content

- **WHEN** a `Card.height(70f)` contains 120px fixed content inside a `Row(growX)` parent and the parent is validated and laid out
- **THEN** the probe records whether layout height stays at 70 (fixed-slot) or expands to content (pref-only) to decide the follow-up fix

#### Scenario: Column parent sanity

- **WHEN** a `Card.height(70f)` with small content is placed inside a `Column` parent instead of `Row` and laid out
- **THEN** the probe records the height to rule out `Row.ATTACHER`-specific behavior

#### Scenario: Scaled height documentation

- **WHEN** raw `70f` is compared against `Scl.scl(70f)` in the probe notes
- **THEN** the divergence is documented to explain in-game `49 vs 70` and guide whether chat must use scaled or raw units
