## Context

In Solim UI, components like Row, Column, Card, and Container configure their visuals through fluent modifiers (.rounded(), .border(), .background()). Currently, Arc's underlying Table element stores only a single Drawable reference in its ackground field. Calling .background(drawable) replaces 	able.getBackground(), erasing any previously configured RoundedDrawable. Conversely, calling .rounded() or .border() creates a fresh RoundedDrawable that erases any pre-existing background Drawable (such as Styles.black6).

Furthermore, when developers request a large corner radius (e.g. ounded(unit(10)) = 40px) on small elements (e.g. height 36px), Arc's NinePatch coordinate calculation generates negative middle slice heights, causing inverted lines and overlapping visual artifacts.

## Goals / Non-Goals

**Goals:**
- Make .background(...), .rounded(...), and .border(...) order-independent across all Solim containers and components.
- Support layering an external Drawable (e.g. Styles.black6) with a rounded border outline in a single composite RoundedDrawable.
- Provide native .background(Color) and .background(Readable<Color>) overloads on LayoutModifiers and ElementModifiers.
- Automatically clamp rendered corner radius in RoundedDrawable so that adius <= min(width, height) / 2, eliminating NinePatch mathematical distortions.
- Ensure 0 per-frame heap allocations during draw(), preserving 60 FPS performance and zero GC pressure.

**Non-Goals:**
- Creating a complex full-fledged multi-pass CSS canvas renderer or separate canvas layers.
- Changing Arc's internal NinePatch class itself.

## Decisions

### Decision 1: Embed aseDrawable directly inside RoundedDrawable
Rather than creating an external wrapper class (LayeredDrawable), RoundedDrawable will own an optional @Nullable Drawable baseDrawable.
- **Rationale**: RoundedDrawable is already the unified rendering hub for Solim rounded corners and borders. Adding aseDrawable allows it to draw the base texture first and the rounded border on top in one unified pass.
- **Alternatives considered**: Separate LayeredDrawable(Drawable... layers). Rejected because it introduces additional object wrapping, extra runtime casts, and complicates reactive color/stroke binding on the active border.

### Decision 2: Smart wrapping in ElementModifiers
When ElementModifiers.background(element, drawable) is called:
- If element.getBackground() is already a RoundedDrawable, set oundedDrawable.baseDrawable(drawable).
- Otherwise, set 	able.setBackground(drawable).
When ElementModifiers.border(...) or ounded(...) is called:
- If element.getBackground() is already a RoundedDrawable, update its border/radius.
- If it is another Drawable, wrap that drawable as d.baseDrawable(existing) and assign d as the new background.
- **Rationale**: Seamlessly supports any invocation order (g -> border -> round, order -> bg -> round, etc.) with zero mental friction for developers.

### Decision 3: High-performance radius safety clamping
In RoundedDrawable.draw(...), compute effective radius dynamically:
- Cache lastWidth and lastHeight. If the element dimensions haven't changed, reuse the already prepared orderPatch with zero map lookup or allocation overhead.
- When dimensions change, if effectiveRadius > min(width, height) / 2, clamp effectiveRadius = (int) (min(width, height) / 2) to guarantee NinePatch middle dimensions never become negative.

## Risks / Trade-offs

- **[Risk] Multiple calls to .background() with different Drawables** → Mitigation: Updating aseDrawable replaces the existing base drawable while preserving the outer border stroke and radius.
- **[Risk] Draw call batch switching** → Mitigation: If aseDrawable uses Mindustry's UI texture atlas and orderPatch uses the procedural rounded texture, it issues 2 batched quads. This is optimal and already minimal for two distinct textures.
