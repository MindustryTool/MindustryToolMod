## Context

`ProgressDisplayFeature` currently overlays a countdown of remaining production time directly on active factories and crafters. The user requested:
1. Renaming the feature to "Block Stats Display" with no backward-compatibility migration needed.
2. Rendering the block's average efficiency percentage (rounded to 0–100% or up to 300% when boosted) under the progress number.
3. Pre-caching percentage strings to guarantee zero allocations per frame in the draw loop.

## Goals / Non-Goals

**Goals:**
- Clean rename of packages, classes, feature ID, setting keys, and i18n keys from `progress-display` to `blockstats-display`.
- Pre-cache percentage strings `0%` through `300%` in a static array.
- Display a two-line overlay centered on the building: remaining time on top, efficiency percentage below.
- Color the efficiency string based on operating health: `Pal.power`/`Pal.accent` ($\ge 100\%$), `Color.orange` ($> 50\%$), and `Pal.scarlet` ($\le 50\%$).
- Keep rendering overhead allocation-free in the `Trigger.draw` loop.

**Non-Goals:**
- Migrating legacy settings from `mindustrytool.features.progress-display.*` (clean cut).
- Complex multi-point historical efficiency charts or graphs.

## Decisions

### Decision 1: Rename Scope & Namespace
- **Decision:** Rename package to `mindustrytool.features.blockstats`, classes to `BlockStatsDisplayFeature`, `BlockStatsDisplaySettingsDialog`, and `BlockStatsDisplaySettingsView`.
- **Feature ID:** `"blockstats-display"`.
- **Config Prefix:** `"mindustrytool.features.blockstats-display.block."`.
- **Rationale:** Direct, clean naming aligning with the new feature purpose without legacy baggage.

### Decision 2: Zero-Allocation Efficiency Strings
- **Decision:** Initialize a static array `String[] PERCENT_STRINGS = new String[301]` where `PERCENT_STRINGS[i] = i + "%"`.
- **Computation:**
  ```java
  int eff = Mathf.clamp(Math.round(build.efficiency * 100f), 0, 300);
  String effText = PERCENT_STRINGS[eff];
  ```
- **Rationale:** The draw loop runs 60 times per second across dozens of buildings in viewport. Dynamic string formatting (`String.format("%d%%", ...)`) would generate thousands of string allocations per second. Pre-caching completely eliminates GC pressure.

### Decision 3: Text Stacking & Vertical Alignment
- **Decision:** Stack countdown time and efficiency text symmetrically around the building center `(build.x, build.y)`:
  - Time text: `build.y + (3.5f * frameScale)`
  - Efficiency text: `build.y - (3.5f * frameScale)`
- **Rationale:** Keeps the combined block of information centered on the structure rather than shifting off-center.

### Decision 4: Efficiency Color Palette
- **Decision:**
  - $\ge 100\%$: `Pal.accent` / `Pal.power`
  - $> 50\%$ and $< 100\%$: `Color.orange`
  - $\le 50\%$: `Pal.scarlet`
- **Opacity handling:** Modulate alpha by `frameOpacity` using `Tmp.c1` and `Tmp.c2`.

## Risks / Trade-offs

- **[Risk]** Overdrive Projectors or Domes can boost efficiency past 300% in heavily modded or custom maps.
  - **Mitigation:** Clamp percentage index safely to `[0, 300]` before array indexing to prevent `ArrayIndexOutOfBoundsException`.
