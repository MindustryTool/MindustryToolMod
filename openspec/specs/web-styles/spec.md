# web-styles Specification

## Purpose
Provides shadcn/ui-inspired semantic design-system button styles with a centralized color sheet, five shared variants (primary/secondary/outline/ghost/danger), continuous rounded borders and multi-state visuals with built-in padding for web and browser components without mutating Arc global styles.

## Requirements

### Requirement: Semantic Color Sheet (WebStyles.Colors)
The system SHALL provide a nested `WebStyles.Colors` static class containing semantic color tokens derived from the shadcn/ui dark theme:
- Primary brand: `PRIMARY` (shadcn `primary`, deep indigo `(0.215f, 0.163f, 0.674f, 1.0f)`), `PRIMARY_HOVER` (shadcn `sidebar-primary`, brighter indigo), `PRIMARY_DOWN`, `PRIMARY_FG` (shadcn `primary-foreground`, lavender-white).
- Primary wash: `PRIMARY_BG` (primary at 15% alpha), `PRIMARY_BG_HOVER` (35%), `PRIMARY_BG_DOWN` (60%).
- Secondary: `SECONDARY` (shadcn `secondary`, dark slate `(0.153f, 0.153f, 0.166f, 0.70f)`), `SECONDARY_HOVER`, `SECONDARY_DOWN`, `SECONDARY_FG` (shadcn `secondary-foreground`).
- Ghost: `GHOST_HOVER` (white 10%, shadcn `border`), `GHOST_DOWN`, `GHOST_FG` (shadcn `muted-foreground`).
- Danger: `DANGER` (shadcn `destructive`, coral red `(1.0f, 0.391f, 0.404f, 1.0f)`), `DANGER_HOVER`, `DANGER_DOWN`, `DANGER_FG`.
- Border and disabled: `BORDER` (shadcn `border`, white 10%), `DISABLED_BG`, `DISABLED_BORDER`, `DISABLED_FG`.

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

### Requirement: WebStyles Multi-State Rounded Button Styling
The system SHALL provide isolated `TextButtonStyle` and `ButtonStyle` instances for each shared variant styled with the `WebStyles.Colors` semantic tokens, distinct `up`, `down`, `over`, and `disabled` visual states, and continuous rounded borders using `RoundedDrawable`.

#### Scenario: Normal and interactive states
- **WHEN** a button styled with any `WebStyles` shared variant is rendered in normal, hovered, or pressed state
- **THEN** it SHALL render with a rounded border and the corresponding variant tint from `WebStyles.Colors` without mutating `arc.scene.ui.Button.ButtonStyle` or `mindustry.ui.Styles` globals

#### Scenario: Disabled state styling
- **WHEN** a WebStyles button has its enabled state set to false
- **THEN** it SHALL render with a muted dark background, dark gray rounded border, and disabled font/image color

### Requirement: Direct Value Inlining
`WebStyles` SHALL define styles by inlining dimensional values directly (e.g. `unit(2)`, `1.5f`) into builder method calls without declaring disposable intermediate variables.

#### Scenario: Static initializer simplicity
- **WHEN** `WebStyles.java` source is inspected
- **THEN** no temporary single-use local variables (e.g. `int radius = ...`, `float stroke = ...`) are declared in the static initializer

### Requirement: No Legacy Aliases
`WebStyles` SHALL NOT define legacy aliases (`webButton`, `webTextButton`, `CHANNEL_BLUE`, `CHANNEL_BLUE_*`). All consuming components across the codebase SHALL reference the new standard methods (`outlineText()`, `primaryText()`, `Colors.PRIMARY`, etc.) directly.

#### Scenario: No legacy aliases exist
- **WHEN** `WebStyles.java` is inspected
- **THEN** it contains no deprecated `webButton`, `webTextButton`, or `CHANNEL_BLUE` fields
