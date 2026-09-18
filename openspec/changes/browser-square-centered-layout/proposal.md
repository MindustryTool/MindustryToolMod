## Why

Currently, `SchematicBrowserDialog` and `MapBrowserDialog` stretch across 100% of the viewport width. Because column counts are calculated using coarse breakpoint ranges (`w / 300f`), card widths fluctuate wildly (from 220px to nearly 400px) depending on the monitor resolution and window scaling. On wide or high-resolution displays (1080p, 1440p, ultrawide), preview images balloon into giant squares that dominate the screen, while the search input bar stretches to absurd widths (over 1500px) with controls pushed to the screen edges.

Switching to fixed-size square cards (`unit(58f)` / 232px) with uncapped responsive auto-fit columns and centering the content column on the X-axis creates a predictable, gallery-like browsing experience where cards maintain crisp proportions, the search header and footer align perfectly with the grid, and content stays comfortably in the player's primary field of view.

## What Changes

- **Fixed-Size Square Cards**: Standardize card preview dimensions for both `SchematicCard` and `MapCard` to a fixed square of `unit(58f)` (232px) with 1:1 aspect ratio.
- **Uncapped Auto-Fit Column Calculation**: Determine column count dynamically based on the fixed card width plus gap: `cols = max(1, (availableWidth + gap) / (cardWidth + gap))`, scaling naturally across any monitor width without card stretching.
- **Snapped Content Width & X-Axis Centering**: Calculate container width as `cols * cardWidth + (cols - 1) * gap` and center the content column on the horizontal axis inside the full-screen dialog.
- **Unified Column Alignment**: Align `BrowserSearchHeader`, the scrollable card grid, and `BrowserFooter` to the same snapped content width so the entire UI forms a cohesive, centered column.
- **Scrollbar Placement**: The vertical scrollbar for the browser grid moves from the far edge of the screen to hug the right-most column of cards.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `browsers`: Update card layout and grid reflow requirements to use fixed-size square cards (`unit(58f)` / 232px), uncapped column calculation, and horizontal X-axis centering for the aligned header, grid, and footer.

## Impact

- `mod/src/mindustrytool/features/browser/schematic/SchematicBrowserDialog.java`: Update column count computation, wrap content into a width-snapped centered column.
- `mod/src/mindustrytool/features/browser/map/MapBrowserDialog.java`: Update column count computation, wrap content into a width-snapped centered column.
- `mod/src/mindustrytool/features/browser/schematic/SchematicCard.java`: Ensure fixed-size square preview layout.
- `mod/src/mindustrytool/features/browser/map/MapCard.java`: Ensure fixed-size square preview layout.
