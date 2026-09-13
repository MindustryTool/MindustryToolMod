## Context

The `Wrap` component (`solim/layout/Wrap.java`) is a 59-line minimal flow layout that wraps a raw Arc `Table`. It only exposes `name()`, `gap()`, and `add()` — no `LayoutModifiers` (rounded, border, growX, width, height, padding), no `children(Runnable)` declarative API, and no `background()`. This makes it unusable for styled UI. Meanwhile, `BrowserFilterDialog` chip sections use `grid(columns)` with a computed fixed column count, forcing rigid column layouts that don't adapt to varying label widths.

## Goals / Non-Goals

**Goals:**
- Enhance `Wrap` with `LayoutModifiers<Wrap>`, `SizeConstraints`, `children(Runnable)`, `padding()`, `background()`
- Replace `grid(columns)` in `BrowserFilterDialog` with `wrap()` using per-item `GlyphLayout` width
- Remove `computeColumns` helper — flow layout replaces fixed column calculation

**Non-Goals:**
- Changing filter behavior, state management, API fetching, signals, or data model
- Adding new UI components beyond enhanced Wrap
- Modifying chip visual styles (kept as-is)
- Changing the dialog structure or section organization

## Decisions

### D1: Enhance existing Wrap vs create new ResponsiveWrap

**Decision:** Enhance existing `Wrap`.

**Rationale:** The Wrap class is only 59 lines. Adding `LayoutModifiers` and `children()` follows the exact same pattern as `Column` and `Grid`. Creating a separate `ResponsiveWrap<T>` adds a new generic component for the same purpose. The enhanced Wrap is general-purpose — any code can use it for flow layout.

**Alternatives considered:**
- New `ResponsiveWrap<T>` with built-in width function: More encapsulated but over-engineered. The width function is specific to chip rendering, not a general Wrap concern.

### D2: Width function lives in FilterDialog, not Wrap

**Decision:** The `chipWidth(String)` helper method lives in `FilterDialog`, not as a parameter to Wrap.

**Rationale:** Wrap is a general-purpose flow container. It doesn't need to know about GlyphLayout or chip sizing. The caller sets `.width(chipWidth)` on each child element. This keeps Wrap simple and reusable.

### D3: Arc Table wrapping mechanism

**Decision:** Use Arc's native Table wrapping — when cells exceed the table width, they wrap to the next row.

**Rationale:** Arc's Table already supports this. The enhanced Wrap just needs to expose it through `children()` with `ParentStack`. No custom wrapping logic needed.

**How it works:**
```
Parent width: 300f
Chips: [newest=60f] [oldest=55f] [most-download=100f] [most-like=70f] [rating=50f]

Row 1: 60+55+100+70 = 285f < 300f ✓
Row 1: +50 = 335f > 300f ✗ → wrap
Row 2: [rating=50f] [updated=65f]
```

### D4: chipWidth = GlyphLayout + 8f padding + 4f safety

**Decision:** `chipWidth(label) = textWidth + 12f` where textWidth uses `GlyphLayout.setText(Fonts.def, label).width`.

**Rationale:** `FILTER_CHIP_STYLE` has `.padding(unit(1))` = 4f per side = 8f total horizontal padding. The 4f safety margin accounts for rounding and subpixel rendering.

## Risks / Trade-offs

- **[Risk] Arc Table wrapping requires finite table width** → Mitigation: Wrap inherits width from parent via `growX()`, ensuring the table has a defined width.
- **[Risk] GlyphLayout measurement at build time is static** → Mitigation: Labels don't change at runtime for sort/planet/tag/block chips. Re-measurement on viewport resize is handled by `dvw()` reactive width on the Wrap itself, not per-chip.
- **[Trade-off] Wrap gains weight (~60 → ~120 lines)** → Acceptable: follows established Column/Grid pattern, enables real usage.
