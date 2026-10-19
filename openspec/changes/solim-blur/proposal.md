## Why

Modern graphical user interfaces rely on depth and layering cues like frosted glass (backdrop blur) and content defocusing to improve visual hierarchy, contrast, and focus. In Mindustry, modal dialogs currently only support a flat semi-transparent dark dimming layer, and floating HUD panels or cards often clash with complex, high-contrast game world terrain, conveyors, and unit sprites underneath.

Introducing native blur capabilities to Solim allows dialogs, floating panels, cards, and HUDs to feature modern glassmorphic styling with clear visual separation from the busy game world, while also enabling content blur for masked or inactive elements.

## What Changes

- Introduce **Backdrop Blur** capability:
  - Enables frosted glass effect behind containers, cards, dialogs, popups, and HUDs while keeping foreground content (text, icons, buttons) completely sharp.
  - Employs incremental screen sampling (batch flush followed by backbuffer sub-rect capture into a downscaled framebuffer) to blur both the game world and any underlying UI layers.
- Introduce **Content Blur** capability:
  - Enables post-processing blur of a component and its children (e.g., for privacy masking, depth-of-field, or disabled states) using offscreen framebuffer capture and Gaussian filter passes.
- Provide declarative Solim props/modifiers:
  - `backdropBlur(float radius)` (and default `backdropBlur()`)
  - `contentBlur(float radius)` (and default `contentBlur()`)
  - Integrated into Solim's modifier system (`Values`, `Props`, and `UI` facades).
- Zero steady-state frame allocations:
  - Framebuffers and filter instances are pre-allocated and pooled; resolution changes are handled lazily without per-frame allocations.
- Graceful low-end & Android fallback:
  - Detects hardware/driver FBO and shader support and respects user settings, cleanly falling back to standard semi-transparent solid backgrounds if blur is unsupported or disabled.

## Capabilities

### New Capabilities
- `solim-blur`: Declarative backdrop blur and content blur modifiers, offscreen downsampled blur pipeline, and hardware fallback handling for Solim UI components and overlays.

### Modified Capabilities
<!-- None -->

## Impact

- `solim-core`: New graphics/rendering helper classes for blur postprocessing, new modifier capabilities in `solim.modifier.Values` and `Props`.
- `solim`: Public facade extensions in `solim.UI`.
- `solim-overlay`: Support in `SolimDialog`, `Popup`, and `Hud` for backdrop blur rendering.
- No breaking changes to existing Solim components or mod features.
