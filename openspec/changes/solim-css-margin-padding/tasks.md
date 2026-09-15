## 1. CellConfig margin API

- [ ] 1.1 Add `margin(float)`, `margin(float, float, float, float)`, `marginX(float)`, `marginY(float)`, `marginTop(float)`, `marginBottom(float)`, `marginLeft(float)`, `marginRight(float)` and reactive `Readable<Float>` overloads to `CellConfig`.
- [ ] 1.2 Deprecate `cellPadding(...)` methods on `CellConfig`, forwarding their implementation to `margin(...)`.
- [ ] 1.3 Deprecate `margin(...)` on `TableConfig` in favor of `padding(...)`.

## 2. Container Disambiguation

- [ ] 2.1 Explicitly implement `margin(...)` on `Row`, `Column`, `Card`, `Grid`, and `Button` to delegate to `CellConfig` so outer margin resolves unambiguously.
- [ ] 2.2 Verify `padding(...)` on containers (`Row`, `Column`, `Card`, etc.) continues to set inner table padding.

## 3. Popup PendingCellConfig Support

- [ ] 3.1 Update `Popup.render()` to look up `PendingCellConfig` on the root content component and apply it to the cell created in `table.add(content.element())`.

## 4. Testing & Verification

- [ ] 4.1 Add tests in `solim-core` verifying `.margin(...)` on a component inside `Row`, `Column`, and `Grid` sets the parent cell's padding.
- [ ] 4.2 Add test in `solim-core` verifying coexistence of `.margin(...)` (outer) and `.padding(...)` (inner) on a container.
- [ ] 4.3 Add test verifying `Popup` applies `.margin(...)` from its root child component to the inner popup cell.
- [ ] 4.4 Run `./gradlew test` to verify all tests pass and backward compatibility is preserved.
