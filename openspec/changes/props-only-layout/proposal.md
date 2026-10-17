## Why

Layout and alignment modifiers currently exist in two conflicting forms: fluent chained methods on components (`.gap()`, `.top()`, `.left()`, `.right()`, `.bottom()`, `.center()`) and properties declared inside `.props(...)`. This creates duplication, inconsistent call sites, and confusion over modifier ordering and ownership. Enforcing all layout, spacing, and alignment exclusively through `.props(...)` establishes a single source of truth and eliminates legacy chaining.

## What Changes

- **Export Layout Props via `solim.UI`**: Re-export common layout and alignment props (`gap`, `center`, `top`, `bottom`, `left`, `right`, `align`) from `solim.UI` so they can be declared inside `.props(...)` without tedious separate static imports.
- **Remove Chained Layout Methods on Solim Components**: Remove or deprecate chained `.gap(...)`, `.top()`, `.bottom()`, `.left()`, `.right()`, and `.center()` from `Column`, `Row`, `Card`, `Wrap`, and `Grid`.
- **Migrate Mod View & Dialog Code**: Migrate all remaining chained `.gap(...)` and alignment methods across `mod/` into consolidated `.props(...)` invocations.
- **Single Source of Truth**: All component layout configuration flows exclusively through `.props(...)`.

## Capabilities

### Modified Capabilities
- `solim-ui-dx`: Layout, alignment, and gap configuration MUST be declared exclusively via `.props(...)`. Chained layout methods on components SHALL be removed.

## Impact

- `solim`: `solim.UI` re-exports `Values.gap`, `Values.center`, `Values.top`, `Values.bottom`, `Values.left`, `Values.right`, `Values.align`.
- `solim-core`: `Column`, `Row`, `Card`, `Wrap`, and `Grid` remove old chained layout methods.
- `mod`: All views, dialogs, cards, and HUDs migrate remaining chained `.gap(...)` and alignment calls into `.props(...)`.
