## Why

In Mindustry build 160, rc.scene.ui.layout.WrapTable provides auto-wrapping flow behavior. However, the Solim solim.layout.Wrap wrapper does not support standard Solim conventions:
1. It does not grow horizontally by default in parent containers, causing WrapTable to compute line breaks against 0 or minimum width.
2. It uses 1D horizontal gap spacing (padLeft), which mistakenly indents the first item on wrapped rows and provides 0 vertical gap between wrapped lines.
3. Alignment modifiers (left(), center(), ight(), justify()) are not overridden on Wrap, preventing alignment customization.

## What Changes

- Update solim.layout.Wrap to attach with default horizontal growth (growX()), ensuring WrapTable receives the full available width of its parent container.
- Implement 2D gap spacing (gapX and gapY) for Wrap, supporting uniform horizontal and vertical spacing between wrapped items without left indentations on new lines.
- Override alignment modifiers (left(), center(), ight(), 	op(), ottom(), justify()) on Wrap to properly configure WrapTable.align.
- Verify responsive wrapping and spacing across chips in BrowserFilterDialog.

## Capabilities

### New Capabilities
- wrap-flow-layout: Auto-wrapping flow container wrapping rc.scene.ui.layout.WrapTable with 2D gap spacing, default container expansion, and Solim alignment support.

### Modified Capabilities

None.

## Impact

- solim-core: solim.layout.Wrap, solim.layout.GapContainer.
- mod: BrowserFilterDialog.java (sort options, planets, and tag categories).
- Zero breaking changes to public Solim API.
