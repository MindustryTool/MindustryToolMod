## Why

In the schematic and map browsers, the card grid container width is currently constrained tightly to the cards' combined width. When content exceeds the viewport height and Arc's `ScrollPane` activates vertical scrolling, it subtracts the scrollbar width from the content area (`areaWidth = width - scrollbarWidth`). Because horizontal scrolling is disabled, the rightmost column of cards is clipped and the scrollbar knob overlaps the card action buttons (Copy, Save/Download). Furthermore, column capacity calculations do not reserve space for a scrollbar, and underestimating vertical overhead causes single-page loads to trigger a spurious scrollbar.

## What Changes

- Add a dedicated symmetrical scrollbar gutter (`SCROLLBAR_GUTTER = unit(5f)` / 20px) in `BrowserLayout`.
- Update `calculateColumns()` and `calculateContentWidth()` to deduct the scrollbar gutters from available width and provide symmetrical space on both sides.
- In `SchematicBrowserDialog` and `MapBrowserDialog`, pad the left side of the scroll content to match the right scrollbar track, ensuring cards remain 100% centered on the screen and action buttons are never obscured.
- Increase `VERTICAL_OVERHEAD` from `unit(44f)` (176px) to `unit(50f)` (200px) to provide a safe buffer for dialog title, search header, gaps, and footer, preventing accidental 1-screen scrollbars.
- Update unit tests in `BrowserLayoutTest` to verify gutter reservation and updated overhead calculations.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `browsers`: Update `Responsive Card Grid Layout` requirement to account for scrollbar gutters and safe overhead calculations.

## Impact

- `BrowserLayout.java`: layout constants and responsive calculation methods.
- `SchematicBrowserDialog.java`: scroll content padding and layout width.
- `MapBrowserDialog.java`: scroll content padding and layout width.
- `BrowserLayoutTest.java`: test cases updated to match the new formulas.
