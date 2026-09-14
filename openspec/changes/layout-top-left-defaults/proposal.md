## Why

`row()` and `column()` children silently default to centered because Solim inherits Arc `Table`'s built-in center default. Every list, form, and content layout must add manual `.top().left()` workarounds, while only loaders and dialogs truly want centering.

## What Changes

- Default all layout containers (`Row`, `Column`, `Card`, `Grid`, `ReactiveGrid`) to top-left child alignment, matching `Scroll` which already does `cell.top().left()`.
- **BREAKING**: Remove `solim.layout.Container` class (zero production usage; `Ui.container()` already returns `column()`).
- **BREAKING**: Remove `solim.layout.Justify` enum and `Row.justify()` (`START/CENTER/END` duplicate `left()/center()/right()`; `BETWEEN/AROUND/EVENLY` are no-ops). Migrate the single production callsite `SettingsPanel` `justify(END)` to `right()`.
- Audit bare `row()`/`column()` callsites and add explicit `.center()` back only where centering was intentional (loaders, dialogs, empty states).

## Capabilities

### New Capabilities

- None. This change only tightens defaults and removes dead APIs.

### Modified Capabilities

- `solim-layout`: default child alignment becomes top-left; `justify(Justify)` modifier and `Justify` enum removed; `Container` requirement removed.
- `solim-card`: card container children default to top-left.
- `solim-shared-modifiers`: `Container` dropped from the covered component list.
- `solim-rounded`: `Container` dropped from the covered container list.

## Impact

- Affected code: `solim-core` `Row`, `Column`, `Card`, `Grid`, `ReactiveGrid` constructors and `ParentStack.Attacher`s; `SettingsPanel`; ~dozens of `mod/` callsites that rely on implicit centering (bare `row()`/`column()` without alignment).
- Tests: `ContainerTest` deleted; `LayoutTest`, `ElementModifiersTest`, `ComponentDefaultNameTest`, `AxisSpacingTest`, `RoundedGraphicsTest` updated; new default-alignment assertions added.
- Visual risk: bare content layouts shift from centered to top-left (intended); intentional-center layouts must be found in audit and given explicit `.center()`.
