## Context

Across commits `8db07f88` and `a2c818c1`, Solim's component hierarchy was unified around a single abstract base class `Component`. However, several patterns compromised the clean declarative design:
1. Widgets used eager constructors `super(element)` and had to explicitly call `own(...)` for effects and children, or rely on `ModifierOwnership` fallback.
2. Tracing/profiling nanosecond measurements were embedded directly in `Component.element()` and `AttachmentBridge`.
3. Disposal loops and QueryView caught and logged or swallowed exceptions instead of failing fast.
4. Test-only methods (`listenerCount()`, `observerCount()`, `assertOwned()`) leaked into public classes.
5. Large narrative comments were added detailing what changed and why.

## Goals / Non-Goals

**Goals:**
- Convert core widgets to lazy `build()` execution so that `OwnershipContext` ambiently captures reactive effects, children, and bindings during element creation.
- Delete `ModifierOwnership` and eliminate manual `own(...)` invocations across widget configurations.
- Remove profiling and tracing hooks from `solim-api` (`Component.java` and `AttachmentBridge.java`).
- Enforce strict fail-fast / fail-loud error handling in `Component.dispose()`, `SessionRoot.dispose()`, and `QueryView`.
- Clean up test-only methods, remove unnecessary constructor overloads, and strip narrative comments.

**Non-Goals:**
- Reverting the unification of `Component` (the single base class model is preserved).
- Removing `SessionRoot` (it remains as the automatic process-level fallback in `OwnershipContext.registerChild()`).

## Decisions

### Decision 1: Lazy `build()` for Widgets vs Eager super(element)
- **Rationale**: When a component is constructed lazily (`protected Component()`), its `build()` is executed inside `OwnershipContext.push(this)`. Any `Effect.of(...)`, `Readable.map(...)`, or child component instantiated during build is automatically adopted into `this` without explicit `own()` calls.
- **Alternative Considered**: Keeping eager constructors and pushing `OwnershipContext` inside constructor blocks. This creates brittle lifecycle states where partially constructed instances escape to ambient context before constructors complete.

### Decision 2: Elimination of `ModifierOwnership`
- **Rationale**: `ModifierOwnership` was an ad-hoc fallback for when modifiers were invoked outside of `build()`. Since Solim widgets build lazily and construct reactive bindings in `build()` or fluent modifiers that attach to the component, ambient ownership is guaranteed. Removing `ModifierOwnership` prevents double-ownership hacks and simplifies `ElementConfig`, `CellConfig`, and `TableConfig`.

### Decision 3: Remove Profiling from `solim-api`
- **Rationale**: `Component.element()` should execute with minimal overhead and no knowledge of profiling spans. Tracing/profiling belongs solely in debug tools and test harnesses or override servers. `AttachmentBridge` becomes a clean seam strictly for pending attachment registration.

### Decision 4: Fail Fast in Disposal and Rendering
- **Rationale**: Swallowing or logging errors during disposal masks bugs (e.g. invalid state, leaks, illegal thread access). Unhandled exceptions during `dispose()` and `QueryView` render will propagate immediately, failing loud to ensure early detection during development.

## Risks / Trade-offs

- **[Risk] Existing unit tests checking `listenerCount()` or `observerCount()` break** → **Mitigation**: Update tests to verify observable behavior (state change propagation and side effect execution) rather than private internal counts.
- **[Risk] Throwing in disposal might stop subsequent disposables from running** → **Mitigation**: Per the fail-fast rule, early crashes in development expose underlying bugs faster than silently trying to limp along.
