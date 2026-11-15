## Why

When defending against incoming enemy waves in Mindustry, players need a quick assessment of threat magnitude across upcoming waves without manually estimating the sum of unit healths. Furthermore, when lookahead depth is greater than 1, the preview panel occupies substantial vertical screen estate on the HUD, which can obstruct gameplay. Allowing players to view the total HP of each wave inline and toggle between the full lookahead view and a compact 1-wave view by clicking the panel solves both usability and spatial challenges.

## What Changes

- **Total Wave HP Calculation & Display**: Compute total health per wave during wave calculation ($\sum \text{unit.amount} \times \text{unit.type.health}$) and display it formatted inline within the wave title header (e.g., `Wave 15 (45.2k HP)` or `15 (45.2k HP)`), omitting HP when the wave is empty.
- **Click to Toggle Collapse**: Make the entire Wave Preview panel clickable. When lookahead depth is greater than 1, clicking the panel toggles between collapsed mode (showing only the immediate upcoming wave) and expanded mode (showing all lookahead waves).
- **Depth 1 Guard**: When the configured lookahead depth is 1, collapse toggling is disabled as the panel is already at minimum wave count.
- **Transient Session State**: The collapsed state is managed in-memory per session and resets to expanded (showing all configured waves) on world load or map transitions.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `wave-preview`: Adds requirements for precomputing and displaying total wave HP inline with the wave header, and enables clicking anywhere on the preview panel to toggle collapse into a single wave view when depth > 1.

## Impact

- `WaveSectionData`: Added `totalHealth` field computed at creation time.
- `WavePreviewCalculator`: Accumulates unit health times count during wave aggregation.
- `WavePreviewState`: Tracks transient `collapsed` reactive state and reset behavior.
- `WavePreviewPanelView`: Adds inline HP string formatting and click event handler to toggle collapse.
- `assets/bundles/bundle.properties`: Added localized format patterns for wave headers containing HP.
