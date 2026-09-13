## Why

`WebStyles` currently relies on verbose intermediate variables, lacks a centralized semantic design system palette, and only provides a single button style (translucent blue wash with blue border). Developers lack standardized variants for primary call-to-action buttons, secondary/muted actions, ghost buttons, or destructive danger actions, and buttons do not have consistent built-in padding.

This change overhauls `WebStyles` into a clean, modern design system hub modeled after shadcn/ui. Rather than maintaining legacy aliases, it completely replaces legacy definitions with the new standard: a semantic color sheet (`WebStyles.Colors`) centered around `CHANNEL_BLUE` as the primary brand color, 5 shared button variants (`primary`, `secondary`, `outline`, `ghost`, `danger`) with built-in padding, and directly updates all browser components to the new standard.

## What Changes

- **Semantic Color Sheet**: Define `WebStyles.Colors` as a nested class providing semantic color tokens: `PRIMARY` (solid CHANNEL_BLUE), `PRIMARY_HOVER`, `PRIMARY_DOWN`, `PRIMARY_FG`, `PRIMARY_BG` (wash), `SECONDARY` (dark slate/indigo), `GHOST_HOVER`, `GHOST_DOWN`, `GHOST_FG`, `DANGER` (crimson red), `BORDER`, and `DISABLED_*`.
- **Remove Single-Use Variables**: Inline dimensional values directly (such as `unit(2)` radius and `1.5f` stroke) into builder declarations.
- **5 Shared Button Variants**: Provide shared, cached button styles for:
  1. `primary()` / `primaryText()`: Solid primary blue with white text and standard padding.
  2. `secondary()` / `secondaryText()`: Dark slate/indigo background with light text and standard padding.
  3. `outline()` / `outlineText()`: 1.5px blue border with translucent blue wash and standard padding.
  4. `ghost()` / `ghostText()`: Transparent default with subtle hover tint and standard padding.
  5. `danger()` / `dangerText()`: Crimson red with white text and standard padding.
- **Built-in Padding**: Configure standard button padding (`unit(2)`) on the shared button variants.
- **SolimButtonStyle Overload**: Ensure `Button.style(SolimButtonStyle)` allows directly passing rich styles that apply both Arc `ButtonStyle` and layout padding/margin/gap.
- **Clean Migration**: Remove legacy fields (`CHANNEL_BLUE`, `webButton`, `webTextButton`) and migrate all browser components (`BrowserFooter`, `BrowserSearchHeader`, `MapCard`, `SchematicCard`, `MapBrowserDialog`, `SchematicBrowserDialog`) directly to the new standard methods.

## Capabilities

### New Capabilities
<!-- None: Enhances existing web-styles specification -->

### Modified Capabilities
- `web-styles`: Add `WebStyles.Colors` semantic palette, 5 shared button variants (`primary`, `secondary`, `outline`, `ghost`, `danger`) with built-in padding, remove one-time variables, and replace legacy definitions with the new standard.

## Impact

- `mod`: `WebStyles.java` refactored into a complete design system hub without legacy alias baggage.
- All browser components (`BrowserFooter`, `BrowserSearchHeader`, `SchematicCard`, `MapCard`, `SchematicBrowserDialog`, `MapBrowserDialog`) updated to use the new standard methods (`outlineText()`, `primaryText()`, etc.).
- `solim-core`: `Button.java` extended with `Button.style(SolimButtonStyle)` overload if needed.
