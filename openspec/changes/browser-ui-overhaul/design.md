## Context

The browser subsystem in MindustryTool provides schematic and map browsing, searching, filtering, downloading, and inspecting. While functionality is solid, user experience is degraded by visual inconsistencies:
1. Search input and adjacent buttons have mismatched heights and misaligned baselines.
2. Cards are cluttered with redundant non-interactive stat badges alongside separate action buttons, subordinating the preview image.
3. Detail dialogs confine the preview image to a small 200px box instead of utilizing the display to show detailed block arrangements and terrain.
4. Asynchronous image loading causes layout jumping as 0x0 elements abruptly pop into place.
5. Inactive elements toggle visibility but continue occupying layout space in Arc tables.

## Goals / Non-Goals

**Goals:**
- Unify search bar container and header action buttons to a fixed height (`unit(10)` / 40px) with centered contents.
- Redesign `SchematicCard` and `MapCard` to hero image-first cards with translucent bottom title overlay and 4 unified interactive action buttons (`♡`, `💬`, `⬇`, `📋`/play).
- Make `SchematicDetailDialog` and `MapDetailDialog` expand to fill the screen viewport, giving the preview image maximum possible screen area (55% width x 100% height in landscape, 45% height in portrait).
- Guarantee zero layout shift during image loading with pre-allocated placeholder containers and `Scaling.fit`.
- Replace `.visible(...)` with `dynamic(...)` on search filter chips, loading states, and error alerts to eliminate empty dead space.
- Apply consistent `padding(unit(2))` and `gap(unit(2))` throughout browser views.

**Non-Goals:**
- Modifying backend API endpoints or search contracts.
- Changing game mechanics for placing schematics or importing maps.

## Decisions

### Decision 1: Fixed Height Header Bar (`unit(10)`)
- *Rationale*: Setting `.height(unit(10))` on both the search input card container and the action buttons ensures they align with pixel precision. The search icon and text field are vertically centered inside the container.

### Decision 2: Image-First Card with Overlaid Title
- *Rationale*: Players browse schematics and maps visually. The preview image should take up the primary space of the card (~140-160px height). The title is rendered inside a translucent dark overlay across the bottom edge of the image, keeping the card compact and visually striking.
- *Layout*:
  ```
  Card (black8, rounded 6, dark border)
  ├── Stack:
  │   ├── NetworkImage (hero preview, fit scaling)
  │   └── Bottom Title Bar (translucent black, white text, ellipsis)
  └── Bottom Action Row (gap unit(1), padding unit(1)):
      ├── Button [ ♡ likes ]
      ├── Button [ 💬 comments ]
      ├── Button [ ⬇ downloads ] (triggers save/download)
      └── Button [ 📋 copy ] (for schematics) or [ ▶ play ] (for maps)
  ```

### Decision 3: Streamlined Interactive Action Buttons
- *Rationale*: Eliminates the redundant `BrowserStatsBadge` row. The bottom row buttons display the stat counts directly as part of the action (e.g. clicking the download icon with download count initiates download), saving space and reducing visual clutter.

### Decision 4: Maximized Detail Dialog Preview
- *Rationale*: When inspecting a schematic or map, the player wants to see block connections, conveyor flow, power routing, and terrain.
  - In landscape: Row with 55% width dedicated to the preview image growing to 100% of dialog height; right column houses author, dimensions, tags, requirements, description, and action buttons in a scroll view.
  - In portrait: Column with preview image taking 45% of screen height, details scrolling below.

### Decision 5: Pre-allocated Placeholder Container
- *Rationale*: To eliminate layout shift, the image container defines its dimensions prior to download completion. It displays a sleek dark rounded placeholder with an icon (`Icon.image` / `Icon.terrain`) while downloading. Once loaded, the texture paints into the existing frame without reflow.

### Decision 6: Dynamic Rendering for Collapsible Sections
- *Rationale*: In Arc's `Table`, hidden cells still take up space unless collapsed. By wrapping filter chips, loading indicators, and error rows in `dynamic(...)` returning `null` when inactive, no table cells or padding are emitted, completely removing dead space.

## Risks / Trade-offs

- **[Risk]** Long titles obscuring the image preview overlay.
  - **→ Mitigation**: The title overlay has a max height of one or two lines, padding `unit(1)`, and uses `.ellipsis(true)`.
- **[Risk]** Very wide schematics appearing too small when fitted.
  - **→ Mitigation**: Expanding the detail dialog to full screen provides more than 2.5x the previous viewing area for `Scaling.fit`.
