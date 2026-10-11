## ADDED Requirements

### Requirement: Unified alignment direction and bitmask composition
All Solim layout containers (`Column`, `Row`, `Card`, `Grid`, `Wrap`, `SolimCollapser`, `Scroll`), reactive containers (`When`, `Dynamic`, `ForEach`, `QueryView`), interactive controls (`Button`, `Checkbox`, `Switch`, `SolimSelect`, `SolimSlider`, `SolimTextField`), and display primitives (`Text`, `SolimImage`, `NetworkImage`) SHALL support directional alignment modifiers `top()`, `bottom()`, `left()`, `right()`, `center()`, and their compositions (`top().left()`, `top().right()`, `bottom().left()`, `bottom().right()`, `top().center()`, `bottom().center()`, `left().center()`, `right().center()`). Alignment modifiers SHALL configure the component's parent cell constraints (`PendingCellConfig.align`) and, for container components, configure inner container alignment and child cell defaults. When opposite directional alignments are applied sequentially (e.g. `top()` then `bottom()`, or `left()` then `right()`), the latter alignment SHALL override the opposing direction without bit corruption.

#### Scenario: Alignment composition on container
- **WHEN** `column().top().center()` or `column().center().top()` is configured
- **THEN** the column's parent cell alignment and table default alignment evaluate to `(Align.top | Align.center)`

#### Scenario: Alignment override replaces opposing direction
- **WHEN** a component invokes `top()` and subsequently invokes `bottom()`
- **THEN** the resulting alignment bitmask contains `Align.bottom` and does NOT contain `Align.top`

### Requirement: Independent coexistence of gap, margin, and padding
Solim components SHALL maintain clean separation between inner container padding (`padding()` / `paddingX()` / `paddingY()`), outer parent cell margins (`margin()` / `marginX()` / `marginY()`), and directional sibling gap (`gap()`):
1. `padding()` SHALL apply strictly to the component's internal boundary (via `Table.margin()` or widget insets) and SHALL NOT mutate parent `Cell` padding.
2. `margin()` SHALL apply strictly to the outer boundary between the element and its parent cell (`Cell.pad()`).
3. `gap()` SHALL apply strictly along the container's primary layout axis between adjacent visible siblings without compounding outer container padding.
4. When `margin()` and `gap()` coincide on a child in a container, the effective parent cell padding along the primary axis SHALL equal `margin + gapOffset` (where `gapOffset == 0` for the first visible child and `gapOffset == gap` for subsequent visible children).

#### Scenario: Gap and margin composition in Column
- **WHEN** a `column().gap(12f)` contains two children each configured with `marginTop(4f)` and `marginLeft(6f)`
- **THEN** the first child cell receives `padTop(4f)` and `padLeft(6f)`, and the second child cell receives `padTop(16f)` (4px margin + 12px gap) and `padLeft(6f)`

#### Scenario: Padding does not alter parent cell margin
- **WHEN** a `card().padding(10f).margin(5f)` is placed inside a layout container
- **THEN** the card's inner table reports 10px margin, and the card's parent cell reports 5px pad

### Requirement: Layout geometry and packing verification
When components with composed alignment and spacing are laid out in a parent table with explicit dimensions and `pack()` or `layout()` is invoked:
1. Child elements SHALL be positioned at exact coordinates `(x, y)` matching their combined alignment and spacing insets within a tolerance of 0.01px.
2. Centered elements SHALL be placed at `Math.round((parentAvailableWidth - childWidth) / 2)` or exact float center offset.
3. Elements with `top().left()`, `bottom().right()`, and intermediate compositions SHALL be bounded accurately within container margins and insets.

#### Scenario: Centered element layout coordinates
- **WHEN** a container of size 200x200 contains a 50x50 element with `center()`
- **THEN** after `pack()`, the element's bounds evaluate to x = 75, y = 75, width = 50, height = 50

### Requirement: Reactive spacing and alignment updates
Components configured with reactive spacing or alignment signals (`Signal<Float>`) SHALL dynamically recalculate parent cell padding and layout geometry upon `SignalDispatcher.flush()`. When a component is disposed, all reactive subscriptions and layout effects SHALL be cleanly disposed.

#### Scenario: Dynamic margin signal update
- **WHEN** a child element is configured with `marginX(signal)` where `signal` changes from 10f to 20f
- **THEN** after `SignalDispatcher.flush()`, the parent cell's `padLeft` and `padRight` update to 20f and table hierarchy is invalidated
