## Why

The Map and Schematic browser dialogs currently use vanilla `BaseDialog` with a transparent container, causing underlying dialogs (such as `FeatureSettingDialog`) to shine through and create visually cluttered, overlapping interfaces. Furthermore, the browser search bar and cards lack the modern design tokens established in the Chat UI—such as card-wrapped input with rounded borders, subtle dark outlines on card tiles, and consistent rounded-corner button styling using the chat channel list's signature blue/indigo theme.

Additionally, Solim's `RoundedDrawable` and `ElementModifiers` currently default `fillColor` to opaque `Color.white`. Consequently, calling `.border(stroke, color)` or `.rounded(radius)` without an explicit fill color automatically turns the element's background solid white instead of remaining transparent.

## What Changes

- **Transparent Default for Rounded & Border (solim-core)**: Update `RoundedDrawable` and `ElementModifiers` so that the default `fillColor` is transparent (`Color.clear`). Calling `.border(stroke, color)` or `.rounded(radius)` without a color will no longer inject an unwanted solid white background.
- **WebStyles System**: Introduce `WebStyles` in `mindustrytool.features.browser.common` providing custom `ButtonStyle` and `TextButtonStyle` instances with rounded borders (`RoundedDrawable`) and all state variants (`up`, `down`, `over`, `disabled`) using the chat channel list's blue/indigo color palette (`Color(0.45f, 0.35f, 0.9f, 0.8f)`), without mutating Arc global styles.
- **Solid Black Background**: Render an opaque black background (`Styles.black`) across `MapBrowserDialog` and `SchematicBrowserDialog` viewports to completely block background dialogs from leaking through.
- **Custom Close & Footer Alignment**: Remove the vanilla Mindustry `addCloseButton()` bottom row from `MapBrowserDialog` and `SchematicBrowserDialog`. Reorganize `BrowserFooter` into a balanced 3-section layout featuring a custom `WebStyles` Close button on the left, centered pagination controls (`Prev`, `Page X`, `Next`), and `Upload` on the right.
- **Chat-Style Search Box**: Update `BrowserSearchHeader` to wrap the search text field inside `card(Styles.black5)` with a rounded container and dark border (`rounded(unit(5))`, `border(1.5f, Color.darkGray)`), keeping the Refresh and Filter buttons adjacent and styled with `WebStyles`.
- **Card Tile Styling**: Upgrade `MapCard` and `SchematicCard` containers with a subtle border (`border(1f, Color.darkGray)`), rounded corners (`rounded(6)`), inner padding, and action buttons (`Download`, `Copy`, `Save`, `Details`) styled with `WebStyles`.

## Capabilities

### New Capabilities
- `web-styles`: Custom button styling foundation providing continuous rounded border drawables and multi-state visual variants (`up`, `down`, `over`, `disabled`) styled with the channel-list blue palette for browser components.

### Modified Capabilities
- `solim-rounded`: Ensure `RoundedDrawable` and `ElementModifiers` default to transparent (`Color.clear`) background fill so `.border()` and `.rounded()` do not introduce an unwanted solid white background.
- `browser-common`: Update search header to use chat-style card/border search input, reorganize footer into balanced 3-section layout with custom close action, and apply `WebStyles` across controls.
- `map-browser`: Provide solid black dialog background, remove `addCloseButton()`, and render `MapCard` as physical tiles with rounded borders and `WebStyles` action buttons.
- `schematic-browser`: Provide solid black dialog background, remove `addCloseButton()`, and render `SchematicCard` as physical tiles with rounded borders and `WebStyles` action buttons.

## Impact

- **Codebase**:
  - `solim-core`: `RoundedDrawable.java`, `ElementModifiers.java`, `RoundedGraphicsTest.java`
  - New class: `mindustrytool.features.browser.common.WebStyles`
  - Modified classes: `BrowserSearchHeader`, `BrowserFooter`, `MapBrowserDialog`, `SchematicBrowserDialog`, `MapCard`, `SchematicCard`
- **Dependencies**: Uses `solim.graphics.RoundedDrawable` and Solim core layout/style APIs.
- **Breaking Changes**: None. Mod-internal UI styling and engine default corrections.
