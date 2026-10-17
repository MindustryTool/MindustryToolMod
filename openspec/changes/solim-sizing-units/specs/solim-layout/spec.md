## ADDED Requirements

### Requirement: Length values are design units

All Solim length values passed as `float` or `Readable<Float>` — `Units.unit(...)`, `width`/`height`/`size`, `prefWidth`/`prefHeight`/`minWidth`/`minHeight`/`maxWidth`/`maxHeight`, `gap`, `padding`/`margin`, and the viewport dimensions (`Units.dvw`, `Units.dvh`, `Units.width()`, `Units.height()`) — SHALL be expressed in Solim design units. A design length `v` renders at `v * Scl.scl()` scene pixels, because Arc `Cell` constraints apply `Scl.scl()` while `element.getWidth()` reports scene pixels. The framework SHALL NOT apply `Scl.scl()` to design values.

#### Scenario: unit() is unscaled

- **WHEN** `Units.unit(58f)` is evaluated
- **THEN** it returns `232f` regardless of the current `Scl.scl()` scale

#### Scenario: viewport dimensions are design units

- **WHEN** `Units.width()` is read on a screen that is `G` pixels wide at UI scale `S`
- **THEN** it returns `G / S`, so a full-width element configured with `prefWidth(Units.width())` renders `G` pixels wide

#### Scenario: element width is the rendered truth

- **WHEN** an element is configured with `prefWidth(232f)` at `Scl.scl() == 0.7f` and laid out
- **THEN** `element.getWidth()` reports `162.4f` (the rendered scene width), not `232f`

## MODIFIED Requirements

### Requirement: ElementConfig provides size mutations

`ElementConfig` SHALL provide `width(float)`, `width(Readable<Float>)`, `height(float)`, `height(Readable<Float>)`, `size(float)`, `size(float, float)`, `size(Readable<Float>)`, `size(Readable<Float>, Readable<Float>)`. Each SHALL record the fixed parent-cell constraint exactly as `prefWidth`/`prefHeight` do — `width`/`height` record one axis, `size` records both — in design units, so `width(x)` is a synonym of `prefWidth(x)`. When the host has no parent cell, the recorded constraint SHALL fall back to an `Scl`-scaled element write (`element.setWidth(Scl.scl(x))` on each axis), so a standalone host such as a `Hud` root still sizes. All Readable overloads SHALL install reactive Effects.

#### Scenario: Setting element width

- **WHEN** `width(100f)` is called on an ElementConfig component attached to a parent `Table`
- **THEN** the parent cell's fixed width is set to `100` design units, and the element is left for `Table.layout()` to size

#### Scenario: Setting reactive element width

- **WHEN** `width(signal)` is called on an attached component and the signal changes from `100f` to `200f`
- **THEN** the parent cell's fixed width tracks the signal, updating to `200` design units

#### Scenario: Width and prefWidth agree

- **WHEN** `width(x)` is applied where `prefWidth(x)` would be
- **THEN** the resulting parent cell constraint and rendered element size are identical, so the two no longer disagree

#### Scenario: Standalone host still sizes

- **WHEN** `width(100f)` is applied to a component that has no parent `Table`
- **THEN** `element.getWidth()` becomes `Scl.scl(100f)`

### Requirement: Height vs min-max clarification probes

The system SHALL record via headless probes that `ElementConfig.height(float)` on layout containers (`Card`, `Row`, `Column`) is a fixed slot applied to the parent cell (`min = max = Scl.scl(v)`), independent of call order relative to `children()`/attach, and that the laid-out `card.table().getHeight()` equals `Scl.scl(v)` for small, exact, and large content.

#### Scenario: Order independence is asserted

- **WHEN** `height(70f)` is called before `children()` versus after attach for otherwise identical hierarchies inside a `Row(growX)` parent and both are laid out
- **THEN** both resulting heights equal `Scl.scl(70f)`, proving no order dependence

#### Scenario: Large content is clamped to the fixed slot

- **WHEN** a `Card.height(70f)` contains `120px` fixed content inside a `Row(growX)` parent and is laid out
- **THEN** the laid-out height stays `Scl.scl(70f)` rather than expanding to the content

#### Scenario: Scaled height is documented

- **WHEN** raw `70f` is compared against `Scl.scl(70f)` in the probe notes
- **THEN** the divergence explains the in-game `49 vs 70` observation and records that chat and browser sizing use design units, not raw scene pixels

### Requirement: Reactive Viewport Signals

The `Units` class SHALL expose reactive signals `dvw` and `dvh` representing 100% of the dynamic viewport width and 100% of the dynamic viewport height in design units (`Core.graphics.getWidth() / Scl.scl()` and `Core.graphics.getHeight() / Scl.scl()`).

#### Scenario: Reading initial dvw and dvh

- **WHEN** `Units.dvw` and `Units.dvh` signals are queried
- **THEN** they return the current screen width and height in design units, so a full-span element configured from them renders to the full viewport

### Requirement: Full Viewport Dimensions Access

The `Units` class SHALL provide access to full viewport dimensions in design units.

#### Scenario: Reading full width and height

- **WHEN** full viewport dimensions are requested
- **THEN** `Units.width()` and `Units.height()` return `100%` of the dynamic viewport width and height in design units
