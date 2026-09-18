## Context

In `BrowserLayout.java`, column count and content width calculations did not account for Arc's `ScrollPane` scrollbar mechanics. When vertical scrolling occurs, Arc's `ScrollPane` reduces available area width (`areaWidth = width - scrollbarWidth`). Because the content width was tightly constrained to the exact width of the cards, the rightmost column was clipped and the scrollbar knob overlapped the action buttons on the right edge of cards. Additionally, `VERTICAL_OVERHEAD` was set to `unit(44f)` (176px), which underestimated the actual dialog chrome and triggered an unwanted scrollbar on full-screen single-page views.

## Goals / Non-Goals

**Goals:**
- Budget dedicated symmetrical scrollbar gutters (`SCROLLBAR_GUTTER = unit(5f)` / 20px) on both sides of the card grid.
- Ensure the rightmost column's interactive action buttons (Copy, Save/Download, Play) are never obscured or clipped by the scrollbar.
- Maintain strict optical centering of the card grid within the dialog.
- Increase `VERTICAL_OVERHEAD` to `unit(50f)` (200px) to prevent false vertical overflows on single-page views.
- Update tests in `BrowserLayoutTest` to reflect gutter reservation and overhead calculations.

**Non-Goals:**
- Removing or hiding the scrollbar (`Styles.noBarPane`).
- Altering internal card component structure or action button positioning.

## Decisions

### 1. Symmetrical Gutters (Option 1)
- Left gutter: `unit(5f)` (20px) padding.
- Right gutter: `unit(5f)` (20px) space reserved for Arc's vertical scrollbar.
- *Rationale*: If only a right gutter were added, the cards would be shifted left by ~10px relative to the dialog center. Symmetrical gutters ensure the cards remain 100% centered while providing a clear track for the scrollbar.

### 2. Gutter Width of `unit(5f)` (20px)
- *Rationale*: Arc's scrollbar knob in Mindustry is typically 16px wide. Allocating 20px (`unit(5f)`) gives the 16px knob a 4px margin from card boundaries, preventing visual collision or misclicks.

### 3. Layout Dimensions & Formulas
- `availableWidth = Math.max(0f, viewportWidth - HORIZONTAL_PADDING * 2f - SCROLLBAR_GUTTER * 2f)`
- `cols = Math.max(1, (int) ((availableWidth + CARD_GAP) / (CARD_WIDTH + CARD_GAP)))`
- `cardsWidth = cols * CARD_WIDTH + (cols - 1) * CARD_GAP`
- `contentWidth = cardsWidth + SCROLLBAR_GUTTER * 2f`
- The inner dialog column is sized to `contentWidth`.
- The scroll container pads its left side by `SCROLLBAR_GUTTER`. The scrollbar naturally occupies the right `SCROLLBAR_GUTTER`.

### 4. `VERTICAL_OVERHEAD = unit(50f)` (200px)
- *Rationale*: Dialog top header (~64px) + outer padding (16px) + search bar (44px) + layout gaps (16px) + pagination footer (40px) + window frame borders $\approx$ 190–200px. Setting `VERTICAL_OVERHEAD` to 200px ensures single-page loads fit cleanly without tripping vertical scrolling.

## Risks / Trade-offs

- **[Loss of 40px horizontal width on small viewports]** → Clamped to at least 1 column; responsive sizing smoothly adapts column count.
- **[Search header & footer alignment]** → Spanning `contentWidth` with `paddingX(BrowserLayout.SCROLLBAR_GUTTER)` keeps the search bar and footer aligned with the card columns.
