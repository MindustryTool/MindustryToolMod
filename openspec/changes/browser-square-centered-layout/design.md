## Context

`SchematicBrowserDialog` and `MapBrowserDialog` are full-screen dialogs (`fillParent(true)`) with black backgrounds. Currently, their content stretches across 100% of the viewport width. The column count is calculated using coarse breakpoints (`w / 300f`), and `ReactiveGrid` stretches each card cell (`cell.uniformX().growX()`) to consume `(availableWidth - gaps) / cols`.

As a result, card previews range from 220px to nearly 400px depending on screen width. On 1080p, 1440p, or ultrawide monitors, preview cards become giant squares, dominating vertical space and allowing only 1–2 rows of items. Additionally, the search bar header stretches to extreme widths (1500px+) with controls isolated at the screen edges, and the pagination footer is similarly dispersed.

## Goals / Non-Goals

**Goals:**
- Fix preview card dimensions to a standardized 1:1 square of `unit(58f)` (232px) for both schematic and map browsers.
- Implement uncapped auto-fitting column calculation that adds columns as screen width grows without stretching cards.
- Snap content width to `cols * CARD_WIDTH + (cols - 1) * GAP` and center the entire content container horizontally on the X-axis.
- Align `BrowserSearchHeader`, the scrollable `ReactiveGrid`, and `BrowserFooter` to this exact content width so they form a single cohesive column.
- Provide graceful downscaling on narrow mobile screens where available width is less than `CARD_WIDTH`.

**Non-Goals:**
- Changing card action functionality (likes, comments, downloads, copy, and play actions remain unchanged).
- Changing search querying, debounce, filter dialog, or pagination state management.
- Modifying detail dialogs (`SchematicDetailDialog` / `MapDetailDialog`).

## Decisions

### Decision 1: Standardized Fixed Card Size (`unit(58f)` = 232px)
- **Rationale**: `unit(58f)` is already the fallback preview height in `SchematicCard` and `MapCard`. At 232px width and 232px preview height, the 4 bottom action buttons (likes, comments, downloads, copy/play) fit comfortably with icon margins, legible counts, and touch targets.
- **Alternatives Considered**:
  - `unit(50f)` (200px): Action buttons are cramped for multi-digit counts.
  - `unit(64f)` (256px): Too few cards fit on standard laptop/desktop screens.

### Decision 2: Uncapped Responsive Column Auto-Fit
- **Formula**:
  ```java
  float cardWidth = unit(58f);
  float gap = unit(4f);
  float padding = unit(4f);
  float availableWidth = Math.max(0f, viewportWidth.get() - padding * 2f);
  int cols = Math.max(1, (int) ((availableWidth + gap) / (cardWidth + gap)));
  ```
- **Rationale**: As viewport width expands, extra columns are added without changing the card size. Cards remain at the exact same size regardless of display resolution.
- **Alternatives Considered**:
  - Capping at 5–6 columns: Dismissed per user selection to allow uncapped scaling on ultrawide monitors.

### Decision 3: Snapped Content Container Width with Horizontal Centering
- **Formula**:
  ```java
  Computed<Float> contentWidth = columnCount.map(cols ->
      Math.min(availableWidth, cols * cardWidth + (cols - 1) * gap)
  );
  ```
- **Structure**:
  ```java
  column().grow().center().children(() -> {
      column().width(contentWidth).growY().gap(unit(2)).children(() -> {
          new BrowserSearchHeader(...);
          scroll().grow().children(() -> reactiveGrid(...));
          new BrowserFooter(...);
      });
  });
  ```
- **Rationale**:
  - Because `ReactiveGrid` is placed inside a container of width `contentWidth`, its internal `itemWidth` computes to `(contentWidth - totalGaps) / cols == cardWidth`. Each cell receives exact width `cardWidth`.
  - Header and footer stretch to match the exact same width as the card grid.
  - The scrollbar in `scroll()` hugs the right-most column of cards rather than sitting hundreds of pixels away at the edge of the monitor.

## Risks / Trade-offs

- **[Narrow Viewports (< 232px)]** → On very narrow mobile viewports or high UI scales, available width may be less than `CARD_WIDTH`. Mitigation: `contentWidth` and card preview scale down using `Math.min(cardWidth, availableWidth)` so content never clips outside the viewport.
- **[Scrollbar Width Reservation]** → If a visible scrollbar reduces the usable inner width of the scroll pane, cards could theoretically experience slight wrapping if margins are zero. Mitigation: Ensure `padding` covers scrollbar width and container margin, or rely on Solim's `scroll()` overlay scrollbar behavior.
