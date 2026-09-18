## Why

Currently, `BrowserState` hardcodes `PAGE_SIZE = 20`. On high-resolution or widescreen displays (such as 1080p, 1440p, 4K, or ultrawide), the browser card grid fits anywhere from 7 to 15 columns and 3 to 6 rows. With only 20 items returned per page, only 2 rows are populated, leaving a massive empty void across the bottom half of the dialog. Conversely, smaller screens fit fewer items. Dynamically sizing the query `PAGE_SIZE` to match the exact number of cards that fit on the user's screen (clamped between 20 and 100) ensures a seamless, full screen of cards on any display.

## What Changes

- Introduce a shared `BrowserLayout` helper in `mindustrytool.features.browser.common` to compute column count, row count, and viewport item capacity from viewport dimensions (`dvw`, `dvh`).
- Update `BrowserState` to support a dynamic `pageSize` signal (`Signal<Integer>`) rather than a hardcoded static constant `PAGE_SIZE = 20`.
- Update `BrowserState.start()` / `autoFetch` to track `pageSize`, automatically refetching and resetting page index to 0 when viewport capacity changes.
- Update `SchematicBrowserDialog` and `MapBrowserDialog` to query the API with the dynamic `state.pageSize()` clamped to $[20, 100]$.
- Reactively synchronize `pageSize` with viewport dimensions, ensuring initial dialog open and window resizes fill the entire visible grid area.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `browsers`: Calculate query page size dynamically from viewport card capacity ($\text{cols} \times \text{rows}$) clamped between 20 and 100, replacing hardcoded page size 20.

## Impact

- `mod/src/mindustrytool/features/browser/common/BrowserState.java`: `PAGE_SIZE` constant remains as default/min value (20), with a new reactive `pageSize` signal, getter, and setter.
- `mod/src/mindustrytool/features/browser/common/BrowserLayout.java`: New shared utility for card grid layout math (card dimensions, columns, rows, capacity).
- `mod/src/mindustrytool/features/browser/schematic/SchematicBrowserDialog.java`: Use `BrowserLayout` and query API with dynamic `state.pageSize()`.
- `mod/src/mindustrytool/features/browser/map/MapBrowserDialog.java`: Use `BrowserLayout` and query API with dynamic `state.pageSize()`.
- APIs: MindustryTool backend search endpoints `/schematics` and `/maps` already accept `size` parameter (backend limits to max 100). No backend changes required.
