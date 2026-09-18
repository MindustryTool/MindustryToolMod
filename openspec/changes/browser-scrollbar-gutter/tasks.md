## 1. Layout Engine & Constants

- [x] 1.1 Add `SCROLLBAR_GUTTER = unit(5f)` and update `VERTICAL_OVERHEAD = unit(50f)` in `BrowserLayout.java`
- [x] 1.2 Update `calculateColumns()` and `calculateContentWidth()` in `BrowserLayout.java` to budget symmetrical scrollbar gutters (`SCROLLBAR_GUTTER * 2f`)
- [x] 1.3 Add `calculateCardsWidth()` helper in `BrowserLayout.java` for explicit card grid width measurement

## 2. Dialog Integration & Padding

- [x] 2.1 Update `SchematicBrowserDialog.java` to apply `SCROLLBAR_GUTTER` left padding inside the scroll container and align search header and footer
- [x] 2.2 Update `MapBrowserDialog.java` to apply `SCROLLBAR_GUTTER` left padding inside the scroll container and align search header and footer

## 3. Verification & Deployment

- [x] 3.1 Update `BrowserLayoutTest.java` test cases to verify symmetrical gutter reservation, content width calculations, and updated row capacity
- [x] 3.2 Run automated tests with `./gradlew test` to verify all unit tests pass
- [x] 3.3 Compile and deploy mod with `./gradlew :mod:deploy`
