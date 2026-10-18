## ADDED Requirements

### Requirement: Reactive Multi-Line Document Binding
`SolimCodeEditor` (exposed via `UI.codeEditor(...)` in `:solim`) SHALL provide two-way reactive binding to a `Signal<String>` representing the plain-text multi-line document without embedding color markup tags in the underlying string value.

#### Scenario: Updating signal updates editor lines
- **WHEN** the bound `Signal<String>` is updated programmatically with multi-line text `"let a = 1\nlet b = 2"`
- **THEN** `SolimCodeEditor` updates its internal line buffer to two lines (`"let a = 1"` and `"let b = 2"`) and clamps the cursor position within valid document bounds

#### Scenario: Typing in editor updates signal with plain text
- **WHEN** the user types or pastes code into `SolimCodeEditor` while a syntax token highlighter is active
- **THEN** the bound `Signal<String>` receives the exact plain-text document without any `[#rrggbb]` color markup tags

#### Scenario: Disposing editor stops reactive updates
- **WHEN** `SolimCodeEditor.dispose()` is called and the bound `Signal<String>` subsequently changes
- **THEN** the disposed editor no longer updates its line buffer or triggers listeners

### Requirement: Pluggable Syntax Token Highlighting
`SolimCodeEditor` SHALL accept a pluggable token highlighter function that maps a line of text (and line index) to colored token spans for rendering while preserving exact character column metrics for cursor placement and text selection.

#### Scenario: Rendering highlighted tokens without shifting cursor offset
- **WHEN** a line contains `"let count = 42"` and the token highlighter assigns distinct colors to keywords (`let`), identifiers (`count`), and numbers (`42`)
- **THEN** the editor renders each token span with its assigned color and places the caret at the exact character column corresponding to the plain-text index

### Requirement: Virtualized Line Viewport Rendering
`SolimCodeEditor` SHALL calculate the visible line range from its scroll offset and viewport height and render only visible lines during `draw()`.

#### Scenario: Large document renders only visible lines
- **WHEN** `SolimCodeEditor` contains 1,000 lines of code and the viewport height displays 25 lines
- **THEN** only the visible line slice is tokenized and drawn during a render frame

### Requirement: Interactive Gutter for Line Numbers, Diagnostics, and Breakpoints
`SolimCodeEditor` SHALL render a left gutter displaying 1-based line numbers, diagnostic severity markers (error/warning icons or highlights) bound via a `Readable`, and clickable breakpoint indicators bound via a `Signal<IntSet>` or callback.

#### Scenario: Clicking gutter toggles line breakpoint
- **WHEN** the user clicks the gutter area next to line 12
- **THEN** line 12 is added to (or removed from) the active breakpoint set and a breakpoint indicator is rendered on line 12

#### Scenario: Diagnostic marker and active execution line highlight
- **WHEN** a diagnostic error is reported on line 5 and the active execution line signal is set to line 8
- **THEN** line 5 displays an error indicator in the gutter/line background and line 8 renders with an active execution highlight

### Requirement: Code Editing Ergonomics and Undo/Redo History
`SolimCodeEditor` SHALL support multi-line cursor navigation, text range selection, copy/cut/paste, automatic indentation on `Enter` after `{`, `Tab` indentation, and an undo/redo history stack (`Ctrl+Z` / `Ctrl+Y` or programmatic `undo()` / `redo()`).

#### Scenario: Auto-indentation on newline after opening brace
- **WHEN** the caret is at the end of `"if (active) {"` and the user presses `Enter`
- **THEN** a new line is inserted with increased indentation matching the block depth

#### Scenario: Undo and redo restore document state
- **WHEN** the user modifies text in `SolimCodeEditor`, invokes `undo()`, and then invokes `redo()`
- **THEN** `undo()` restores the previous plain-text value in the bound `Signal<String>` and `redo()` reapplies the change
