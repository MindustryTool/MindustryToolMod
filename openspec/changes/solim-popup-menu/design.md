## Context

`mod/.../chat/ChatActionPopup` (~240 lines) implements message actions (Copy/Reply/Translate) as a floating menu by abusing `SolimDialog`: it clears the dialog background, disables fill-parent, positions the dialog manually at click stage coordinates, and hand-rolls outside-dismissal via a scene capture listener with re-hit-testing (Arc's `InputEvent` exposes no target actor). Only ~50 lines are chat-specific. The generic machinery — anchor positioning with prefer-above/flip/clamp, tap-outside swallow-dismissal, Back/Esc dismissal, show/hide listener lifecycle — belongs in Solim, where `solim.overlay.Popup` exists as an acknowledged placeholder (Table wrapper with `rounded()`/`border()` chrome, a `popup()` facade, and pinned tests). `SolimDialog` has ~20 call sites, all genuine modals; its modal contract is load-bearing and must not be muddied. There is currently exactly one context-menu consumer (chat); no second use case is known.

## Goals / Non-Goals

**Goals:**
- Give floating context menus a first-class native home in `solim.overlay` by evolving `Popup`.
- Move all non-chat machinery (positioning, dismissal trio, listener lifecycle, scene hosting) out of the mod.
- Keep the public surface minimal and chainable: `popup().children(provider).rounded(...).show(data, x, y) / hide()`.
- Keep everything headless-testable: pure clamp math, scene-null no-ops, provider rebuild observable without a stage.
- Preserve existing chat behavior exactly (same menu, same actions, same dismissal feel).

**Non-Goals:**
- No second consumer design: no icons/submenus/disabled item states, no `showNear(element)` sugar — one generic provider row model, extension seams left open.
- No menu animations (Dialog show/hide actions are lost with the custom element; acceptable).
- No modal/dim variant — non-blocking floating is the whole point; modals stay on `SolimDialog`.
- No changes to chat spec-level behavior; the `chat-message-group-layout` scenarios must keep passing untouched.

## Decisions

### Decision 1: Evolve `Popup`, not a new `ContextMenu`, not a dialog float-mode
A new `ContextMenu` would leave two overlapping overlay concepts with an invented boundary; a `SolimDialog` float-mode would muddy a contract 20 call sites rely on. `Popup` is already the reserved name, its `rounded()`/`border()` already paint menu-suitable chrome (`rounded()` returns a `RoundedDrawable`), and the placeholder status makes breaking its facade legitimate.

### Decision 2: Custom scene-hosted element, no dialog reuse
Wrapping `BaseDialog` drags in title chrome, modal semantics, and show/hide action machinery the menu fights against (the current code's `setBackground(null)` + `fillParent(false)` is the smell). A plain `Table` added to the scene root on show and removed on hide is transparent, non-modal, and above list scissor clipping by construction. Cost: Back/Esc and resize handling become manual (see Decisions 4–5) — small, explicit, owned.

### Decision 3: Data lives inside, `show(data, x, y)` is atomic — rendered imperatively
Alternatives were a caller-owned signal (`shared.set(req); menu.showAt(x, y)`) or a Solim-internal singleton. Caller-owned splits one user gesture across two calls; a Solim singleton bakes global state into shared infra (lifecycle + test-reset burden forever). `Popup<T>` keeps the current data/anchor in plain fields: `show(data, x, y)` rebuilds content through the provider, positions, adds to the scene, and attaches listeners in one pass; `hide()` reverses it. A private-signal-plus-render-effect indirection was considered and rejected as machinery without function — the signal would be written only by `show()` and read only by an effect doing exactly what `show()` does directly. The imperative form additionally removes any build-ordering requirement (`show()` works whether or not the component was hosted yet), while provider content itself stays fully reactive inside (nested `dynamic()`/`text(signal)` bindings work unchanged, following the `SolimDialog.ensureContentBuilt` precedent for building outside `build()`). Re-showing while open hot-swaps content and re-anchors for free.

### Decision 4: Dismissal trio is built-in behavior
Tap-outside (capture listener + `root.hit()` re-test, swallowed — swallowing is stated, not incidental), Back **and** Esc via an explicit scene key listener attached only while open (both codes listed, no reliance on desktop Escape→Back backend mapping), and hide-on-`ResizeEvent` (transient menus must not survive re-layout; replaces Dialog's resize handling). ESC layering falls out correctly: the listener lives only while open, so the first ESC closes the menu and stops, the next reaches whatever is beneath.

### Decision 5: Keep the proven clamp, extract it pure
The current prefer-above-anchor / flip-below-on-overflow / clamp-XY algorithm is smarter than Arc's `keepWithinStage()` (which just shoves inside bounds). Keep it, but extract the arithmetic into a scene-free pure function — the unit-testable seam. `Core.scene == null` guards make show/hide safe headless.

### Decision 6: Shared = one hosted instance + static ref in mod (option i)
Solim stays instance-pure (no statics, freely testable). The mod hosts one `Popup<ChatMessage>` in the HUD children block (zero-footprint spacer root, ambient ownership per `BaseComponent` constructor rules) and exposes it via a static field assigned at the host site; `openActions` calls `menu.show(...)`. Constructor plumbing (HUD → list → group → tests) was rejected as churn for a single-HUD game mod.

### Decision 7: Fluent chaining across the whole surface
Existing `Popup` style already chains (`rounded()`/`border()` return `this`); all new methods (`children(provider)`, `show()`, `hide()`) return `Popup<T>`. Implementation finding: generic inference does **not** propagate through a chain rooted at the bare facade call — `popup().children(req -> ...)` infers `Popup<Object>` and fails to compile. The adopted pattern is direct assignment first (`menu = popup();`, which infers exactly) and chaining from the typed variable; no explicit type witness was needed. Call sites follow this two-statement form.

### Decision 8: Default visible chrome
A bare `Table` is transparent — an invisible-by-default popup is a footgun. The native menu ships a default dark rounded background; caller `rounded()`/`border()` calls override. (Recommendation; veto during review if explicit-only styling is preferred.)

## Risks / Trade-offs

- **[Risk] `popup()` facade break** → The no-arg attach-to-parent facade and its two pinned tests (`OverlayFeedbackTest`, `RoundedGraphicsTest`) are rewritten for the generic scene-hosted behavior. *Mitigation*: placeholder-status API, young codebase, mechanical test updates in the same change.
- **[Risk] Generic inference fallback** → If chained `popup().children(...)` inference fails on the project's compiler, call sites use an explicit witness. *Mitigation*: verified at implementation time; contained to call sites.
- **[Risk] Scroll-under-menu drift** → Stage-coordinate menu misaligns if the chat list scrolls beneath it. *Mitigation*: accepted caveat (transient, short-lived menu); documented, not tasked.
- **[Risk] Menu vs future modals z-order** → Scene-append order puts the menu above the HUD but below later-shown dialogs. *Mitigation*: dismissal is one tap away; acceptable for v1.
- **[Risk] Single-consumer over-abstraction** → Designing framework API for one caller. *Mitigation*: surface kept minimal (provider rows + show/hide + chrome already present); generality comes from the provider function, not added features.

## Migration Plan

1. Evolve `Popup` + facade + native tests (mod untouched, suite green).
2. Slim `ChatActionPopup` to the shell: static instance, `PopupRequest` data, provider rows, translation logic; rewire `openActions` to `menu.show(...)`.
3. Delete the old dialog machinery (transparent `SolimDialog`, manual catcher/clamp in mod).
4. Full suite + in-game eyeball (menu placement, all three dismissal paths, translate flow). Rollback = revert; old and new do not coexist.

## Open Questions

- Default chrome (Decision 8) stands as a recommendation — confirm or veto in review.
- `children(provider)` vs a `content(provider)` alias to avoid overload confusion with `children(Runnable)` — implementation-time naming call.
