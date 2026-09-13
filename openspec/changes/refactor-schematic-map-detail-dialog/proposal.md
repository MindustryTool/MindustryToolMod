## Why

The SchematicDetailDialog is already Solim-declarative but uses inconsistent styling compared to SchematicCard — different card styles, gap values, and spacing patterns. The preview image sizes could be larger using viewport-relative units, and the detail sections lack uniform gap spacing. Aligning the detail dialog with SchematicCard's established visual language (WebStyles.previewCard, cardActionText, consistent gaps) creates a cohesive browsing experience.

## What Changes

- Restyle detail dialog sections (author row, stats, tags, requirements, description) with `WebStyles.previewCard()` and `WebStyles.cardActionText()` to match SchematicCard
- Enlarge preview image using `dvw()`/`dvh()` viewport-relative sizing (increase from 45%/55% to larger values)
- Apply consistent `gap(unit(1))` spacing throughout all detail sections
- Extract shared styling constants (gap, padding, rounded radius, font scale) from SchematicCard for reuse in SchematicDetailDialog
- Add proper gap between the action buttons (Copy/Save) matching SchematicCard's stat button row

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `schematic-browser`: Update "Schematic Detail Inspection" requirement to specify WebStyles usage, viewport-relative image sizing, and consistent gap spacing matching SchematicCard patterns

## Impact

- `mod/src/mindustrytool/features/browser/schematic/SchematicDetailDialog.java`: Restyle inner DetailContent sections, adjust dvw/dvh values, add gap consistency
- `mod/src/mindustrytool/features/browser/schematic/SchematicCard.java`: Extract shared constants (optional — may keep inline if small)
- `openspec/specs/schematic-browser/spec.md`: Update detail inspection requirement
