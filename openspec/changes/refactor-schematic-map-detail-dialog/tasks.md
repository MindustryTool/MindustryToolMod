# Tasks

## 1. Preview Image Sizing

- [ ] 1.1 Increase portrait preview image height from `dvh(45f)` to `dvh(50f)` in `SchematicDetailDialog.java` <!-- id: 1.1 -->
- [ ] 1.2 Increase landscape preview image width from `dvw(55f)` to `dvw(60f)` in `SchematicDetailDialog.java` <!-- id: 1.2 -->

## 2. Section Styling with WebStyles

- [ ] 2.1 Wrap author row and dimensions row inside `details()` with `card(WebStyles.previewCard().style())` containers <!-- id: 2.1 -->
- [ ] 2.2 Wrap `BrowserStatsBadge` row with `card(WebStyles.previewCard().style())` container <!-- id: 2.2 -->
- [ ] 2.3 Wrap tags section (label + grid) with `card(WebStyles.previewCard().style())` container <!-- id: 2.3 -->
- [ ] 2.4 Wrap requirements section (label + grid) with `card(WebStyles.previewCard().style())` container <!-- id: 2.4 -->
- [ ] 2.5 Wrap description text with `card(WebStyles.previewCard().style())` container <!-- id: 2.5 -->

## 3. Action Button Restyling

- [ ] 3.1 Replace Copy button style from `Styles.defaultb` to `WebStyles.cardActionText()` and set `height(unit(9))` <!-- id: 3.1 -->
- [ ] 3.2 Replace Save button style from `Styles.defaultb` to `WebStyles.cardActionText()` and set `height(unit(9))` <!-- id: 3.2 -->

## 4. Gap Consistency

- [ ] 4.1 Change details column gap from `unit(2)` to `unit(1)` in `details()` method <!-- id: 4.1 -->
- [ ] 4.2 Change landscape layout gap from `unit(4)` to `unit(2)` in `landscapeLayout()` method <!-- id: 4.2 -->
- [ ] 4.3 Change action buttons row gap from `unit(2)` to `unit(1)` to match SchematicCard stat row <!-- id: 4.3 -->

## 5. Compile and Test

- [ ] 5.1 Run `./gradlew :mod:compileJava` to verify clean compilation <!-- id: 5.1 -->
- [ ] 5.2 Run `./gradlew test` to verify full test suite passes <!-- id: 5.2 -->
