## 1. Schematic Browser Layout

- [x] 1.1 Update `SchematicBrowserDialog.BrowserContent` column count calculation to uncapped auto-fit based on `CARD_WIDTH = unit(58f)` and `gap = unit(4f)`
- [x] 1.2 Implement reactive snapped `contentWidth` calculation and wrap `BrowserSearchHeader`, `scroll(reactiveGrid)`, and `BrowserFooter` in a centered container inside `SchematicBrowserDialog`
- [x] 1.3 Verify `SchematicCard` preview maintains a 1:1 fixed square aspect ratio of `unit(58f)`

## 2. Map Browser Layout

- [x] 2.1 Update `MapBrowserDialog.BrowserContent` column count calculation to uncapped auto-fit based on `CARD_WIDTH = unit(58f)` and `gap = unit(4f)`
- [x] 2.2 Implement reactive snapped `contentWidth` calculation and wrap `BrowserSearchHeader`, `scroll(reactiveGrid)`, and `BrowserFooter` in a centered container inside `MapBrowserDialog`
- [x] 2.3 Verify `MapCard` preview maintains a 1:1 fixed square aspect ratio of `unit(58f)`

## 3. Verification & Build

- [x] 3.1 Compile codebase with `./gradlew compileJava` to ensure zero compilation or desugaring errors
- [x] 3.2 Verify layout reactivity, horizontal centering on resize, and narrow viewport responsiveness
