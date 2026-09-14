## 1. Stage 1 pilot — chat + browsers (mechanical concat)

- [x] 1.1 Concatenate `chat-overlay` + 5 chat sources into `openspec/specs/chat/spec.md` with `**Source:` markers, anchor first, per-source `## Purpose`/`## Requirements` headers removed (strict-validation format)
- [x] 1.2 Concatenate `browser-common` + `map-browser` + `schematic-browser` into `openspec/specs/browsers/spec.md`, no `animated-loader` duplication
- [x] 1.3 Verify requirement counts match source totals, run `openspec validate --strict`, delete the 7 consumed source dirs

## 2. Stage 2 — services (8→1)

- [x] 2.1 Concatenate `http-client` + 7 service sources into `openspec/specs/services/spec.md` with `**Source:` markers, anchor first, TBDs carried forward
- [x] 2.2 Verify requirement counts match, run `openspec validate --strict`, delete the 7 consumed source dirs

## 3. Stage 3 — framework core (reactivity + lifecycle)

- [x] 3.1 Concatenate `solim-reactivity` + 9 sources into `openspec/specs/solim-reactivity/spec.md`, single-homing `automatic-effect-ownership` and config primitives here
- [x] 3.2 Concatenate `solim-component` + 11 lifecycle sources into `openspec/specs/solim-lifecycle/spec.md`, no file splitting
- [x] 3.3 Verify requirement counts match both merges, run `openspec validate --strict`, delete the 20 consumed source dirs

## 4. Stage 4 — layout + styling + widgets (largest win)

- [x] 4.1 Concatenate `solim-layout` + 8 sources into `openspec/specs/solim-layout/spec.md`, preserving gap-duplication visibly
- [x] 4.2 Concatenate `solim-styling` + 7 sources into `openspec/specs/solim-styling/spec.md`
- [x] 4.3 Concatenate `solim-widgets` + 11 sources into `openspec/specs/solim-widgets/spec.md`, single-homing `animated-loader` here
- [x] 4.4 Verify requirement counts match all three merges, run `openspec validate --strict`, delete the 26 consumed source dirs

## 5. Stage 5 — settings sweep + closeout

- [x] 5.1 Concatenate `feature-settings-dialog` + 3 settings sources into `openspec/specs/app-settings/spec.md`, listing config primitives as related not duplicated
- [x] 5.2 Verify requirement counts match, run `openspec validate --strict`, delete the 3 consumed source dirs
- [x] 5.3 Final `openspec validate --strict` over 9 merged specs, confirm no orphan source dirs remain, file follow-ups for dedupe and TBD purpose rewrites
