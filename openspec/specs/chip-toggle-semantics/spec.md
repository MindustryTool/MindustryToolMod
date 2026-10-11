# chip-toggle-semantics Specification

## Purpose
TBD - created by archiving change action-chip-no-checked-state. Update Purpose after archive.
## Requirements
### Requirement: Action chips expose no checked state

The WebStyles design system SHALL provide `actionChip()`, a filter-chip visual whose resolved `ButtonStyle.checked` is `null`. Because Arc's `arc.scene.ui.Button` toggles its internal `checked` flag on every click and renders `style.checked` whenever `isChecked && style.checked != null`, a style with a `null` checked drawable SHALL make that auto-toggle visually inert while preserving normal (`up`), hover (`over`), and pressed (`down`) feedback.

#### Scenario: Pressing an action chip does not latch a selected appearance

- **WHEN** a user clicks a button styled with `actionChip()`
- **THEN** the button does not adopt a persistent selected/checked background, regardless of how many times it is clicked

#### Scenario: Action chips retain hover and press feedback

- **WHEN** the pointer hovers or presses a button styled with `actionChip()`
- **THEN** the `over` / `down` visuals render as for any filter chip

### Requirement: Toggle chips keep an explicit checked visual

Styles used for selected-state toggles SHALL declare an explicit `.checked(...)` visual. `WebStyles.filterChip()` SHALL retain its `checked` drawable, and `WebStyles.cardAction()` SHALL gain a real `checked` state so a toggle bound to it visibly reflects selection.

#### Scenario: Toggle chips render a selected state when bound

- **WHEN** a `.checked(signal)` binding on a `filterChip()` resolves to true
- **THEN** the button renders the chip's `checked` background

#### Scenario: A cardAction toggle visibly reflects selection

- **WHEN** a `.checked(signal)` binding on a `cardAction()` resolves to true
- **THEN** the button renders a distinctly selected background rather than a transparent fill

### Requirement: Unused styles do not carry a phantom checked drawable

Styles with no selected-state use SHALL resolve to a `null` checked drawable rather than a transparent-fill phantom synthesized from a root border. `sectionPanel()`, `clearFilters()`, and `previewCard()` SHALL resolve `checked` to `null`.

#### Scenario: A non-toggle button on an unused style shows no change when clicked

- **WHEN** a user clicks a button styled with `sectionPanel()`, `clearFilters()`, or `previewCard()`
- **THEN** the button renders no selected state, because those styles have no checked visual

### Requirement: A declarative checked binding always wins over Arc's click auto-toggle

When a `.checked(Readable<Boolean>)` binding is attached to a button, the binding SHALL re-establish the readable's value whenever Arc's click auto-toggle would otherwise flip the underlying `checked` flag. This SHALL apply only to reactive (signal-backed) readables; constant readables SHALL be left to Arc's default behavior.

#### Scenario: Re-clicking a selected toggle keeps it visibly selected

- **WHEN** a `.checked(signal)` toggle chip is already selected and the user clicks it again
- **THEN** the chip remains visibly selected, matching the signal's value

#### Scenario: Constant readables are not pinned by the binding

- **WHEN** a button is bound with `.checked(Readable.of(constant))` — a constant, non-reactive value
- **THEN** the binding does not re-assert on click, leaving such buttons to their existing rebuild-on-toggle lifecycle

### Requirement: Misuse of checked on a checked-less style is surfaced in development

`solim.input.Button.checked(Readable)` SHALL emit a one-shot warning when the button's current style resolves to a `null` checked drawable, indicating that the bound selected state will not render.

#### Scenario: Binding checked against actionChip warns once

- **WHEN** `.checked(signal)` is called on a button styled with `actionChip()`
- **THEN** a single warning is logged that the selected state will not render, and the warning is not repeated for subsequent calls in the same run

