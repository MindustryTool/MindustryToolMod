## Why

In Solim, declarative `Component`s wrap retained Arc `Element`s. Historically, layout constraint resolution (`PendingCellConfig.find`), gap container traversal, and expansion checks relied on ad-hoc legacy assignments directly to `element.userObject`—such as `element.userObject = this`, `element.userObject = "expanding"`, or casting `table.userObject instanceof GapContainer`.

This legacy pattern suffers from severe flaws:
1. **Easy to miss**: Primitive component authors had to manually remember `this.element.userObject = this;` in constructors. Forgetting it (as occurred in `NetworkImage`) resulted in cell constraints like `.width()`, `.growX()`, and `.pad()` silently failing.
2. **Single-slot collision and clobbering**: Storing bare strings like `"expanding"` (in `Text.growX()` or `Spacer`) clobbered component references on that element.
3. **Mount-time scavenger hunt**: `ParentStack` held the `Component` instance during children execution, but discarded it upon attachment and forced `PendingCellConfig.find` to inspect `element.userObject` to rediscover what `ParentStack` already had.
4. **Boilerplate duplication**: Leaf widgets (`SolimImage`, `NetworkImage`, `Badge`, `Text`, `Button`, etc.) duplicated dozens of delegation methods for `PendingCellConfig` and `ElementConfig`.
5. **Technical debt across the codebase**: Ad-hoc `userObject` checks and casts were sprinkled across `Ui`, `GapContainer`, `Column`, `Row`, `Card`, `PendingCellConfig`, `ElementConfig`, MCP inspectors, and tests.

## What Changes

1. **Mount-Pipeline Inversion in `ParentStack`**:
   - Update `ParentStack` attachment and `CellConfigurator` to pass `(Cell<?> cell, Element child, @Nullable Component comp)`.
   - `PendingCellConfig.applyToCell` configures the cell directly from `comp` when available, completely bypassing element lookups during declarative mounting.

2. **Structured `SolimToken` Envelope**:
   - Introduce `SolimToken` in `solim.core` stored on `Element.userObject` that cleanly isolates `component`, `cellConfig`, `expanding` boolean, and a `userPayload` field (preserving any external user/mod data).
   - Provide clean static APIs: `SolimToken.bind(element, component)`, `SolimToken.setExpanding(element, boolean)`, `SolimToken.isExpanding(element)`, and `SolimToken.getComponent(element)`.

3. **`LeafComponent<E, SELF>` Base Class**:
   - Introduce an abstract base class `LeafComponent<E extends Element, SELF extends LeafComponent<E, SELF>>` mirroring `BaseComponent` for leaf/primitive components.
   - The base constructor automatically associates the element with `SolimToken`, registers to `ComponentContext`, registers pending component attachment to `ParentStack.current()`, and provides complete implementations of `CellConfig<SELF>` and `ElementConfig<SELF>`.
   - Migrate leaf components (`SolimImage`, `NetworkImage`, `Badge`, `Button`, `Text`, etc.) to extend `LeafComponent`, eliminating hundreds of lines of duplicate delegation code.

4. **Complete Removal of Legacy `userObject` Code**:
   - **No legacy string tagging**: Eliminate all bare `"expanding"` assignments and string checks across `Spacer`, `Text`, `SplitBar`, `Ui.isExpanding`, and `LayoutInspector`.
   - **No legacy ad-hoc casts**: Replace all `table.userObject instanceof GapContainer` checks in `Column`, `Row`, `Card`, `Grid`, `SolimCollapser`, `PendingCellConfig`, and `ElementConfig` with `GapContainer.respace(Table)` or `SolimToken.getComponent(table)`.
   - **No legacy fallback in `PendingCellConfig.find`**: Remove untyped recursive `return find(el.userObject);` fallback. `PendingCellConfig.find(element)` resolves strictly via `SolimToken`.
   - **No legacy test assertions**: Modernize all unit tests in `solim-core`, `solim-mcp`, and `mod` to assert on `SolimToken.getComponent(element)` or `SolimToken.isExpanding(element)` rather than bare `element.userObject`.

## Capabilities

### Modified Capabilities
- `solim-layout`: Update `PendingCellConfig.find` and `ParentStack` cell attachment to resolve constraints via the component passed directly during mounting or via `SolimToken`. Remove all legacy bare `userObject` string tags and ad-hoc `GapContainer` casts.
- `solim-widgets`: Standardize primitive/leaf UI widgets on `LeafComponent`, guaranteeing automatic token binding, lifecycle registration, and inherited cell/element modifiers, eliminating manual `userObject` assignments.

## Impact

- `solim-runtime`: `ParentStack` attacher pipeline passes `Component` reference to `CellConfigurator`.
- `solim-core`: New classes `SolimToken` and `LeafComponent`. All primitive and container components migrated to use `SolimToken`. Legacy `userObject` assignments and checks completely deleted.
- `solim-mcp`: `LayoutInspector` and `ObjectGraphScanner` inspect `SolimToken` instead of raw string/untyped `userObject`.
- `mod`: Feature widgets (`SplitBar`, `ChatAvatar`, `BoundedSchematicImage`) and test assertions updated to use `SolimToken`.
