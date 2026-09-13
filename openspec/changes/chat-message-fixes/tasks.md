## 1. Network Image Timeout and Headers

- [ ] 1.1 Update `NetworkImage.java` to use a 30,000ms (30s) connection timeout and add a standard `User-Agent` request header.
- [ ] 1.2 Verify texture loading resilience for CDN images.

## 2. Interactive Link Detection & Confirmation Dialog

- [ ] 2.1 Add i18n translation keys in `assets/bundles/bundle.properties` for external link confirmation dialog title, description prompt, and actions with comments.
- [ ] 2.2 Implement link parser/tokenizer in `ChatMessageListView.java` to highlight URLs with `WebStyles.Colors.PRIMARY`.
- [ ] 2.3 Add interactive click handling on link tokens that opens a Solim confirmation dialog and invokes `Core.app.openURI(url)` on confirm.

## 3. Schematic Message Card Layout & Height Calculation

- [ ] 3.1 Refactor `buildSchematicCard` in `ChatMessageListView.java` so action buttons (Info, Export, Edit, Use) are positioned below the preview image.
- [ ] 3.2 Update `ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT` from 180f to 212f.

## 4. Testing & Verification

- [ ] 4.1 Add or update unit tests in `ChatMessageHeightCalculatorTest` for schematic message height calculation.
- [ ] 4.2 Run `./gradlew test` to ensure all tests pass and no regression occurs.
