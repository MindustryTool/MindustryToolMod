## MODIFIED Requirements

### Requirement: Picker card previews fill the card

Picker card thumbnails SHALL render aspect-fit at the card-size bound (a Solim design-unit length, e.g. `unit(42f)`) centered, regardless of the schematic's native texture size. The widget's own preferred size SHALL report the bound in scene units (`Scl.scl(bound)`), so the preview box matches its cell-scaled container at any UI scale.

#### Scenario: Large schematic is capped

- **WHEN** a picker card shows a large schematic (e.g. 14 x 15, native texture larger than the bound)
- **THEN** the thumbnail scales down to fit inside the card-sized box, centered, without overflowing the card.

#### Scenario: Small schematic fills the card

- **WHEN** a picker card shows a small schematic (e.g. 2 x 2)
- **THEN** the thumbnail scales up to fill the card-sized box, centered, instead of rendering small and top-left with empty bottom/right.

#### Scenario: Bound matches container at non-default UI scale

- **WHEN** the picker is shown at `Scl.scl() == 0.7f` and the surrounding card is sized `unit(42f)`
- **THEN** the thumbnail box and the card are the same rendered size, so the thumbnail neither overflows nor under-fills the card.

### Requirement: Slot dialog preview fills the card

The per-slot edit dialog preview SHALL render the resolved schematic aspect-fit at the card-size bound (a Solim design-unit length), centered in its preview card, with the widget's own preferred size expressed in scene units (`Scl.scl(bound)`).

#### Scenario: Slot preview fills its card

- **WHEN** the slot dialog shows any resolved schematic
- **THEN** the preview fills the card-sized box, centered, instead of rendering small and top-left.

#### Scenario: Missing schematic keeps warning

- **WHEN** the slot dialog's schematic cannot be resolved
- **THEN** the warning icon is shown instead of a preview (unchanged behavior).

### Requirement: Settings row thumbnails render at fixed small size

Settings entry rows SHALL render schematic thumbnails at a fixed small design-unit size (`unit(8f)`), aspect-fit, centered in the leading visual slot, reported by the widget's own preferred size in scene units.

#### Scenario: Row layout is not crushed

- **WHEN** a settings row shows a large schematic
- **THEN** the thumbnail fits inside its box and the title/subtitle/buttons keep their normal widths.

### Requirement: Preview bounds do not depend on attach timing

Preview sizing SHALL be determined by the widget's own preferred size, not by parent-cell mutation from a reactive effect. The preferred size SHALL be returned in scene units (`Scl.scl(bound)`) so it agrees with the `Scl`-scaled cell constraints of the surrounding layout.

#### Scenario: No emission needed

- **WHEN** a bounded preview is built with a static size and its size signal never emits again
- **THEN** the preview still renders at the bound on first layout and after any structural re-attach.

#### Scenario: Consistent units with the container

- **WHEN** a bounded preview is placed in a card whose cell is sized in design units
- **THEN** the preview's scene-unit preferred size equals the card's scene-unit size, so both agree at any UI scale.
