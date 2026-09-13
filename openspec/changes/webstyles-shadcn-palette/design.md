## Context

With the introduction of the `solim.style` subsystem, Solim now has support for fluent button style building, layout properties, and flyweight caching. However, `WebStyles.java` in the `mod` module still uses legacy patterns:
1. One-time intermediate variables (`int radius = unit(2); float stroke = 1.5f;`).
2. Only a single style is exposed (`webButton` / `webTextButton`), which uses a translucent wash and a 1.5px blue border.
3. No semantic color palette exists for other states (such as destructive/danger actions, secondary dark slate actions, or ghost/icon actions).
4. Button styles do not define default padding, requiring widgets to set padding manually or rely on Arc defaults.

Instead of keeping deprecated aliases, this change replaces all legacy names with a clean, unified standard across `WebStyles` and all consuming browser components.

## Goals / Non-Goals

**Goals:**
- Create a shadcn/ui-inspired semantic color palette inside `WebStyles.Colors`.
- Eliminate one-time intermediate variable definitions in `WebStyles` by directly inlining dimensional values (`unit(2)`, `1.5f`).
- Implement 5 shared button variants in `WebStyles`: `primary()`, `secondary()`, `outline()`, `ghost()`, and `danger()`, alongside matching `*Text()` variants.
- Add consistent built-in padding (`unit(2)`) across all shared button variants.
- Ensure `solim.input.Button` supports `.style(SolimButtonStyle)` so callers can pass rich style definitions directly.
- Fully migrate all 6 consuming browser files directly to the new standard methods (`outlineText()`, etc.), eliminating legacy aliases completely.

**Non-Goals:**
- Mutating Arc global styles or Mindustry's `Styles` class.
- Retaining dead/deprecated legacy fields (`CHANNEL_BLUE`, `webButton`, `webTextButton`).

## Decisions

### Decision 1: Semantic Color Sheet as Nested Class (`WebStyles.Colors`)
- *Rationale*: A nested static class `WebStyles.Colors` keeps all color tokens cleanly grouped and easily importable via `import mindustrytool.features.browser.common.WebStyles.Colors;`.
- *Palette Mapping*:
  - `PRIMARY`: Solid `CHANNEL_BLUE` `(0.45f, 0.35f, 0.90f, 1.0f)`.
  - `PRIMARY_FG`: `Color.white`.
  - `PRIMARY_BG`: Translucent wash `(0.45f, 0.35f, 0.90f, 0.15f)` used for outline buttons and active hover washes.
  - `SECONDARY`: Dark slate/indigo `(0.20f, 0.20f, 0.28f, 0.70f)` with light text `(0.90f, 0.90f, 0.95f, 1.0f)`.
  - `GHOST_HOVER`: `(1.0f, 1.0f, 1.0f, 0.10f)` with muted text `(0.75f, 0.75f, 0.82f, 1.0f)`.
  - `DANGER`: Crimson red `(0.85f, 0.25f, 0.25f, 1.0f)` with white text.
  - `BORDER`: Subtle outline border `(0.35f, 0.35f, 0.45f, 0.40f)`.
  - `DISABLED_*`: Dark muted background and borders.

### Decision 2: 5 Shared Button Variants with Text Counterparts
- *Rationale*: Standardizing on the 5 canonical shadcn/ui variants covers 99% of button requirements:
  - `primary()` / `primaryText()`: High-visibility call to action (solid blue fill, white text).
  - `secondary()` / `secondaryText()`: Subtle dark slate background for secondary actions.
  - `outline()` / `outlineText()`: 1.5px blue border + translucent blue wash (exact equivalent to existing `webButton`).
  - `ghost()` / `ghostText()`: Transparent background that highlights on hover (ideal for icon buttons and navigation).
  - `danger()` / `dangerText()`: Vibrant crimson fill for destructive actions.

### Decision 3: Standard Default Button Padding
- *Rationale*: In modern UI, buttons look and feel significantly better when they have internal padding around label/icon content. Adding `.padding(unit(2))` to each shared variant ensures consistent button breathing room without requiring manual `.pad()` on every button callsite.

### Decision 4: Inlining Dimensions & Eliminating Single-Use Variables
- *Rationale*: Declaring `int radius = unit(2); float stroke = 1.5f;` creates temporary noise in the static initializer. Inlining `unit(2)` and `1.5f` directly into `SolimButtonStyleBuilder` chained calls keeps the code concise and idiomatic.

### Decision 5: Clean Migration Without Legacy Aliases
- *Rationale*: With only 6 consuming files in the codebase, maintaining legacy fields (`CHANNEL_BLUE`, `webButton`, `webTextButton`) adds unnecessary tech debt and confusion. Updating consumers to `WebStyles.outlineText()` or `WebStyles.Colors.PRIMARY` establishes a single clean standard.

## Risks / Trade-offs

- **[Risk]** Unmigrated callsites failing to compile if legacy names are removed.
  - **→ Mitigation**: Full codebase search confirmed only 6 files in `mod` reference `WebStyles`. All 6 will be migrated in the same change step, ensuring a completely clean compile and zero dead code.
