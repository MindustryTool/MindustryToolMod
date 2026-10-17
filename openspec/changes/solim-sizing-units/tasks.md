## 1. Sizing props become cell-driving

- [x] 1.1 Change `Values.width(Object)` / `width(Readable<Float>)` to `CELL` props that record `config.prefWidth`, keeping the `Math.max(0f, value)` clamp
- [x] 1.2 Change `Values.height(Object)` / `height(Readable<Float>)` to `CELL` props that record `config.prefHeight`
- [x] 1.3 Change `Values.size(...)` overloads to `CELL` props recording `config.prefWidth` and/or `config.prefHeight` for both axes
- [x] 1.4 Remove the now-unused raw element width/height setters in `Values` (or repurpose them for the fallback in group 2)

## 2. Standalone element fallback

- [x] 2.1 Add the `Scl`-aware fallback to `PendingCellConfig.applySize`: when `parentCell(element) == null`, write `element.setWidth/Height(Scl.scl(value))` and invalidate the hierarchy
- [x] 2.2 Ensure the fallback does not run when a parent cell exists, so `Table.layout()` stays authoritative

## 3. NetworkImage unified sizing

- [x] 3.1 Route `NetworkImage.size(float, float)` and `size(Readable, Readable)` through `constraints.prefWidth`/`prefHeight` plus the shared fallback instead of raw `element.setWidth`
- [x] 3.2 Confirm the decode/mask target size still uses the design-unit value and that rounded-corner masking is unaffected

## 4. Preview bound units

- [x] 4.1 Make `BoundedSchematicImage.bound()` return `Scl.scl(rawBound)` so its preferred size is in scene units
- [x] 4.2 Verify the picker/slot/quick-schematic-grid preview boxes match their cell-scaled containers at `Scl.scl() == 0.7f`

## 5. Feature call-site consolidation

- [x] 5.1 `SchematicCard`: collapse `width(size) + prefWidth(size)` and `size(size) + prefWidth(size) + prefHeight(size)` pairs to `prefWidth`/`prefHeight`
- [x] 5.2 `MapCard`: same consolidation
- [x] 5.3 `ChatOverlayHudView`: collapse the expanded-window `width(winWidth) + prefWidth(winWidth) + height(winHeight) + prefHeight(winHeight)` pair
- [x] 5.4 Grep the affected files for any remaining mismatched `width(a)`/`prefWidth(b)` pairs and reconcile them to one value

## 6. Tests

- [x] 6.1 Update `solim-core` sizing/prop tests that assert raw element width after `width(x)` to the fixed-cell contract (`width(x)` == `prefWidth(x)`)
- [x] 6.2 Add a standalone-host test proving a cell-less component sized with `width(x)` yields `element.getWidth() == Scl.scl(x)`
- [x] 6.3 Add a test proving `width(x)` alone yields the same parent-cell constraint and rendered size as `prefWidth(x)`
- [x] 6.4 Finalize the `card-height-contract` probes into assertions that `height(70f)` lays out at `Scl.scl(70f)` for small/exact/large content and both call orders, under a fixed test `Scl`
- [x] 6.5 Update `NetworkImage` sizing tests to the unified path

## 7. Documentation

- [x] 7.1 Document the design-unit contract (`value * Scl.scl()`, `element.getWidth()` as the rendered truth) in the sizing-prop jsdoc
- [x] 7.2 Add the same unit contract to `docs/`
