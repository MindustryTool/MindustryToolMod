# solim-input-extensions Specification

## Purpose
Provides enter-key submission and validation hooks on SolimTextField.

## Requirements
### Requirement: Enter Key Submission
The system SHALL provide onEnter(Consumer<String> onSubmit) and onEnter(Runnable onSubmit) on solim.input.SolimTextField.

#### Scenario: Pressing enter key with text
- **WHEN** user types text into the field and presses the Enter key
- **THEN** the onEnter callback is invoked with the current text content of the field.

#### Scenario: Enter key on disabled field
- **WHEN** the text field is disabled or disabled signal evaluates to true
- **THEN** pressing Enter does not trigger the onEnter callback.

### Requirement: Custom Input Validation Feedback
The system SHALL allow attaching custom input validator predicates to SolimTextField with reactive validity signals.

#### Scenario: Content validation check
- **WHEN** a validator predicate is registered on the text field
- **THEN** validity updates reactively based on the current text length and format.

### Requirement: InputStyle Value Object
The system SHALL provide an immutable `InputStyle` value in `solim-core` describing textfield chrome. Any background slot left unset SHALL fall back to the field's base style at application time; cursor, selection, fonts, and colors behave the same way.

#### Scenario: Partial preset inherits the rest
- **WHEN** an `InputStyle` declares only background drawables and is applied to a field
- **THEN** the field keeps its base font, font colors, cursor, and selection while using the preset backgrounds

### Requirement: Per-Instance Input Styling Without Leaks
`SolimTextField` SHALL accept an `InputStyle` and install it by copying the field's current `TextFieldStyle` and overriding only the declared slots on the copy. The shared base style object SHALL remain unmodified, and sibling fields SHALL be visually unaffected.

#### Scenario: Styled field does not leak to siblings
- **WHEN** an `InputStyle` with blank backgrounds is applied to one field while another field shares the same base style
- **THEN** the styled field renders without chrome and the sibling field still renders the base chrome, and the base style object is reference-equal to its pre-application state

#### Scenario: Repeated application is allocation-free on the preset side
- **WHEN** the same `InputStyle` instance is applied to multiple fields
- **THEN** the preset itself is shared (one copy per field instance only, as Arc requires per-widget style objects)
