## Why

Currently, tooltip modifiers are defined ad-hoc on specific components like `Button` rather than universally on `ElementConfig`. Components such as `Checkbox`, `Row`, and `Card` lack `.tooltip()` modifiers, forcing developers to break out of declarative Solim and imperatively register Arc `Tooltip` listeners on the underlying `Element` or `Table` (e.g., in `GeneralSettingsView.java`). Adding `.tooltip()` directly to `ElementConfig` allows any Solim component to declare tooltips fluently, eliminates intermediate variables and wrapper rows, and promotes consistent declarative UI across the codebase.

## What Changes

- Add `.tooltip(@Nullable String tip)` to `ElementConfig` to attach text tooltips and replace existing tooltip listeners.
- Add `.tooltip(@Nullable Readable<String> tip)` to `ElementConfig` with automatic reactive `Label` binding and ambient lifecycle cleanup via `ComponentContext.register()`.
- Add `.tooltip(@Nullable Cons<Table> tooltipBuilder)` to `ElementConfig` for custom tooltip styling and multi-line content.
- Deduplicate tooltip implementations in `Button` so it inherits the shared `ElementConfig` contract.
- Refactor `GeneralSettingsView.java` to remove intermediate row variables, redundant single-child wrapper rows, and imperative `addListener(new Tooltip(...))` calls, migrating fully to declarative Solim.
- Clean up unused imports (`arc.scene.ui.Tooltip`, `solim.layout.Row`) in `GeneralSettingsView.java`.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `solim-layout`: Add requirement for `ElementConfig` to support static, reactive, and custom builder `.tooltip()` modifiers with replacement semantics and ambient lifecycle registration.

## Impact

- **Solim Core**: `solim.modifier.ElementConfig`, `solim.input.Button`.
- **Mod UI**: `mindustrytool.features.settings.GeneralSettingsView`.
- **Dependencies**: No external dependency changes. Full Java 8 runtime compatibility maintained.
