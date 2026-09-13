## ADDED Requirements

### Requirement: Semantic Color Sheet (WebStyles.Colors)
The system SHALL provide a nested `WebStyles.Colors` static class containing semantic color tokens:
- Primary brand: `PRIMARY` (solid channel blue `(0.45f, 0.35f, 0.90f, 1.0f)`), `PRIMARY_HOVER`, `PRIMARY_DOWN`, `PRIMARY_FG` (`Color.white`).
- Primary wash: `PRIMARY_BG` (translucent wash `(0.45f, 0.35f, 0.90f, 0.15f)`), `PRIMARY_BG_HOVER`, `PRIMARY_BG_DOWN`.
- Secondary: `SECONDARY` (dark slate `(0.20f, 0.20f, 0.28f, 0.70f)`), `SECONDARY_HOVER`, `SECONDARY_DOWN`, `SECONDARY_FG`.
- Ghost: `GHOST_HOVER`, `GHOST_DOWN`, `GHOST_FG`.
- Danger: `DANGER` (crimson red `(0.85f, 0.25f, 0.25f, 1.0f)`), `DANGER_HOVER`, `DANGER_DOWN`, `DANGER_FG`.
- Border and disabled: `BORDER`, `DISABLED_BG`, `DISABLED_BORDER`, `DISABLED_FG`.

#### Scenario: Accessing semantic tokens
- **WHEN** `WebStyles.Colors.PRIMARY` or `WebStyles.Colors.DANGER` is referenced
- **THEN** it returns the corresponding non-null `Color` token

### Requirement: Shared Button Variants and Text Counterparts
`WebStyles` SHALL provide standard button style variants and matching text button variants:
- `primary()` / `primaryText()`
- `secondary()` / `secondaryText()`
- `outline()` / `outlineText()`
- `ghost()` / `ghostText()`
- `danger()` / `dangerText()`
Each variant SHALL be pre-built and cached as a reusable singleton with zero runtime allocations.

#### Scenario: Retrieving cached button variants
- **WHEN** `WebStyles.primary()`, `WebStyles.outlineText()`, or any variant method is called repeatedly
- **THEN** it returns the cached singleton instance with zero new object allocations

#### Scenario: Text button styling
- **WHEN** `WebStyles.outlineText()` is inspected
- **THEN** it contains `Fonts.def` with matching state-dependent font colors and outline drawables

### Requirement: Built-in Button Padding
Shared button variants SHALL configure built-in default padding (`unit(2)`) so that buttons styled with these variants automatically receive standard internal spacing around children.

#### Scenario: Button padding applied from style
- **WHEN** a button is styled with a `WebStyles` shared variant via `button.style(...)`
- **THEN** the button's internal padding is set to `unit(2)`

### Requirement: Direct Value Inlining
`WebStyles` SHALL define styles by inlining dimensional values directly (e.g. `unit(2)`, `1.5f`) into builder method calls without declaring disposable intermediate variables.

#### Scenario: Static initializer simplicity
- **WHEN** `WebStyles.java` source is inspected
- **THEN** no temporary single-use local variables (e.g. `int radius = ...`, `float stroke = ...`) are declared in the static initializer

### Requirement: Clean Removal of Legacy Aliases and Migration
`WebStyles` SHALL NOT define legacy aliases (`webButton`, `webTextButton`, `CHANNEL_BLUE`, `CHANNEL_BLUE_*`). All consuming components across the codebase SHALL reference the new standard methods (`outlineText()`, `primaryText()`, `Colors.PRIMARY`, etc.) directly.

#### Scenario: No legacy aliases exist
- **WHEN** `WebStyles.java` is inspected
- **THEN** it contains no deprecated `webButton`, `webTextButton`, or `CHANNEL_BLUE` fields
