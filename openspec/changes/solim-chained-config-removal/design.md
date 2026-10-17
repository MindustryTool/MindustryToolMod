## Context

Three phases have made `props(...)` the typed configuration path. The chained setters remain. `AGENTS.md` describes chained component-specific methods as still valid (`style(...)`, `color(...)`, `text(...)`, `icon(...)`, `onClick(...)`, `checked(...)`, `enabled(...)`, `name(...)`, `gap(...)`), and `solim-ui-dx` mandates that containers declare layout exclusively through props while `Button` keeps fluent content. So the boundary between "prop" and "fluent" must be stated, not assumed, and then applied.

The boundary agreed for this sequence:

```
Configuration (→ prop)                          Structure / Behaviour / Access (→ fluent)
──────────────────────────────────────────────  ─────────────────────────────────────────────
rendered or laid out values                     onClick / onLongClick / onChanged / onEnter
  size, spacing, alignment, gap, padding          scrollTo / scrollToTop / focus
  color, background, rounded, border, opacity     mount / pack / keepInScreen / touchable
  width/height/pref/min/max/grow/margin           toFrontOnTouch / draggable mutation
  visible, enabled, disabled, tooltip             validator / filter / onReach* callbacks
state / config values                           children(...) / items(...) / tab(...) / layer(...)
  checked, selected, value, activeTab            show() / hide() / hideOnZero? (see below)
  progress, collapsed, expanded, muted            accessors: text(), checked(), columns(), value()
  volume, loop, controls, autoplay
  duration, animated, scrollingDisabled
  scrollX, scrollY, pinnedEdge, headerGap
  placeholder, maxLength, fallback, url
  style (Phase 3 typed), icons, textColor
content passed to the constructor               content appended as a child at call time
  new Text("hi"), new Badge("7")                  button().text("Save").icon(add)
  Checkbox(label, signal)
```

`Button.text(...)`/`Button.icon(...)` are deliberately in the fluent column: they append a child, which is structure, and `solim-ui-dx` mandates them. `Text.text(String)` is a setter and is removed; `Values.text(...)` already exists and covers it.

`values` in the table are the props that already exist; the state/config rows are the props this change adds.

## Goals / Non-Goals

**Goals:**

- One configuration path: `.props(...)`. A chained configuration setter is a compile error when the phase completes.
- Preserve every fluent capability that is not configuration: content, behaviour, structure, accessors.
- Keep the migration compiler-enforced and reviewable package by package.
- Reconcile the specs that mandate the removed setters.

**Non-Goals:**

- Reconciler configuration props (`columns`, `key`, `items`, `overscan`, `empty`) — they stay fluent.
- New style value objects.
- Changing rendered behaviour; this change is surface removal plus the prop factories that replace it.

## Decisions

### 1. The classification above is the boundary

A method is configuration and becomes a prop if its only effect is to set a value the component renders, lays out, or exposes as state. It stays fluent if it appends structure, registers behaviour, performs an imperative action, or reads state back.

- *Alternative — "layout and styling only"*: leaves `collapsed`, `muted`, `controls`, `autoplay`, `duration` chained while `gap` is a prop, so a host has two configuration styles. Rejected; the decision was to include state/config.
- *Alternative — "everything"*: pulls `onClick`, `items`, `show`, `mount` into the props pipeline, contradicting the decision to keep structure/behaviour fluent. Rejected.

### 2. Behaviour and accessors stay fluent

`onClick`, `onLongClick`, `onChanged`, `onEnter`, `validator`, `filter`, `focus`, `scrollTo`, `scrollToTop`, `mount`, `pack`, `keepInScreen`, `touchable`, `toFrontOnTouch`, `items`, `tab`, `layer`, `show`, `hide`, `onReachTop`, `onReachBottom`, `children`, and the getters are untouched. `hideOnZero` is a rendered-visibility flag (it changes what is drawn) and becomes a prop.

- *Alternative — delete behaviour methods too*: the decision keeps structure/behaviour fluent, and a callback is not a value. Rejected.

### 3. State props bind two-way where the user can change them

`collapsed`/`expanded` (`SolimCollapser`), `muted` (`SolimVideo`), `checked` (`Button`/`Checkbox`/`Switch`), `value` (`SolimSlider`), and `selected` (`SolimSelect`) accept a `Signal<T>` and install a `TwoWayBinding`, so a header toggle, mute click, or slider drag writes back to the signal. Pure-display values (`volume`, `loop`, `controls`, `progress`, `scrollX`, `scrollY`) stay `Readable<T>` one-way. `Signal` is a `Readable`, so declaring `collapsed(Signal<Boolean>)` alongside `collapsed(Readable<Boolean>)` resolves to the two-way form for a `Signal` argument.

