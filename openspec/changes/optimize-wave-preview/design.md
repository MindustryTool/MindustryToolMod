## Context

`WavePreviewFeature` provides tactical HUD information about upcoming wave enemy compositions. Currently, it displays the wave number and domain rows (Ground, Air, Naval) with unit icons and counts. When a player configures a lookahead depth greater than 1 (up to 10 waves), the panel stacks multiple wave sections vertically, which can occupy significant HUD space during active combat. Furthermore, assessing total wave threat currently requires players to mentally tally unit counts and health values.

## Goals / Non-Goals

**Goals:**
- Precompute total wave health in `WaveSectionData` during wave computation so that per-frame rendering allocates zero memory.
- Display total wave HP inline with the wave header label (e.g. `Wave 15 (45.2k HP)` or `15 (45.2k HP)` for single-wave mode).
- Make the entire wave preview panel clickable to toggle collapse: when expanded, it displays all lookahead waves; when collapsed, it displays only the immediate upcoming wave (index 0).
- Disable toggle action when configured lookahead depth is 1.
- Maintain transient in-memory collapse state that resets to expanded on world load or feature disable.

**Non-Goals:**
- Persisting collapsed state across game restarts (transient in-memory was chosen).
- Providing domain-specific HP breakdowns (Ground/Air/Naval individual HP tallies).
- Modifying enemy spawn logic, difficulty multiplier calculations, or dropzone resolution.

## Decisions

### Decision 1: Precompute `totalHealth` in `WaveSectionData`
- **Choice**: Store `public final float totalHealth` directly on `WaveSectionData`, accumulated during `WavePreviewCalculator.computeWave(...)` by summing `entry.amount * entry.type.health` for each unit entry.
- **Rationale**: Keeps rendering zero-allocation. Wave calculation happens strictly on wave events or depth changes, so no heap allocation occurs on per-frame updates.
- **Alternatives Considered**: Calculating on the fly in `WavePreviewPanelView` (violates zero-allocation frame loop rule if string formatting or iteration occurs repeatedly without caching).

### Decision 2: Compact Number Formatting for HP
- **Choice**: Format health values compactly:
  - `< 1,000`: integer representation (e.g., `520`)
  - `1,000` to `999,999`: one decimal place with `k` suffix (e.g., `45.2k`)
  - `≥ 1,000,000`: one decimal place with `M` suffix (e.g., `1.8M`)
- **Rationale**: Keeps the header compact and prevents horizontal overflow on narrow HUD layouts.
- **Alternatives Considered**: Raw unformatted integer strings (too long for late-game waves with millions of HP).

### Decision 3: Localized Header Display with i18n Bundles
- **Choice**: Introduce localized bundle keys:
  - `feature.wave-preview.section.wave-hp=Wave {0} ({1} HP)`
  - `feature.wave-preview.section.single-hp={0} ({1} HP)`
  If the wave is empty or has 0 health, fall back to standard wave number display (`Wave {0}` or `{0}`).
- **Rationale**: Complies with mandatory i18n rules while neatly integrating HP inline with the wave number.

### Decision 4: Transient `collapsed` State in `WavePreviewState`
- **Choice**: Manage a `Signal<Boolean> collapsed = Signal.of(false);` within `WavePreviewState`, exposed via `Readable<Boolean> collapsed()` and `toggleCollapsed()`.
- **Rationale**: Single reactive source of truth. When `clear()` is called (e.g. On game exit or world load), `collapsed` resets to `false`.

### Decision 5: Entire Panel Clickable with Depth Guard
- **Choice**: Attach `.clicked(() -> feature.getState().toggleCollapsed())` to the root container of `WavePreviewPanelView`.
- **Rationale**: In `WavePreviewPanelView`, check whether `depth > 1`. If `depth == 1` or `waves.size <= 1`, `toggleCollapsed()` performs no action. When `collapsed.get()` is true, only `waves.first()` is rendered.

## Risks / Trade-offs

- **[Risk: Accidental clicks during combat]** → Mitigation: Toggling merely collapses/expands the lookahead preview; it does not trigger any game actions or modal dialogs. A single tap/click immediately restores full view.
- **[Risk: Long wave titles causing text wrap]** → Mitigation: Compact number formatting (`45.2k HP`) keeps the total title string short and readable even with scaled fonts.
