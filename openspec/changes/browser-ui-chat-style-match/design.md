## Context

The schematic and map browsers are Solim-based dialogs implemented across `mindustrytool.features.browser.*`. However, when opened over other dialogs (such as `FeatureSettingDialog`), the absence of an opaque stage background causes background components to shine through, breaking immersion and readability. In addition, the search bar, browser action buttons, and card grids use default/unbordered Arc styles instead of the polished dark-card and rounded-border aesthetics introduced in the Chat UI.

Furthermore, Solim's `RoundedDrawable` and `ElementModifiers` currently default their fill color to `Color.white`. Consequently, calling `.border(stroke, color)` or `.rounded(radius)` without an explicit fill color automatically turns the element's background solid white.

## Goals / Non-Goals

**Goals:**
- Fix `RoundedDrawable` and `ElementModifiers` in `solim-core` to default to `Color.clear` so that calling `.border()` or `.rounded()` without an explicit background color preserves transparency instead of turning white.
- Provide an isolated `WebStyles` class defining fresh, rounded-border `TextButtonStyle` and `ButtonStyle` instances without mutating Arc's shared `Styles`.
- Match the chat channel list's signature blue/indigo color palette (`Color(0.45f, 0.35f, 0.9f, 0.8f)`) across all state variants (`up`, `down`, `over`, `disabled`).
- Give `MapBrowserDialog` and `SchematicBrowserDialog` an opaque black background (`Styles.black`) to completely hide underlying dialogs and menu elements.
- Replace vanilla `addCloseButton()` with a custom `WebStyles` close action integrated directly into an aligned 3-section footer layout.
- Wrap `BrowserSearchHeader`'s text input inside a chat-styled card container (`card(Styles.black5)` + `rounded(unit(5))` + `border(1.5f, Color.darkGray)`) while keeping Refresh and Filter buttons adjacent and styled with `WebStyles`.
- Transform `MapCard` and `SchematicCard` into distinct physical tiles with subtle borders (`border(1f, Color.darkGray)`), rounded corners (`rounded(6)`), inner padding, and `WebStyles` action buttons.

**Non-Goals:**
- Mutating Arc's global `Styles` or `Tex` drawables.
- Replacing the backend API or query fetching logic in `BrowserState`.
- Rewriting filter modal logic or pagination state handling.

## Decisions

### 1. Transparent Default in `RoundedDrawable` & `ElementModifiers`
- **Choice**: Change `RoundedDrawable`'s default `fillColor` from `Color.white` to `Color.clear`. When `ElementModifiers.rounded(element, radius, color)` is called with a null color, or `border(element, stroke, color)` is called, the default fill remains `Color.clear`.
- **Rationale**: An element modifier named `.border()` should strictly define the border; it should not inject a solid white background. Similarly, `.rounded(radius)` without an explicit color should define shape/corner radius without forcing a white fill.

### 2. Dedicated `WebStyles` with `RoundedDrawable`
- **Choice**: Implement `WebStyles` inside `mindustrytool.features.browser.common` using `solim.graphics.RoundedDrawable`.
- **Rationale**: `RoundedDrawable` provides continuous-curvature rounded rects with separate stroke borders in a single draw pass. Creating separate style objects avoids polluting global Arc themes.
- **Alternatives Considered**: Mutating `Styles.defaultb` or `Styles.clearNonei` directly. Rejected because it violates Arc stability and risks side effects across unrelated game dialogs.

### 3. Opaque Dialog Surface
- **Choice**: Apply an opaque black surface (`Styles.black`) to the dialog viewport.
- **Rationale**: `BaseDialog.setFillParent(true)` defaults to a transparent container in Arc. Setting `Styles.black` on the main container prevents underlying UI trees (e.g., `FeatureSettingDialog`) from being visible through the browser.

### 4. Balanced 3-Section Footer Layout
- **Choice**: Remove `addCloseButton()` and integrate a custom Close button into `BrowserFooter` alongside the pagination and upload controls.
  - Left: Close / Back button (`WebStyles.webTextButton`)
  - Center: `[ Prev ] [ Page X ] [ Next ]` pagination cluster
  - Right: `[ Upload ]` button (`WebStyles.webTextButton`)
- **Rationale**: Replaces the awkward bottom-center Mindustry "Quay lại" bar with a cohesive, balanced footer bar that fits the modern web-style dashboard aesthetic.

### 5. Input Wrapper vs Adjacent Action Buttons
- **Choice**: Wrap only the search text field inside the rounded card border (`card(Styles.black5)` + `border(1.5f, Color.darkGray)`), while keeping Refresh and Filter buttons as adjacent standalone buttons with `WebStyles`.
- **Rationale**: Ensures the search field matches the Chat composer's look and feel while preserving immediate tactile access to Refresh and Filter controls without cluttering the input container.

### 6. Card Tile Appearance
- **Choice**: Add `border(1f, Color.darkGray)`, `rounded(6)`, and inner padding to each card component (`MapCard`, `SchematicCard`).
- **Rationale**: Gives cards distinct physical boundaries against the black background, preventing thumbnails and stats from looking like loose floating elements.

## Risks / Trade-offs

- **[Risk] Existing usages expecting `.rounded()` without color to default to white** → **Mitigation**: Existing callers that wanted a white background either called `.rounded(radius, Color.white)` or wrapped content in white components; callers wanting transparent borders (like `Tabs` and `ChatInputView`) had to pass `Color.clear` manually. Changing the default to `Color.clear` makes behavior intuitive and removes boilerplate.
- **[Risk] Focus background overriding rounded corners on search input** → **Mitigation**: Sync `focusedBackground = background` on the text field style, exactly as done in `ChatInputView`.
