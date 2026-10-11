## Context

The MindustryTool mod features over 20 modular feature settings dialogs built with the declarative Solim UI framework. While Solim provides reactive UI primitives (`column`, `row`, `card`, `slider`, `badge`, `WebStyles`), existing settings views were implemented at different times with divergent layout choices:
- Vertical alignment: Dialog bodies often default to `center()`, causing dialogs with few items to float vertically rather than anchoring cleanly to the top.
- Spacing: Arbitrary gap values (`unit(2)`, `unit(2.5f)`) are scattered without a coherent hierarchy.
- Section styling: Some views use raw `column`s with dividers, while others use inconsistent card borders and paddings.
- Sliders: Many views render sliders inline with labels and place slider value indicators in pill badges with solid backgrounds (`badge(...).color(Colors.SECONDARY)`), clashing with modern design conventions.

This design unifies all setting views around an exponential spacing scale, standardized card structure, top-aligned dialog viewports, clean background-less slider readouts, and a reusable `SettingScaffold`/`SettingCard`/`SettingSlider` component set.

## Goals / Non-Goals

**Goals:**
- Unify dialog positioning: top-aligned on Y (`top()`), centered on X (`center()`), width-constrained (`maxWidth(500f)`).
- Establish an exponential spacing scale: `unit(1.5f)` micro-gap, `unit(3)` intra-card padding & gap, `unit(6)` inter-card gap.
- Standardize the Header Card: line 1 has `[Pal.accent]` icon + feature name + right-aligned category badge; line 2 has muted feature description.
- Standardize Section Cards: line 1 has `[Pal.accent]` section icon + section name.
- Standardize Sliders: header row with title and background-less value readout strictly placed *above* the full-width slider track.
- Enforce `WebStyles` token usage consistently across all setting views.
- Provide declarative reusable helpers (`SettingScaffold`, `SettingCard`, `SettingSlider`) in `mindustrytool.components` to eliminate duplication.
- Document and enforce these rules in `AGENTS.md` for all future setting views.

**Non-Goals:**
- Rewriting underlying feature logic or configuration schemas.
- Changing non-setting views (such as in-game HUDs, canvas overlays, or browser tables).
- Altering the core Solim engine (`:solim-core` or `:solim-api`).

## Decisions

### 1. Structural Viewport Architecture: Top-Aligned, Centered Scaffold

```
┌─────────────────────────────────────────────────────────────┐
│ Dialog (maxWidth: 500f, top().center())                     │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Scroll (grow, scrollX: false)                           │ │
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ Column (growX, top, gap: unit(3), padding: unit(2)) │ │ │
│ │ │                                                     │ │ │
│ │ │  ┌─ Card 1: Header (padding: 2.5, gap: 2) ────────┐ │ │ │
│ │ │  │ [Accent Icon] Feature Name        [BADGE]      │ │ │ │
│ │ │  │ Muted description text...                      │ │ │ │
│ │ │  └────────────────────────────────────────────────┘ │ │ │
│ │ │                                                     │ │ │
│ │ │  ┌─ Card 2: Section (padding: 2.5, gap: 2) ───────┐ │ │ │
│ │ │  │ [Accent Icon] Section Title                    │ │ │ │
│ │ │  │ Controls (switches, buttons, sliders...)       │ │ │ │
│ │ │  └────────────────────────────────────────────────┘ │ │ │
│ │ │                                                     │ │ │
│ │ │  ┌─ Card 3: Next Section ─────────────────────────┐ │ │ │
│ │ └─┴─────────────────────────────────────────────────┴─┴─┘ │
└─────────────────────────────────────────────────────────────┘
```

**Rationale:**
- Aligning `top()` in Y prevents awkward vertical floating when dialog content is short.
- Center alignment in X keeps content balanced regardless of screen aspect ratio.
- `maxWidth(500f)` prevents stretched, unreadable lines on wide PC displays.

### 2. Compact Spacing Hierarchy