- *Alternative — one-way everywhere*: `SolimCollapser.collapsed(Readable)` already desyncs when its header is toggled; making that the model would bake the bug in. Rejected.
- *Alternative — a new `TwoWay` wrapper type*: a second currency, and `Signal` already means "writable". Rejected.

### 4. Applied-once config takes plain values

`autoplay` is read only when a media source opens; `duration` and `animated` configure collapse timing; the overlay icons are drawables. These become props that take plain values (resolved once). Applying a reactive source to them is not offered, because the read happens once.

- *Alternative — accept `Readable` and re-apply*: a reactive `autoplay` could never take effect after open. Rejected as misleading.

### 5. `Grid.columns(...)` becomes a prop; reconciler `columns` stays fluent

`Grid` is a static layout container, so `columns` is a layout value like `gap` and moves to `props(columns(3))`. `ReactiveGrid`/`ForEach`/`VirtualList` keep fluent `columns`/`key`/`items`/`overscan` because `solim-ui-dx` mandates their fluent, order-independent, terminal-`children` shape.

- *Alternative — keep `Grid.columns` fluent for symmetry*: inconsistent with `gap` becoming a prop on the same class. Rejected.

### 6. Not every deleted setter has a prop yet; the missing set is written first

The new factories (state/config rows of the boundary table, plus `fillParent`, `visible`/`enabled` where missing) are added before the setters are deprecated, so a migrated call site has a target. The full inventory is produced from the grep of the deleted setters during the first task.

### 7. Staged, compiler-enforced removal

Step 1: add the new prop factories. Step 2: `@Deprecated` every configuration setter with a javadoc pointing at its prop. Step 3: migrate `mod` package by package. Step 4: delete the deprecated setters and run `solim-core` + `mod` builds; the compiler proves no call site remains.

- *Alternative — hard cut*: one enormous diff with no reviewable checkpoint and no way to bisect a regression. Rejected.
- *Alternative — keep deprecated setters permanently*: two configuration paths forever, which is the defect. Rejected.

### 8. Docs and the AI-facing guidance are updated

`AGENTS.md`'s "Layout Modifiers Go Through `.props(...)`" section currently lists the chained methods as still valid; it is amended to the configuration/behaviour boundary, and `docs/solim_internals.md` and the prop jsdoc are updated. The bundle gains no new keys: this change is API surface, not UI text.

## Risks / Trade-offs

- **Migration size.** ~1000 call sites. Mitigated by `@Deprecated` + compiler enforcement, by migrating per package, and by leaving Arc (non-Solim) `.color`/`.style`/`.background` calls alone.
- **Two-way binding semantics.** A two-way state prop writes back to the caller's `Signal`; if the caller passes the same `Signal` to two hosts they fight. This is inherent to shared state and is documented; the one-way `Readable` form remains for read-only consumers.
- **`SolimVideo.muted` currently keeps an internal `mutedState` signal.** The two-way prop must bind the caller's `Signal` to that internal state rather than shadow it, preserving the "one source of truth" rule.
- **`SolimImage.padding` vs `margin`.** `SolimImage.padding(...)` writes its parent cell, which `Values.padding` (a table prop) cannot do for an image; the equivalent is `margin(...)`. The image's chained `padding(...)` is therefore removed and call sites move to `margin(...)`; this is a semantic mapping the migration must apply deliberately, not mechanically.
- **Rendered output must not change.** Every migration task pairs a call-site rewrite with a before/after check of the affected screen.
- **`Grid.columns` churn.** Making it a prop breaks `.columns(3)` on retained references used at runtime; those become a `columns` prop change or a retained-signal re-render. Audit the retained-reference call sites before landing.

## Migration Plan

1. Inventory every chained configuration setter (the classification table) and add the missing prop factories.
2. `@Deprecated` the setters with a javadoc linking to the prop.
3. Migrate `mod` package by package (settings, chat, browser, schematic grid, features, HUD, overlays, …), leaving Arc calls alone.
4. Delete the deprecated setters; build `solim-core` and `mod`; fix the stragglers the compiler finds.
5. Amend `solim-config-dsl`, `solim-ui-dx`, `solim-widgets`, `solim-styling`, `solim-video`, and `AGENTS.md`; update `docs/`.

Rollback: revert the change directory and its diff. Phases 1–3 remain; the deprecated setters are restored by the revert.

## Open Questions

None.
