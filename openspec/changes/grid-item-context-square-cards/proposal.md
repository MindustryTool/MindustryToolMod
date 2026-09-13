## Why

In the schematic and map browser dialogs, item cards dynamically resize their width based on viewport width and column count. However, the preview card container previously used a fixed static height (`unit(58)`), causing cards to appear rectangular and distorted at various screen resolutions instead of maintaining a clean 1:1 square aspect ratio.

By introducing a `GridItemContext` to `ReactiveGrid` that exposes the reactive `itemWidth()` and `columnCount()`, cards can dynamically bind their preview height to the grid item width, ensuring true square 1:1 aspect ratio previews across all screen resolutions and window resizing.

## What Changes

- Add `GridItemContext` interface to `solim-core` (`solim.layout.GridItemContext`) exposing `Readable<Float> itemWidth()` and `Readable<Integer> columnCount()`.
- Update `ReactiveGrid` in `solim-core` to track table width, dynamically compute per-item usable width, and provide an overloaded item factory accepting `(item, context)`.
- Update `solim.UI` facade with overloaded `grid` and `reactiveGrid` builder methods accepting `BiFunction<T, GridItemContext, Component>`.
- Update `SchematicCard` and `MapCard` to accept a reactive `previewHeight` / `cardWidth` signal and bind the preview container height to it.
- Update `SchematicBrowserDialog` and `MapBrowserDialog` to pass the `GridItemContext.itemWidth()` to each card.

## Capabilities

### New Capabilities
- `grid-item-context`: Provides contextual layout dimensions (`itemWidth`, `columnCount`) to child components within a `ReactiveGrid`.

### Modified Capabilities
- `schematic-browser`: Uses `GridItemContext.itemWidth()` to render 1:1 square preview cards.
- `map-browser`: Uses `GridItemContext.itemWidth()` to render 1:1 square preview cards.

## Impact

- `solim-core`: `ReactiveGrid`, `GridItemContext`.
- `solim`: `solim.UI` facades.
- `mod`: `SchematicCard`, `MapCard`, `SchematicBrowserDialog`, `MapBrowserDialog`.
- Fully backwards compatible: existing `Function<T, Component>` factories in `ReactiveGrid` continue to work without modification.
