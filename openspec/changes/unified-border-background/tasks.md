## 1. Core Graphics & RoundedDrawable Enhancements

- [x] 1.1 Add baseDrawable support to RoundedDrawable so underlying drawables (e.g. Styles.black6) are drawn beneath the fill and rounded border stroke
- [x] 1.2 Implement dynamic corner radius safety clamping in RoundedDrawable.draw(...) to prevent NinePatch negative slice coordinate inversion when radius > min(width, height) / 2
- [x] 1.3 Add unit tests in solim-core verifying RoundedDrawable base drawable rendering and dynamic radius clamping

## 2. ElementModifiers & LayoutModifiers Composition

- [x] 2.1 Update ElementModifiers.background(...) to preserve existing RoundedDrawable border/radius by updating baseDrawable
- [x] 2.2 Update ElementModifiers.border(...) and ElementModifiers.rounded(...) to wrap existing non-rounded backgrounds into a baseDrawable
- [x] 2.3 Add .background(Color) and .background(Readable<Color>) overloads to ElementModifiers and LayoutModifiers
- [x] 2.4 Add unit tests in solim-core verifying all permutations of .background(), .rounded(), and .border() produce identical composite results

## 3. UI Integration & Verification

- [ ] 3.1 Update BrowserFilterDialog section panel to cleanly use .rounded(...), .border(...), and .background(...)
- [ ] 3.2 Verify project builds and all unit tests pass with gradlew :solim-core:test :mod:test
