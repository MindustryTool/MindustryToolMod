## 1. Semantic Palette & Shared Button Variants

- [ ] 1.1 Define nested static class `WebStyles.Colors` with semantic tokens (`PRIMARY`, `SECONDARY`, `GHOST`, `DANGER`, `BORDER`, `DISABLED`, and interaction states)
- [ ] 1.2 Implement 5 shared `ButtonStyle` and `TextButtonStyle` singletons (`primary`, `secondary`, `outline`, `ghost`, `danger`) with built-in padding (`unit(2)`), directly inlining dimensional values
- [ ] 1.3 Expose clean public factory methods (`primary()`, `primaryText()`, `secondary()`, `secondaryText()`, `outline()`, `outlineText()`, `ghost()`, `ghostText()`, `danger()`, `dangerText()`)
- [ ] 1.4 Remove legacy aliases (`CHANNEL_BLUE*`, `webButton`, `webTextButton`) from `WebStyles.java`

## 2. Migrate Consuming Components

- [ ] 2.1 Update `BrowserFooter.java` to use `WebStyles.outlineText()`
- [ ] 2.2 Update `BrowserSearchHeader.java` to use `WebStyles.outlineText()`
- [ ] 2.3 Update `SchematicCard.java` and `MapCard.java` to use `WebStyles.outlineText()`
- [ ] 2.4 Update `SchematicBrowserDialog.java` and `MapBrowserDialog.java` to use `WebStyles.outlineText()`

## 3. Component Integration

- [ ] 3.1 Ensure `Button.style(SolimButtonStyle)` overload is available in `solim.input.Button` to accept rich style definitions with padding

## 4. Verification & Testing

- [ ] 4.1 Write unit tests verifying that all 5 variants and text counterparts return cached singletons with correct colors and padding
- [ ] 4.2 Verify zero compilation errors across `mod` and verify all browser components build cleanly
- [ ] 4.3 Run `./gradlew test` to verify complete test suite passes
