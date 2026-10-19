## Context

Solim provides a declarative, reactive UI system for Mindustry mods. While Solim supports rich styling (rounded corners, borders, badges, buttons, shadows), it currently lacks modern depth and layering primitives such as backdrop blur (frosted glass) and element post-processing blur (content blur).

In Mindustry, the game scene is rendered in an OpenGL context managed by Arc. Arc includes the `arc.fx` package, which provides `FxProcessor`, `PingPongBuffer`, and `GaussianBlurFilter`. However, `arc.fx.util.FxWidgetGroup` is designed only for filtering child elements into an offscreen framebuffer before blitting them, not for sampling the background behind an element to produce frosted glass.

Furthermore, Mindustry modding runs across diverse environments, including desktop PC and Android devices with varying OpenGL ES capabilities. Frame-loop allocations and high fill-rate shader costs must be strictly managed according to the project's performance guidelines.

## Goals / Non-Goals

**Goals:**
- Provide a declarative `backdropBlur(float radius)` modifier in Solim to render frosted glass behind containers, cards, dialogs, popups, and HUDs while keeping child content crisp.
- Provide a declarative `contentBlur(float radius)` modifier in Solim to post-process an element and its children (e.g. for privacy masking or disabled views).
- Implement an incremental screen-capture and downsampled multi-pass blur pipeline that avoids full-resolution fill-rate penalties.
- Guarantee zero heap allocation during steady-state frame rendering.
- Provide automatic graceful fallback to solid semi-transparent tinting when shaders/FBOs are unsupported by the driver or disabled by the user.

**Non-Goals:**
- Reactive blur radius animation (`Readable<Float>`): Kept as a static float at build time to eliminate reactivity and subscription overhead in frame loops.
- Arbitrary CSS-like filter chains (sepia, invert, hue-rotate): Only blur is in scope.
- Direct CPU-side pixel manipulation or readbacks (`glReadPixels` is forbidden due to GPU stall).

## Decisions

### 1. Universal Modifiers in `solim.modifier` and `solim.UI`
- **Decision**: Expose `backdropBlur(float radius)` and `contentBlur(float radius)` as standard element modifiers, supported across tables, containers, and dialogs.
- **Rationale**: Keeps Solim API declarative and consistent. Any existing container or card can become frosted glass simply by adding `.props(backdropBlur(8f))`.
- **Alternatives Considered**:
  - Dedicated components like `glassCard(...)`: Rejected because it duplicates layout functionality and forces developers to switch component types instead of applying a style modifier.

### 2. Incremental Backbuffer Sampling for Backdrop Blur
- **Decision**: When drawing an element with `backdropBlur`:
  1. Flush the current `Draw.spriteBatch` to ensure all underlying draw commands (game world and preceding UI elements) are committed to the screen buffer.
  2. Use `glCopyTexSubImage2D` into a downsampled offscreen texture (or render to an intermediate FBO) for the element's bounding rect.
  3. Execute 2-pass Gaussian blur (horizontal then vertical) using a pooled `PingPongBuffer`.
  4. Render the blurred texture quad over the element's bounds, modulated with any background tint.
  5. Render the element's children normally on top.
- **Rationale**: Allows proper frosted glass over both the game world and underlying UI elements (e.g., a modal dialog over a HUD, or a popup over a card).
- **Alternatives Considered**:
  - Single pre-UI screen capture: Faster, but cannot blur UI layers beneath other UI layers.

### 3. Downsampling (2x / 4x) for Performance & Mobile Friendliness
- **Decision**: Sample the screen region at 1/2 or 1/4 resolution before running the blur shader passes.
- **Rationale**: Blurring at full screen resolution scales quadratically with pixel count. Downsampling drastically cuts fragment shader cost and texture bandwidth, while visually producing a smoother, more cinematic blur with fewer passes.
- **Alternatives Considered**:
  - Full-resolution Gaussian blur: Causes severe fill-rate bottlenecks on integrated GPUs and Android.

### 4. Zero-Allocation Frame Loop & Pooled PingPong Buffers
- **Decision**: Centralize all blur framebuffers in a singleton `BlurManager` / `BlurRenderer` in `solim-core/src/solim/graphics/`. Buffers are lazily created and resized on display resize.
- **Rationale**: Strictly adheres to the project's zero-allocation frame loop rule. Prevents GC pauses in Mindustry.

### 5. Graceful Fallback Strategy
- **Decision**: Check `Gl.checkFramebufferStatus` and device GL capabilities on startup. If unavailable, or if the user disables blur in settings, `backdropBlur` simply paints the element's background color with standard alpha blending.
- **Rationale**: Prevents crashes and black screens on older Android devices or buggy GL ES 2.0 implementations.

## Risks / Trade-offs

- **[Risk] Multiple overlapping blurred elements could cause multiple batch flushes and texture copies per frame.**
  → *Mitigation*: Limit the blur region strictly to the scissor/clipping bounds of the element; downsample before copying; document recommended usage (primarily on modals, popups, and top-level cards).
- **[Risk] GL texture coordinate upside-down orientation between OpenGL FBOs and Arc UI coordinate space.**
  → *Mitigation*: Properly flip the V coordinate when blitting the blurred texture back to the Arc batch.
- **[Risk] ScissorStack / clipping interaction with blurred cards.**
  → *Mitigation*: Respect `ScissorStack.peek()` during screen sub-rect sampling so blur never bleeds outside parent scroll panes.
