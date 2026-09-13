## Why

In Solim layouts (Row, Column, Card, Container, etc.), .rounded(), .border(), and .background() currently overwrite each other on Arc's underlying Table.setBackground(). If .background(...) is called after .border(), the border is destroyed; if .border(...) is called after .background(...), the background is destroyed. Additionally, Row and Column lack .background(Color) overloads, and excessively large corner radii relative to element height break NinePatch geometry with inverted lines and overlapping crossover caps.

## What Changes

- Enable **order-independent styling**: Calling .rounded(), .border(), and .background(...) on any Solim layout container in any sequence composes seamlessly without destroying the other styling facets.
- Add .background(Color) and .background(Readable<Color>) overloads to LayoutModifiers and ElementModifiers.
- Enhance RoundedDrawable to support an underlying baseDrawable layer (e.g. Styles.black6 or custom textures), rendering the base drawable first and the rounded border outline on top without requiring nested Arc tables.
- Add dynamic radius safety clamping in RoundedDrawable to ensure rendered corner radius never exceeds half the element dimension (min(width, height) / 2), preventing NinePatch mathematical coordinate inversions and visual line artifacts.
- Update BrowserFilterDialog section panel styling to verify clean composition of background, border, and rounded.

## Capabilities

### New Capabilities
- unified-border-background: Order-independent composition of background drawables/colors, continuous-curvature rounded corners, and border strokes on Solim elements with automatic geometry safety clamping.

### Modified Capabilities

None.

## Impact

- solim-core: solim.graphics.RoundedDrawable, solim.modifier.ElementModifiers, solim.layout.LayoutModifiers.
- mod: BrowserFilterDialog.java (and any other browser views using section/card panel styling).
- Zero breaking API changes: All existing .rounded() and .border() methods remain fully backwards-compatible.
