## 1. Loader Component Implementation

- [x] 1.1 Create `Loader.java` in `mindustrytool.components` with continuous rotation, centered origin, and size/color customization
- [x] 1.2 Implement `Loader.centered()` convenience methods for vertical and horizontal container centering

## 2. Dialog Integration

- [x] 2.1 Replace static loading text in `SchematicBrowserDialog.java` with `Loader.centered()`
- [x] 2.2 Replace static loading text in `MapBrowserDialog.java` with `Loader.centered()`
- [x] 2.3 Replace static loading text in `AuthLoginDialog.java` with `Loader.centered()`
- [x] 2.4 Replace tag loading text in `BrowserFilterDialog.java` with centered inline `Loader`

## 3. Verification & Build

- [x] 3.1 Compile classes with `.\gradlew.bat :mod:classes` and run test suite to verify zero regressions
