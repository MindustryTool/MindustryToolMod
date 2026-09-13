# Tasks

## 1. Solim Modifier and Layout Spacing Updates
- [x] 1.1 Remove pad* methods from ElementModifiers.java <!-- id: 1.1 -->
  - Remove pad(Table, float), pad(Table, float, float, float, float), and directional padTop, padBottom, padLeft, padRight from solim.modifier.ElementModifiers
- [x] 1.2 Add paddingX, paddingY, marginX, marginY to ElementModifiers.java <!-- id: 1.2 -->
  - Implement paddingX(@Nullable Table, float) and paddingY(@Nullable Table, float)
  - Implement paddingX(@Nullable Element, float) and paddingY(@Nullable Element, float)
  - Implement marginX(@Nullable Table, float) and marginY(@Nullable Table, float)
  - Implement marginX(@Nullable Element, float) and marginY(@Nullable Element, float)
- [x] 1.3 Add marginX and marginY to LayoutModifiers.java <!-- id: 1.3 -->
  - Add default SELF marginX(float x) and default SELF marginX(Readable<Float> x) to configure horizontal padLeft and padRight
  - Add default SELF marginY(float y) and default SELF marginY(Readable<Float> y) to configure vertical padTop and padBottom

## 2. Container and Component Spacing Methods
- [x] 2.1 Update Column.java and Row.java <!-- id: 2.1 -->
  - Remove all pad(float), pad(float, float, float, float), padTop, padBottom, padLeft, padRight methods
  - Add paddingX(float x), paddingY(float y), marginX(float x), and marginY(float y) methods delegating to ElementModifiers
- [x] 2.2 Update Text.java, SolimImage.java, and NetworkImage.java <!-- id: 2.2 -->
  - In Text.java: add paddingX(float x), paddingY(float y), marginX(float x), and marginY(float y)
  - In SolimImage.java: add paddingX(float x), paddingY(float y), marginX(float x), and marginY(float y)
  - In NetworkImage.java: add paddingX(float x), paddingY(float y), marginX(float x), and marginY(float y)
- [x] 2.3 Update Container.java, Card.java, and Button.java <!-- id: 2.3 -->
  - Add paddingX(float x) and paddingY(float y) to Container.java and Card.java
  - Add marginX(float x), marginY(float y), marginX(@Nullable Readable<Float> x), and marginY(@Nullable Readable<Float> y) to Button.java

## 3. Codebase Migration and Verification
- [x] 3.1 Migrate existing pad* calls in mod code and Solim tests <!-- id: 3.1 -->
  - Replace .pad() calls in AuthOverlay.java with .padding()
  - Migrate Solim component test references from pad* to padding*
- [x] 3.2 Add automated unit tests for two-axis spacing <!-- id: 3.2 -->
  - Add tests verifying paddingX and paddingY set horizontal and vertical padding respectively
  - Add tests verifying marginX and marginY set horizontal and vertical margin constraints respectively, for both static floats and reactive signals
- [x] 3.3 Execute test suite and verify build <!-- id: 3.3 -->
  - Run ./gradlew :solim-core:test to verify full compilation and test suite passing
