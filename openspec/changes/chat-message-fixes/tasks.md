## 1. Network Image Timeout and Headers

- [x] 1.1 Update `NetworkImage.java` to use a 30,000ms (30s) connection timeout and add a standard `User-Agent` request header.
- [x] 1.2 Verify texture loading resilience for CDN images.

## 2. Interactive Link Detection & Confirmation Dialog

- [x] 2.1 Add i18n translation keys in `assets/bundles/bundle.properties` for external link confirmation dialog title, description prompt, and actions with comments.
- [x] 2.2 Implement link parser/tokenizer in `ChatMessageListView.java` to highlight URLs with `WebStyles.Colors.PRIMARY`.
- [x] 2.3 Add interactive click handling on link tokens that opens a Solim confirmation dialog and invokes `Core.app.openURI(url)` on confirm.

## 3. Schematic Message Card Layout & Height Calculation

- [x] 3.1 Refactor `buildSchematicCard` in `ChatMessageListView.java` so action buttons (Info, Export, Edit, Use) are positioned below the preview image.
- [x] 3.2 Update `ChatMessageHeightCalculator.SCHEMATIC_CARD_HEIGHT` from 180f to 212f.

## 4. Shared Floating Action Popup & In-Place Translation

- [x] 4.1 Implement a shared floating action popup menu (Copy, Reply, Translate) rendered in the overlay container above the virtual list.
- [x] 4.2 Update message item click handler in `ChatMessageListView.java` to anchor the shared floating menu near the clicked card.
- [x] 4.3 Add touch-outside dismissal behavior for the floating action menu.
- [x] 4.4 Implement in-place translation rendering in the message item card and remove/supersede `MessageActionDialog`.

## 5. Testing & Verification

- [x] 5.1 Add or update unit tests in `ChatMessageHeightCalculatorTest` for schematic message height calculation.
- [x] 5.2 Run `./gradlew test` to ensure all tests pass and no regression occurs.
