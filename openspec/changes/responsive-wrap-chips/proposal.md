## Why

The `BrowserFilterDialog` currently uses `grid(columns)` with a computed fixed column count for all chip layouts (sort, planet, tag, block). This forces chips into rigid columns that don't adapt naturally to varying label widths — short labels waste space, long labels get cramped. The `Wrap` layout component exists in Solim but is minimal (no `LayoutModifiers`, no `children()` API, no styling support), making it unusable for real UI. Enhancing `Wrap` with the full modifier API and integrating it with per-item width calculation via `GlyphLayout` gives chips natural flow layout that adapts to content and viewport.

## What Changes

- **Enhance `solim.layout.Wrap`**: Add `LayoutModifiers<Wrap>` interface, `SizeConstraints`, `children(Runnable)` with `ParentStack`, `padding()`, and `background()` methods. Wrap gains `rounded()`, `border()`, `growX()`, `width()`, `height()`, `align()`, and all other modifier capabilities.
- **Refactor `BrowserFilterDialog` chip layouts**: Replace `grid(columns)` with `wrap()` for sort, planet, tag, and block sections. Each chip gets an explicit width computed from `GlyphLayout` text measurement + 8f button padding + 4f safety margin.
- **Remove `computeColumns` helper**: No longer needed — flow layout replaces fixed column calculation.

## Capabilities

### New Capabilities

_(none — this change enhances existing capabilities)_

### Modified Capabilities

- `solim-layout`: Wrap component gains `LayoutModifiers`, `children()`, `padding()`, `background()` — new requirement for flow layout with modifier support
- `browser-common`: Filter dialog chip sections use `wrap()` with per-item width instead of `grid(columns)` with computed column count

## Impact

- **Solim core**: `solim/layout/Wrap.java` — enhanced from ~60 to ~120 lines
- **Mod**: `mod/.../BrowserFilterDialog.java` — chip rendering refactored, `computeColumns` removed
- **Dependencies**: `arc.graphics.g2d.GlyphLayout` (already available in Arc), `Fonts.def` (already used)
- **No breaking changes**: Existing `Wrap.add()` API preserved, new methods are additive
