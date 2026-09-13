## 1. Solim-Core Transparent Default Fix

- [x] 1.1 Update `RoundedDrawable.java` so default `fillColor` is `Color.clear` instead of `Color.white`
- [x] 1.2 Update `ElementModifiers.java` so `getOrCreateRounded` and `border()` preserve transparent background unless explicitly given a fill color
- [x] 1.3 Add unit test in `RoundedGraphicsTest.java` verifying that `border(stroke, color)` and `rounded(radius)` default to transparent fill

## 2. WebStyles Foundation

- [x] 2.1 Create `WebStyles.java` in `mindustrytool.features.browser.common`
- [x] 2.2 Implement rounded border `TextButtonStyle` and `ButtonStyle` with the channel-list blue palette (`Color(0.45f, 0.35f, 0.9f, 0.8f)`) for all state variants (`up`, `down`, `over`, `disabled`) using `RoundedDrawable`

## 3. Search Header Styling

- [x] 3.1 Update `BrowserSearchHeader.java` to wrap search `textField` in `card(Styles.black5)` with `rounded(unit(5))` and `border(1.5f, Color.darkGray)`
- [x] 3.2 Style Refresh and Filter buttons using `WebStyles` and preserve focused background rounding on the search input

## 4. Footer Alignment & Custom Close

- [x] 4.1 Update `BrowserFooter.java` to accept a close runnable and implement a balanced 3-section layout (Close on left, pagination centered, Upload on right)
- [x] 4.2 Apply `WebStyles` to all footer buttons (`Previous`, `Page Jump`, `Next`, `Upload`, and `Close`)

## 5. Dialog Background & Close Integration

- [x] 5.1 Update `MapBrowserDialog.java` to remove `addCloseButton()`, apply solid black background (`Styles.black`), and pass close action to `BrowserFooter`
- [x] 5.2 Update `SchematicBrowserDialog.java` to remove `addCloseButton()`, apply solid black background (`Styles.black`), and pass close action to `BrowserFooter`

## 6. Card Tile Enhancements

- [x] 6.1 Update `MapCard.java` with rounded corners, `border(1f, Color.darkGray)`, inner padding, and `WebStyles` action buttons
- [x] 6.2 Update `SchematicCard.java` with rounded corners, `border(1f, Color.darkGray)`, inner padding, and `WebStyles` action buttons

## 7. Verification

- [x] 7.1 Run unit tests `./gradlew :solim-core:test` to verify `RoundedGraphicsTest`
- [x] 7.2 Run `./gradlew :mod:classes` to verify Java 8 compatibility and error-free compilation
- [x] 7.3 Verify in runtime that `border()` and `rounded()` render transparent backgrounds without white boxes
