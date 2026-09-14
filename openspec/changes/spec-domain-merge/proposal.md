## Why

`openspec/specs/` holds ~70 fragmented capabilities because almost every archived change created one new spec directory. Overlapping specs (ownership ×5, modifiers ×4, reactivity ×6) duplicate requirements, 17 specs still carry `TBD - created by archiving` purposes, and single-requirement specs (badge, scroll-pagination, input-rounded-border) add navigation cost without behavioral boundaries. Consolidating by domain makes specs reviewable and validates future changes against one home per concept.

## What Changes

- Consolidate ~70 existing spec capabilities into 9 domain specs by mechanical concatenation (no requirement rewrites in this change):
  - `solim-reactivity`, `solim-lifecycle`, `solim-layout`, `solim-styling`, `solim-widgets`, `services`, `chat`, `browsers`, `app-settings`.
- **BREAKING**: Delete the old fragmented spec directories after their content is concatenated into the merged home (single source of truth, no stubs).
- Carry `TBD` purposes forward verbatim; purpose rewrites and requirement dedupe are explicitly out of scope and tracked as follow-ups.
- Execute in 5 reviewable stages: (1) chat + browsers, (2) services, (3) reactivity + lifecycle, (4) layout + styling + widgets, (5) settings + tooling sweep.
- No runtime, API, or game-behavior changes; this change touches only `openspec/specs/`.

## Capabilities

### New Capabilities

- `solim-reactivity`: merged reactive core — signals, dispatcher, bindings, property bindings, two-way binding, callback cleanup, effect ownership registration.
- `solim-lifecycle`: merged component lifecycle and ownership — component model, auto-attach, base separation, disposal correctness, structural reconciler, dynamic subtrees, core split, runtime encapsulation, ownership APIs, scoped context.
- `solim-layout`: merged layout and modifiers — column/row/grid/wrap/flow/scroll, shared modifiers, element/table config mixins, pending cell config, modifier clarity rules, viewport units, grid item context.
- `solim-styling`: merged styling system — style values, rounded generation, border/background composition, web styles palette, card, badge, pure-component constraints, button purity.
- `solim-widgets`: merged widgets and overlays — widget set, input extensions, dialog, popup menu, tabs, hud, virtual list, scroll pagination, network image, animated loader, Arc interop facade, declarative UI facades.
- `services`: merged backend services — HTTP client, API models, MindustryTool API, Github service, auth service + session signal, query builder, update service.
- `chat`: merged chat feature — feature lifecycle, overlay, message group layout, settings, rounded input, optimistic send.
- `browsers`: merged content browsers — browser common chrome, map browser, schematic browser.
- `app-settings`: merged app settings — feature settings dialog, general settings dialog, development-flag stubs, quick access, config-value signals, orientation signal.

### Modified Capabilities

- None — no spec-level runtime behavior changes. Existing fragmented specs are removed (not modified) after concatenation; dedupe and rewrites are follow-up work.

## Impact

- Affects only `openspec/specs/` documentation structure and `openspec` validation scope (fewer, larger capabilities).
- No code, API, dependency, or Mindustry runtime impact.
- Risk is review load and requirement-name collisions surfacing during concatenation; mitigated by staging and concat-then-dedupe (collisions preserved visibly for follow-up).
