## Why

The BrowserFilterDialog uses plain `Styles.togglet` buttons that look like a different app from the rest of the browser (which uses WebStyles). Tag and planet data are fetched from the API every time the dialog opens, causing unnecessary network calls. The filter options are stacked full-width, wasting horizontal space.

## What Changes

- Replace all toggle buttons (sort, planets, tags, blocks) from `Styles.togglet` to `WebStyles.ghostText()` for visual consistency with the browser design system
- Replace "Clear all" button from `Styles.defaultb` to `WebStyles.dangerText()` for clear destructive-action affordance
- Cache tag categories and planet data in static signals that persist across dialog open/close — fetch once, read forever
- Replace fixed column counts (2/4) with viewport-width-based dynamic column calculation using `max(1, floor(availableWidth / minColWidth))` where `minColWidth = longestLabel + padding + gap`
- Remove `.growX()` from individual toggle buttons — let grid cells control width for compact flowing layout

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `browser-common`: Update "Tag & Category Filtering Modal" requirement to specify ghost button styling, dynamic viewport-based column calculation, and persistent data caching

## Impact

- `mod/src/mindustrytool/features/browser/common/BrowserFilterDialog.java`: Toggle styles, column calculation, static cache signals
- `openspec/specs/browser-common/spec.md`: Update filtering modal requirement
