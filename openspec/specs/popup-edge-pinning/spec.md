# popup-edge-pinning Specification

## Purpose

Opt-in pinned-edge behavior for Solim `Popup` so popups whose content height changes after showing keep a chosen edge fixed in stage coordinates instead of growing upward from a fixed bottom-left corner. Created by archiving change army-control-popup-pinning.

## Requirements

**Source: army-control-popup-pinning**

### Requirement: Opt-in pinned edge for popup content growth
The system SHALL offer an opt-in pinned edge (`TOP` or `BOTTOM`) on Solim `Popup` that keeps the chosen edge fixed in stage coordinates as popup content height changes after showing.

#### Scenario: Top-pinned popup grows downward
- **WHEN** a popup opts into `TOP` pinning and its content height increases
- **THEN** the popup table Y is re-anchored so the top edge stays fixed and growth extends downward.

#### Scenario: Bottom-pinned popup grows upward
- **WHEN** a popup opts into `BOTTOM` pinning and its content height increases
- **THEN** the bottom edge stays fixed and growth extends upward (legacy behavior, made explicit).

#### Scenario: Shrinking preserves the pinned edge
- **WHEN** a pinned popup's content height decreases
- **THEN** the pinned edge stays fixed and the opposite edge retracts toward it.

#### Scenario: Clamping takes precedence over pinning
- **WHEN** preserving the pinned edge would push the popup outside the stage
- **THEN** the popup is clamped inside the stage even if the pinned edge moves.

#### Scenario: Default popups are unaffected
- **WHEN** a popup does not opt into edge pinning
- **THEN** content growth behaves exactly as today (fixed bottom-left corner).
