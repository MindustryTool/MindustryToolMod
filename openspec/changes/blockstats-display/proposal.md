## Why

The existing "Progress Display" feature only displays remaining countdown time on production buildings. Renaming it to "Block Stats Display" reflects a broader scope for block status overlays. In addition to the remaining progress time, showing the building's average efficiency provides players with immediate insight into factory bottlenecks, overdrive effects, power shortages, and liquid deficits directly on the world map.

## What Changes

- **Rename Feature**: Rename `progress-display` to `blockstats-display` across the codebase, packages, classes, configuration keys, and i18n bundle keys (no migration needed).
- **Efficiency Overlay**: Add efficiency readout (e.g., `100%`) positioned below the countdown progress timer at the center of the building.
- **Efficiency Caching**: Use a pre-cached array of percentage strings (`0%` to `300%`) to achieve zero allocations per frame.
- **Efficiency Color-Coding**: Color the efficiency readout based on operating thresholds (`Pal.accent`/`Pal.power` for $\ge 100\%$, `Color.orange` for $> 50\%$, `Pal.scarlet` for $\le 50\%$).
- **Vertically Centered Two-Line Layout**: Adjust text positioning so the two-line stack (countdown time above, efficiency below) is vertically centered at `(build.x, build.y)`.

## Capabilities

### Modified Capabilities
- `progress-display`: Renamed to `blockstats-display` and requirements updated to include efficiency percentage rendering below the countdown timer with zero-allocation string caching and threshold color-coding.

## Impact

- Mod feature registration in `Main.java` updated to `BlockStatsDisplayFeature`.
- Package renamed from `mindustrytool.features.progressdisplay` to `mindustrytool.features.blockstats`.
- `ProgressDisplayFeature.java`, `ProgressDisplaySettingsDialog.java`, and `ProgressDisplaySettingsView.java` renamed to `BlockStatsDisplayFeature`, `BlockStatsDisplaySettingsDialog`, and `BlockStatsDisplaySettingsView`.
- Config keys updated to `mindustrytool.features.blockstats-display.*`.
- Bundle files updated from `feature.progress-display.*` to `feature.blockstats-display.*`.
