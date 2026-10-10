## Context

Currently, several features expose individual `scaleConfig` options (`AutoplayFeature`, `GodModeFeature`, `HealthBarFeature`, `ProgressDisplayFeature`, `QuickAccessFeature`, `TeamResourceFeature`, `TimeControlFeature`, and `WavePreviewFeature`). Each feature's UI scales independently based strictly on its local config value (defaulting to 1.0f).

On mobile devices, screen real estate and UI proportions differ markedly from desktop monitors. Requiring mobile users to navigate into every single feature setting dialog to scale down UI widgets individually is cumbersome. Providing a global universal scale setting in `GeneralSettingsDialog` allows users to scale all mod overlays and bars at once while preserving fine-tuned relative proportions.

## Goals / Non-Goals

**Goals:**
- Provide a persistent `universalScale` configuration value with sensible defaults: `1.0f` on desktop and `0.8f` on mobile (`Vars.mobile ? 0.8f : 1.0f`).
- Add a slider in `GeneralSettingsView` from `0.5f` to `1.5f` (step `0.05f`) with percentage label display.
- Expose a centralized reactive helper `ModSettings.effectiveScale(Readable<Float> featureScale)` returning a reactive `Computed<Float>`, and a non-reactive helper `ModSettings.effectiveScale(float featureScale)`.
- Connect all existing scaled feature HUD views and in-world indicators to `ModSettings.effectiveScale(...)`.
- Retain backward compatibility and independent local tuning: individual feature settings sliders still control their relative multiplier.

**Non-Goals:**
- Altering the global Mindustry game UI scale (`Core.settings.getInt("uiscale")` or Arc `Scl.scl()`).
- Changing non-scalable dialog layouts (settings dialogs themselves maintain their standard `maxWidth(500f)` constraints).
- Forcing a fixed scale across all features without per-feature overrides.

## Decisions

### 1. `ModSettings.universalScale` Config & Platform Default
- **Decision**: Define `universalScale = GROUP.floatValue("universal-scale", Vars.mobile ? 0.8f : 1.0f)` in `ModSettings`.
- **Rationale**: Keeps mod-wide global preferences centralized in `ModSettings`, automatically respecting the Android/mobile environment at initialization.
- **Alternatives Considered**:
  - Setting default to 1.0f on all platforms: suboptimal for mobile out of the box.
  - Adding a separate scale per device type: unnecessary complexity when a single slider with platform-specific default solves the problem.

### 2. Reactive Combination via `ModSettings.effectiveScale`
- **Decision**: Provide:
  ```java
  public static Computed<Float> effectiveScale(@Nullable Readable<Float> featureScale) {
      return universalScale.signal().combine(
          featureScale != null ? featureScale : Readable.of(1f),
          (u, f) -> (u != null ? u : 1f) * (f != null ? f : 1f)
      );
  }
  public static float effectiveScale(float featureScale) {
      Float u = universalScale.signal().peek();
      return (u != null ? u : 1f) * featureScale;
  }
  ```
- **Rationale**: Ensures Solim's declarative reactive paradigm is preserved. When the universal slider moves, all active HUD elements and overlays react immediately without polling or recreating elements.
- **Alternatives Considered**:
  - Manually computing `universalScale.signal().combine(...)` inside each view: boilerplate and prone to inconsistency.
  - Adding `effectiveScale` into the base `Feature` class: violates cohesion because not all features have scaling or HUD views.

### 3. Scope of Application (HUDs and In-World Bars)
- **Decision**: Apply `effectiveScale` to both screen HUD overlays (`AutoplayHudView`, `GodModeHudView`, `QuickAccessHudView`, `TeamResourceHudView`, `TimeControlHudView`, `WavePreviewPanelView`) and in-world drawn indicators (`HealthBarFeature`, `ProgressDisplayFeature`).
- **Rationale**: User confirmed in exploration that all scaled elements benefit from universal scaling.

### 4. GeneralSettingsView Slider Layout
- **Decision**: Place the universal scale slider in `GeneralSettingsView` right after the checkboxes and before the diagnostic buttons, matching existing Solim slider patterns:
  ```java
  column().growX().gap(unit(1)).children(() -> {
      text(Core.bundle.get("setting.universal-scale")).left();
      row().growX().gap(unit(2)).center().children(() -> {
          slider(ModSettings.universalScale.signal(), 0.5f, 1.5f, 0.05f).growX();
          text(ModSettings.universalScale.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"))
              .width(unit(12)).right();
      });
  });
  ```
- **Rationale**: Clean, consistent with Solim declarative UI guidelines and responsive.

## Risks / Trade-offs

- **[Double Scaling Extreme Values]** If both universal scale and feature scale are set to minimum (0.5 * 0.5 = 0.25) or maximum (1.5 * 1.5 = 2.25), UI elements could become very small or very large.
  - *Mitigation*: The ranges (0.5x – 1.5x) are safe bounds, and users retain direct visual feedback and full control over both sliders.
- **[Reactivity severed by peek() during build]**
  - *Mitigation*: Ensure all HUD views pass the `Readable<Float>` directly into sizing/font bindings rather than reading raw floats during `build()`.
