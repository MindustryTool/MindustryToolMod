# schematic-preview-bounds Specification

## Purpose
Requirements for schematic preview sizing in quick schematic grid surfaces: fixed-size aspect-fit previews, centered alignment, no native-size fallback. Synced from change quick-schematic-grid-bounds.

## Requirements

### Requirement: Picker card previews fill the card

Picker card thumbnails SHALL render aspect-fit at the card size (~168px bound), centered, regardless of the schematic's native texture size.

#### Scenario: Large schematic is capped

- **WHEN** a picker card shows a large schematic (e.g. 14 x 15, native texture larger than the bound)
- **THEN** the thumbnail scales down to fit inside the card-sized box, centered, without overflowing the card.

#### Scenario: Small schematic fills the card

- **WHEN** a picker card shows a small schematic (e.g. 2 x 2)
- **THEN** the thumbnail scales up to fill the card-sized box, centered, instead of rendering small and top-left with empty bottom/right.

### Requirement: Slot dialog preview fills the card

The per-slot edit dialog preview SHALL render the resolved schematic aspect-fit at the card size (~168px bound), centered in its preview card.

#### Scenario: Slot preview fills its card

- **WHEN** the slot dialog shows any resolved schematic
- **THEN** the preview fills the card-sized box, centered, instead of rendering small and top-left.

#### Scenario: Missing schematic keeps warning

- **WHEN** the slot dialog's schematic cannot be resolved
- **THEN** the warning icon is shown instead of a preview (unchanged behavior).

### Requirement: Settings row thumbnails render at fixed small size

Settings entry rows SHALL render schematic thumbnails at 32px, aspect-fit, centered in the leading visual slot.

#### Scenario: Row layout is not crushed

- **WHEN** a settings row shows a large schematic
- **THEN** the thumbnail fits inside a 32px box and the title/subtitle/buttons keep their normal widths.

### Requirement: Preview bounds do not depend on attach timing

Preview sizing SHALL be determined by the widget's own preferred size, not by parent-cell mutation from a reactive effect.

#### Scenario: No emission needed

- **WHEN** a bounded preview is built with a static size and its size signal never emits again
- **THEN** the preview still renders at the bound on first layout and after any structural re-attach.
