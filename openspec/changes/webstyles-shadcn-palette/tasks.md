## 1. Semantic Palette & Shared Button Variants

- [x] 1.1 Define nested static class `WebStyles.Colors` with semantic tokens (`PRIMARY`, `SECONDARY`, `GHOST`, `DANGER`, `BORDER`, `DISABLED`, and interaction states)
- [x] 1.2 Implement 5 shared `ButtonStyle` and `TextButtonStyle` singletons (`primary`, `secondary`, `outline`, `ghost`, `danger`) with built-in padding (`unit(2)`), directly inlining dimensional values
- [x] 1.3 Expose clean public factory methods (`primary()`, `primaryText()`, `secondary()`, `secondaryText()`, `outline()`, `outlineText()`, `ghost()`, `ghostText()`, `danger()`, `dangerText()`)
- [x] 1.4 Remove legacy aliases (`CHANNEL_BLUE*`, `webButton`, `webTextButton`) from `WebStyles.java`

## 2. Migrate Consuming Components

- [x] 2.1 Update `BrowserFooter.java` to use `WebStyles.outlineText()`
- [x] 2.2 Update `BrowserSearchHeader.java` to use `WebStyles.outlineText()`
- [x] 2.3 Update `SchematicCard.java` and `MapCard.java` to use `WebStyles.outlineText()`
- [x] 2.4 Update `SchematicBrowserDialog.java` and `MapBrowserDialog.java` to use `WebStyles.outlineText()`

## 3. Component Integration

- [x] 3.1 Ensure `Button.style(SolimButtonStyle)` overload is available in `solim.input.Button` to accept rich style definitions with padding

## 4. Verification & Testing

- [x] 4.1 Write unit tests verifying that all 5 variants and text counterparts return cached singletons with correct colors and padding
- [x] 4.2 Verify zero compilation errors across `mod` and verify all browser components build cleanly
- [x] 4.3 Run `./gradlew test` to verify complete test suite passes
