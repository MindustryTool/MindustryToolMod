## Why

The current browser UI across schematics and maps has several layout and visual polish issues:
1. Irregular spacing and missing gaps between header, content list, and footer.
2. The search input container and adjacent action buttons (refresh, filter) have inconsistent heights, creating visual misalignment.
3. Schematic and map cards are cluttered with redundant static stat badges alongside separate action buttons, while the thumbnail image is small and secondary.
4. Detail dialogs are constrained with small fixed 200px images rather than filling the screen and giving players an expansive, high-detail view of schematics and maps.
5. Network images cause jarring layout shifts when loading due to lack of pre-allocated dimensions and placeholders.
6. Elements conditionally hidden with `.visible(...)` leave empty table space in Arc's layout system instead of cleanly disappearing.

This change overhauls the browser UI to be image-first, spacious, cleanly aligned, and reactive without empty space.

## What Changes

- **Unified Header Heights & Spacing**: Lock the search bar container and adjacent header buttons to a standardized height (`unit(10)` / 40px) with clean vertical centering, and establish consistent `gap(unit(2))` and `padding(unit(2))` throughout browser views.
- **Image-First Cards**: Overhaul `SchematicCard` and `MapCard` to hero preview cards where the image dominates the top of the card with the title overlaid on a translucent bottom bar. Streamline the bottom row into 4 compact, interactive action buttons (`♡ Likes`, `💬 Comments`, `⬇ Downloads/Save`, `📋 Copy`/Play) removing useless redundant badges.
- **Full-Screen Detail Dialogs**: Expand `SchematicDetailDialog` and `MapDetailDialog` to fill the screen viewport with a massive preview image (taking full height and 55% width in landscape, 45% height in portrait) so players can inspect builds and maps in high detail.
- **Zero Layout Shift Image Loading**: Wrap image previews in sized containers with dark placeholders and proper scaling (`Scaling.fit`), guaranteeing zero layout shift while downloading.
- **Dynamic Conditional Rendering**: Replace `.visible(...)` with `dynamic(...)` on search filter chips, loading states, and error messages to eliminate ghost empty space in the layout hierarchy.

## Capabilities

### New Capabilities
<!-- None: Enhances existing browser specs -->

### Modified Capabilities
- `browser-common`: Standardize search bar and header button height, add proper root gaps, and convert filter chips from visibility to dynamic rendering.
- `schematic-browser`: Overhaul `SchematicCard` to image-first layout with overlaid title and 4 interactive buttons; enlarge `SchematicDetailDialog` to fill screen with massive image preview.
- `map-browser`: Overhaul `MapCard` to image-first layout with overlaid title and interactive buttons; enlarge `MapDetailDialog` to fill screen with massive map preview.

## Impact

- `mod`:
  - `BrowserSearchHeader.java`: Fixed height matching buttons, dynamic filter row.
  - `BrowserFooter.java`: Balanced gaps and padding.
  - `SchematicCard.java` & `MapCard.java`: Redesigned image-first cards.
  - `SchematicDetailDialog.java` & `MapDetailDialog.java`: Full-screen layout with massive preview images and zero layout shift.
  - `SchematicBrowserDialog.java` & `MapBrowserDialog.java`: Dynamic loading/error states and root spacing.