We adopt a compact spacing hierarchy based on multiples of `BASE_UNIT = 4f`:
- **Micro Spacing (`unit(1)` = 4px)**: Gap between a control's label and its secondary description text; gap between closely paired chips.
- **Card Spacing (`padding(unit(2.5f))` = 10px, `gap(unit(2))` = 8px)**: Internal padding of every card and gap between rows inside a card.
- **Inter-Card Spacing (`unit(3)` = 12px)**: Gap between adjacent cards in the outer view column (`gap(unit(3))`).

### 3. Header Card & Section Card Standard

- **Header Card**:
  - Row 1: `icon(featureIcon).size(unit(5)).color(Pal.accent)`, `text(featureName).color(Pal.accent).growX().left()`, `badge(categoryName).color(WebStyles.Colors.PRIMARY)` (pushed right).
  - Row 2: `text(featureDescription).color(WebStyles.Colors.GHOST_FG).wrap().growX().left()`.
- **Section Cards**:
  - Row 1: `icon(sectionIcon).size(unit(5)).color(Pal.accent)`, `text(sectionTitle).color(Pal.accent).growX().left()`.
  - Body: List of controls separated by `gap(unit(3))` or subtle dividers (`WebStyles.Colors.SECTION_BORDER`).

### 4. Above-Track Sliders with Clean Value Readout

- Sliders must NEVER be placed inline on the same horizontal row as the title.
- Slider layout structure:
  - Header Row (`row().growX().center()`):
    - Title text on the left (`text(title).left().color(Color.white)`).
    - Spacer pushing value to the right (`spacer()`).
    - Value text on the right (`text(valueDisplay).color(WebStyles.Colors.GHOST_FG)` or `Pal.accent`). No `badge()` container, no background color.
  - Optional Description: Muted description text directly under the title if applicable.
  - Slider Track: `slider(...).growX()` occupying full width.

**Alternatives Considered:**
- *Badge with secondary background (`badge(...).color(Colors.SECONDARY)`)*: Adds visual clutter and creates a distracting boxed aesthetic around simple numerical text.
- *Inline slider with small fixed width*: Cramps slider resolution and causes label truncation on narrower windows.

### 5. Reusable Component Primitives in `mindustrytool.components`

To streamline implementation and guarantee adherence across all future settings, we introduce:
- `SettingCard`: Helper creating a standard `card().growX().padding(unit(3)).gap(unit(3)).rounded(unit(2), WebStyles.Colors.SECTION_BG).border(1f, WebStyles.Colors.SECTION_BORDER)`.
- `SettingCard.header(...)`: Pre-composed feature header card.
- `SettingCard.section(...)`: Pre-composed section header card with accent icon and title.
- `SettingSlider`: Pre-composed slider layout accepting title, formatted value signal, and slider parameters.
- `SettingScaffold`: Pre-composed root wrapper managing `top().center()`, scroll, padding, and `unit(6)` card gap.

### 6. Project Guidelines Update (`AGENTS.md`)

Add an explicit "Setting Dialogs & Views Specification" section in `AGENTS.md` covering:
- Layout hierarchy (Scaffold, Top Alignment, Center X, Max Width 500f)
- Spacing rules (Exponential scale: 1.5 micro / 3 card / 6 outer)
- Header card format
- Section card format
- Slider format (above track, background-less value)
- WebStyles requirement

## Risks / Trade-offs

- **[Risk] High view count (20+ setting views to migrate)** → **Mitigation**: Reusable `SettingCard` and `SettingSlider` helpers reduce migration of each view to concise declarative blocks.
- **[Risk] Layout regression on mobile/narrow viewports** → **Mitigation**: `maxWidth(500f)` combined with full-width sliders and scrollable containers ensures responsive adaptation across mobile and desktop.
- **[Risk] Custom views with complex tables (e.g. RangeDisplay, Visualizer, QuickSchematicGrid)** → **Mitigation**: Reusable components provide layout wrappers while allowing nested custom child components within standard section cards.
