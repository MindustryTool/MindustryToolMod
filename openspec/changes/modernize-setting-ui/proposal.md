## Why

Settings dialogs across features in the MindustryTool mod currently exhibit fragmented layout patterns, inconsistent vertical alignment, divergent card paddings and gaps, and conflicting slider representations. Some features render sliders inline with tiny widths, others wrap slider values in pill badges with backgrounds, and several views use raw Arc styles or arbitrary spacing. 

This change modernizes and unifies all setting dialog views across the mod into a cohesive, declarative design system rooted in Solim and `WebStyles`, establishing clear visual hierarchy, exponential spacing standards, and reusable building blocks for all current and future setting UIs.

## What Changes

- **Unified Dialog Alignment & Scaffolding**: All setting dialogs strictly align to the top on the Y-axis and center on the X-axis (`top().center()`), with a constrained max width of `500f`.
- **Card-Based Section Layout**: Every section is rendered inside a distinct card with `padding(unit(2.5f))` and inner `gap(unit(2))`.
- **Compact Spacing System**: Spacing follows a balanced compact scale (`unit(1)` micro-gap, `unit(2.5f)` card padding, `unit(2)` inner card gap, `unit(3)` inter-card gap).
- **Standardized Header Card**: The first section card consistently features:
  - Line 1: `[Pal.accent]` feature icon + feature name, with a category badge aligned to the far right.
  - Line 2: Muted feature description (`WebStyles.Colors.GHOST_FG`), wrapped and left-aligned.
- **Standardized Section Cards**: Every subsequent section card starts with `[Pal.accent]` section icon + section name.
- **Modernized Slider Controls**: Sliders always place their title and value in a header row *above* the slider track (never inline). Slider values are rendered as clean text with no pill background. The slider track grows full-width (`.growX()`).
- **WebStyles First**: Enforce `WebStyles` variants (`primary`, `outline`, `ghost`, `filterChip`, `cardAction`, semantic colors) across all controls, eliminating ad-hoc game button styles and raw colors.
- **Reusable Setting UI Primitives**: Provide reusable declarative builders/components (e.g. `SettingCard`, `SettingSlider`, `SettingScaffold`) in `mindustrytool.components` to eliminate copy-paste boilerplate and guarantee consistency.
- **Future Design Guardrails in AGENTS.md**: Update project guidelines in `AGENTS.md` so all future feature setting views automatically adhere to this layout and style specification.

## Capabilities

### New Capabilities
- `settings-ui-modernization`: Specification for unified settings dialog layout, card hierarchy, exponential spacing, slider layout, WebStyles usage, and reusable setting UI components across the mod.

### Modified Capabilities
<!-- None -->

## Impact

- **Existing Setting Views**: All existing `*SettingsView.java` implementations in `mindustrytool.features.*` will be migrated to the new unified layout and components.
- **UI Architecture**: Addition of reusable setting UI layout primitives in `mindustrytool.components`.
- **Developer Guidelines**: Updated `AGENTS.md` settings dialog conventions to enforce the new layout for all future features.
- **Dependencies**: No external dependency changes; fully built on top of `:solim` and `WebStyles`.
