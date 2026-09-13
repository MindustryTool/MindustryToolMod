## Context

In Mindustry build 160.1, rc.scene.ui.layout.WrapTable exists as a specialized Table subclass that calculates dynamic line wrapping during its layout() and computeSize() methods. Solim introduces solim.layout.Wrap to expose this layout to declarative mod UI. However, Solim's container conventions (spacing gap, alignment, and container hierarchy integration) must be adapted for 2D wrapping:
- Unlike a 1D Row where pplySpacing sets cell.padLeft(gap), a wrapping container has both horizontal spacing between items on the same line and vertical spacing between lines.
- WrapTable determines wrapping based on layoutWidth - hpadding. In Solim's declarative DSL, Wrap must fill its parent container width (growX()) so layoutWidth reflects the actual dialog or card width.

## Goals / Non-Goals

**Goals:**
- Make Wrap properly wrap child elements when line capacity is exceeded.
- Provide consistent 2D gap spacing (gap(float) or gap(float gapX, float gapY)).
- Implement alignment methods (left(), center(), ight(), 	op(), ottom(), justify(Justify)) on Wrap that properly configure WrapTable.align.
- Ensure Wrap attaches to parent containers with growX().

**Non-Goals:**
- Replacing WrapTable with a custom canvas or re-implementing layout logic from scratch.

## Decisions

### Decision 1: Spacing strategy for WrapTable
In WrapTable, each cell's width is computed as elementWidth + computedPadLeft + computedPadRight and row height is elementHeight + computedPadTop + computedPadBottom.
- Instead of the 1D GapContainer.applySpacing which only pads subsequent cells on padLeft, Wrap will apply padRight(gapX / 2f).padLeft(gapX / 2f).padTop(gapY / 2f).padBottom(gapY / 2f) or row-relative padding.
- Setting symmetric cell padding (padRight(gapX).padBottom(gapY)) guarantees uniform spacing across all rows and columns while WrapTable natively accounts for this in its line-wrap calculations!

### Decision 2: Attachment and Growth
In Wrap.children() and Wrap.ATTACHER:
- The Wrap component's own element (the WrapTable) attaches to its parent layout cell with growX().
- Children added inside Wrap do NOT receive growX() unless they explicitly request it, ensuring chips and buttons retain their natural preferred widths.

### Decision 3: Alignment propagation
Implement left(), center(), ight(), 	op(), ottom(), and justify() on Wrap:
- Calling .left() calls 	able.left() and aligns cells to Align.left.
- Calling .center() calls 	able.center().
- Calling .right() calls 	able.right().

## Risks / Trade-offs

- **[Risk] Margin/Gap collision** → Mitigation: Use cell padding combined with child margins in espace() so child-level margins are respected.
