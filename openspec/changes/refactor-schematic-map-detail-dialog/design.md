## Context

`SchematicDetailDialog` is a Solim-declarative dialog (extends `SolimDialog`, inner `DetailContent extends BaseComponent`) that displays a schematic's preview image, author, dimensions, stats, tags, requirements, and description. It already uses `dvh(45f)`/`dvw(55f)` for responsive image sizing and switches between portrait/landscape layouts via `dynamic(isPortrait())`.

`SchematicCard` establishes the visual language for the schematic browser: `WebStyles.previewCard()` containers, `WebStyles.cardActionText()` buttons, `unit(1)` gaps, `rounded(8)`, and `Scaling.fit`. The detail dialog does not consistently use these patterns — it applies inline styling and inconsistent spacing.

Both dialogs live in `mod/src/mindustrytool/features/browser/schematic/`.

## Goals / Non-Goals

**Goals:**
- Unify detail dialog styling with SchematicCard's WebStyle presets
- Enlarge the preview image with larger viewport-relative values
- Apply consistent `gap(unit(1))` across all detail sections
- Ensure action buttons (Copy/Save) match SchematicCard's stat button visual style

**Non-Goals:**
- Changing the dialog's structural layout (portrait/landscape switching stays)
- Modifying SchematicCard itself (only extract constants if needed)
- Adding new features or data to the detail view
- Changing the MapDetailDialog (separate concern)

## Decisions

### Decision 1: Use WebStyles.previewCard() for detail sections
- **Approach**: Wrap author row, stats badge, tags grid, requirements grid, and description in `card(WebStyles.previewCard().style())` containers with `gap(unit(1))` padding.
- **Rationale**: Matches SchematicCard's visual container style (dark gray bg + 1px border). Creates visual consistency across browser and detail views.
- **Alternatives Considered**: Using `Styles.black5` (too plain), `WebStyles.secondary()` (button style, not card style).

### Decision 2: Increase preview image viewport percentages
- **Approach**: Portrait: `dvh(50f)` (up from 45f). Landscape: `dvw(60f)` (up from 55f).
- **Rationale**: Larger preview images improve the inspection experience. The extra 5% is safe because the dialog already fills the viewport and details scroll.
- **Alternatives Considered**: Fixed pixel sizes (not responsive), keeping current values (no improvement).

### Decision 3: Consistent gap everywhere
- **Approach**: Use `gap(unit(1))` as the standard gap value across all sections in the detail content column and within each row/grid. action buttons use `gap(unit(1))` matching SchematicCard's stat row.
- **Rationale**: Single gap value simplifies the mental model and matches SchematicCard's established pattern.
- **Alternatives Considered**: Variable gaps per section (unnecessary complexity).

### Decision 4: Action buttons use WebStyles.cardActionText()
- **Approach**: Copy and Save buttons styled with `WebStyles.cardActionText()`, same as SchematicCard's stat buttons. Size with `height(unit(9))` and `.growX()`.
- **Rationale**: Direct visual consistency with the card's action row.
- **Alternatives Considered**: `WebStyles.ghostText()` (too subtle for primary actions).

## Risks / Trade-offs

- [Larger image may push details below fold on small screens] → Details are inside a `ScrollPane`, so scrolling handles it. The portrait layout already scrolls details below the image.
- [WebStyles.previewCard hover effect on non-clickable sections] → PreviewCard has hover brightening; applying it to static info rows (author, tags) may be misleading. Mitigate by only using previewCard on interactive containers or accept the subtle visual effect as consistent styling.
