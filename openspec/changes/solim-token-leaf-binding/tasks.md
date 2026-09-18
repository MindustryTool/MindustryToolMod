## 1. SolimToken Envelope

- [ ] 1.1 Create `solim.core.SolimToken` in `solim-core` with fields for `component`, `cellConfig`, `expanding`, and `userPayload`, plus static helper methods (`getOrCreate`, `get`, `getComponent`, `bind`, `setExpanding`, `isExpanding`).
- [ ] 1.2 Add unit tests for `SolimToken` verifying payload preservation, expanding flag updates, and component association.

## 2. ParentStack Mount-Pipeline Inversion

- [ ] 2.1 Update `ParentStack` in `solim-runtime` with a 3-argument `CellConfigurator` accepting `(Cell<?> cell, Element child, @Nullable Component comp)`.
- [ ] 2.2 Update `ParentStack.attachPendingComponents` to pass `comp` into `doAttach`.
- [ ] 2.3 Update `PendingCellConfig` static initializer to configure cells using the directly passed `comp`, falling back to `child`.
- [ ] 2.4 Add unit tests verifying `ParentStack` attaches cells and applies constraints directly from `Component` without requiring `child.userObject`.

## 3. Remove Legacy UserObject Patterns from Layout & Modifiers

- [ ] 3.1 Update `PendingCellConfig.find` to resolve via `SolimToken` and delete untyped `find(el.userObject)` recursion.
- [ ] 3.2 Update `Ui.isExpanding` to check `SolimToken.isExpanding`, completely deleting legacy `"expanding".equals(child.userObject)` checks.
- [ ] 3.3 Update `GapContainer.respace` and layout attachers (`Column`, `Row`, `Card`, `Grid`, `SolimCollapser`) to resolve via `SolimToken.getComponent(table)` and delete all `table.userObject instanceof GapContainer` checks.
- [ ] 3.4 Update `Spacer`, `Text`, and `SplitBar` to use `SolimToken.setExpanding(element, true)`, eliminating all legacy `"expanding"` string assignments.
- [ ] 3.5 Remove legacy `el.userObject = this` fallback assignments in `CellConfig.java` and `GenericGapContainer.java`.

## 4. LeafComponent Base Class & Widget Migration

- [ ] 4.1 Create `solim.core.LeafComponent<E extends Element, SELF extends LeafComponent<E, SELF>>` in `solim-core` implementing `Component`, `CellConfig<SELF>`, and `ElementConfig<SELF>`, automatically setting `SolimToken`, registering to `ComponentContext`, and registering pending attachment on `ParentStack`.
- [ ] 4.2 Migrate `SolimImage` and `NetworkImage` to extend `LeafComponent`, deleting manual `userObject = this` and duplicate delegation methods.
- [ ] 4.3 Migrate `Badge`, `Button`, and other leaf components to extend `LeafComponent` or bind via `SolimToken`, eliminating manual `userObject = this`.
- [ ] 4.4 Add unit tests for `LeafComponent` verifying automatic registration, constraint application, and lifecycle disposal.

## 5. Modernize MCP, Tests & Verification

- [ ] 5.1 Migrate `solim-mcp` (`LayoutInspector`, `ObjectGraphScanner`) to inspect `SolimToken` instead of raw untyped `userObject`.
- [ ] 5.2 Modernize all unit tests in `:solim-core`, `:solim-mcp`, and `:mod` to remove direct `element.userObject` casts and assertions, using `SolimToken.getComponent(el)` or `SolimToken.isExpanding(el)`.
- [ ] 5.3 Run all tests across the repository (`./gradlew test`) to verify zero regressions.
