## MODIFIED Requirements

### Requirement: Hud viewport boundary clamping and resize adaptation

The `Hud` container SHALL support a `keepInScreen()` method and automatically register an Arc `ResizeEvent` listener on construction. The displayed position SHALL be derived from the desired position — the bound `x`/`y` signals when present, otherwise the current root coordinates — clamped within $[0, \text{screenWidth} - \text{hudWidth}]$ and $[0, \text{screenHeight} - \text{hudHeight}]$. Viewport-driven clamping SHALL NOT mutate the bound `x`/`y` signals; only user drag and an explicit position reset SHALL change the persisted position. When the viewport grows, the displayed position SHALL be re-derived from the desired position. When the viewport or root is degenerate (viewport width or height at or below `100` units, or a root with no measurable size), clamping SHALL be skipped and the root position SHALL be left unchanged. When scale is applied to the Hud container, `root.pack()` SHALL be called after the scale change so the root cell tracks the container's scaled visual bounds and hit-testing remains accurate.

#### Scenario: Clamping Hud coordinates when positioned outside screen

- **WHEN** a `Hud` has width 200, height 100, and its desired position is $(1200, 900)$ on a $1024 \times 768$ screen
- **THEN** `keepInScreen()` moves the displayed position to $(824, 668)$ while the desired position remains $(1200, 900)$

#### Scenario: Screen resize event keeps Hud within viewport

- **WHEN** a `ResizeEvent` occurs in the game runtime
- **THEN** `Hud` updates its layout, clamps the displayed position within the new viewport dimensions, and leaves the bound `x`/`y` signals unchanged

#### Scenario: Viewport shrink and restore preserves the saved position

- **WHEN** the viewport shrinks so the panel no longer fits and later returns to its previous size
- **THEN** the panel displays clamped while the viewport is small and returns to its saved coordinates when it grows back, without the saved position being rewritten

#### Scenario: Rotation preserves the outgoing context position

- **WHEN** the device rotates while a HUD is positioned near an edge, and later rotates back
- **THEN** the position persisted for the outgoing orientation is unchanged by the first rotation, and the panel returns to it after rotating back

#### Scenario: Degenerate viewport does not reposition the HUD

- **WHEN** the viewport width or height is at or below `100` units (for example a minimized desktop window or a viewport collapsed by the on-screen keyboard)
- **THEN** the HUD root is left at its current position and no position signal is written

#### Scenario: Dragging persists the position for the current context

- **WHEN** the user drags the HUD handle and releases it
- **THEN** the bound `x`/`y` signals receive the clamped displayed position and are persisted under the current context key, while a pure click without movement leaves them unchanged

#### Scenario: Hit region matches visual bounds after scale change

- **WHEN** `hud.scale(0.8f)` is applied
- **THEN** hovering and clicking within the visually rendered area of the Hud triggers correct element hover and click states, with no offset between visual position and hit region
