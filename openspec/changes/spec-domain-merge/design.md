## Context

`openspec/specs/` contains ~70 capabilities; ~90 archived changes each fossilized one fragment into a spec directory. The result is systematic duplication: ownership is described in 5 specs (`automatic-effect-ownership`, `solim-automatic-ownership`, `unified-ownership-api`, `ownership-encapsulation`, `scoped-context-api`); table/element modifiers in 4 (`solim-shared-modifiers`, `element-config-mixin`, `table-config-mixin`, `modifier-clarity`); reactivity in 6+. Seventeen specs still have `TBD - created by archiving` purposes. The largest anchors (`chat-overlay` 17k, `http-client` 12.7k, `solim-layout` 11.7k) already overlap their satellites — e.g. `solim-shared-modifiers/spec.md` restates mixin requirements nearly verbatim. Stakeholders: anyone writing or validating future OpenSpec changes. Constraint from confirmation: mechanical concat-then-dedupe, delete old dirs, carry TBDs forward, 5 stages. No code or runtime behavior is in scope.

## Goals / Non-Goals

**Goals:**
- Reduce 70 fragmented specs to 9 domain specs with a deterministic, reviewable merge procedure per stage.
- Preserve every existing requirement and scenario verbatim in the merge (no silent drops, no rewrites).
- Make collisions and duplications visible in the merged output so follow-up dedupe has a complete inventory.
- Keep each stage independently reviewable and revertible via git.

**Non-Goals:**
- Rewriting purposes (the 17 TBDs are carried forward as-is).
- Deduplicating or reconciling overlapping requirements (explicit follow-up, not this change).
- Renaming requirements/scenarios, changing SHALL semantics, or altering any game/runtime behavior.
- Keeping stub/redirect specs at old paths; old dirs are deleted.

## Decisions

### Decision: 9 fixed domain homes with anchor-first ordering
Each merged spec lives at `openspec/specs/<domain>/spec.md` and concatenates sources anchor-first (largest spec first, it sets section order), then remaining sources alphabetically. Rationale: anchors (`chat-overlay`, `http-client`, `solim-layout`, `solim-reactivity`, `solim-component`, `solim-styling`, `solim-widgets`, `browser-common`, `feature-settings-dialog`) already define the dominant section vocabulary; alphabetical tail is deterministic and diff-stable. Alternative (pure alphabetical) was rejected because it buries the anchor's structure.

Source map (confirmed 9-domain cut):

| Merged home | Sources concatenated (anchor ★ first) |
|---|---|
| `solim-reactivity` | `solim-reactivity`★, `automatic-effect-ownership`, `config-value-signal`, `contextual-config-value`, `orientation-signal`, `signal-callback-cleanup`, `solim-binding`, `solim-property-bindings`, `solim-signal-dispatcher`, `two-way-binding` |
| `solim-lifecycle` | `solim-component`★, `basecomponent-ui-separation`, `lifecycle-correctness`, `ownership-encapsulation`, `scoped-context-api`, `solim-automatic-ownership`, `solim-component-auto-attach`, `solim-core-split`, `solim-dynamic`, `solim-runtime-encapsulation`, `structural-reconciler`, `unified-ownership-api` |
| `solim-layout` | `solim-layout`★, `element-config-mixin`, `grid-item-context`, `modifier-clarity`, `pending-cell-config`, `solim-shared-modifiers`, `solim-units`, `table-config-mixin`, `wrap-flow-layout` |
| `solim-styling` | `solim-styling`★, `pure-button-component`, `pure-solim-components`, `solim-badge`, `solim-card`, `solim-rounded`, `unified-border-background`, `web-styles` |
| `solim-widgets` | `solim-widgets`★, `animated-loader`, `solim-arc-interop-facade`, `solim-declarative-ui`, `solim-dialog`, `solim-hud`, `solim-input-extensions`, `solim-network-image`, `solim-popup-menu`, `solim-scroll-pagination`, `solim-tabs`, `solim-virtual-list` |
| `services` | `http-client`★, `api-models`, `auth-service`, `auth-session-signal`, `github-service`, `mindustrytool-api`, `request-query-builder`, `update-service` |
| `chat` | `chat-overlay`★, `chat-feature`, `chat-input-rounded-border`, `chat-message-group-layout`, `chat-settings`, `optimistic-message-send` |
| `browsers` | `browser-common`★, `map-browser`, `schematic-browser` |
| `app-settings` | `feature-settings-dialog`★, `feature-development-flag`, `general-settings-dialog`, `quick-access` (config primitives single-homed in `solim-reactivity`, listed as related only) |

### Decision: concat-then-dedupe with provenance markers and header remap, no content edits
Each merged file has one `# <domain> Specification` title, one merge-note `## Purpose`, and one `## Requirements` section. Each source contributes its purpose body and all `### Requirement` blocks verbatim in original order under a bold `**Source: <name>**` marker. Per-source `## Purpose` / `## Requirements` (or `## ADDED Requirements`) header lines are removed — this is a mechanical structural necessity, discovered during implementation: `openspec validate --strict` parses requirements only inside a single `## Requirements` section, and parses every `### `-prefixed line as a requirement, so `## Source:` markers (v1 format) and `### Source:` markers (v2 format) both fail validation. No requirement or scenario text is altered, merged, or dropped — even known duplicates (e.g. gap semantics in shared-modifiers vs mixins). Rationale: mechanical merges are reviewable; any editorial pass would hide drops. Alternative (reconcile-as-we-go) was explicitly rejected in confirmation.

### Decision: cross-cutting specs are single-homed by primary owner
`animated-loader` → `solim-widgets` (component owner; browsers keep only integration scenarios already in their own files, nothing is copied across). `config-value-signal` / `contextual-config-value` → `solim-reactivity` holds the primitive contract; `app-settings` references it but does not duplicate — the one exception is these two specs are concatenated into `solim-reactivity` only, and `app-settings` lists them as related, not copied. `automatic-effect-ownership` → `solim-reactivity` (registration mechanism), while lifecycle aspects stay covered by `solim-automatic-ownership` in `solim-lifecycle`; no spec file is split — whole-file homing only. Rationale: splitting files mid-merge destroys provenance; single-homing keeps `git log --follow` traceable.

### Decision: delete old dirs in the same stage commit, no stubs
Each stage ends with `git rm` of consumed source dirs. Rationale: confirmed choice; stubs would double validation scope and invite edits in two places. Rollback is `git revert` of the stage commit.

### Decision: 5 stages in dependency-light order
Stage 1 chat+browsers (pilot, self-contained) → Stage 2 services → Stage 3 reactivity+lifecycle (hardest) → Stage 4 layout+styling+widgets (largest) → Stage 5 settings sweep. Rationale: learn the pattern on low-overlap domains before touching framework core; each stage is a separately reviewable commit. Alternative (single stage) was rejected — one 70-dir diff is unreviewable.

## Risks / Trade-offs

- [Risk] Requirement header collisions (three `Requirement: ...gap...` with different semantics) confuse future readers → Mitigation: collisions preserved verbatim with `## Source:` markers; follow-up dedupe task inventories them explicitly.
- [Risk] Merged files get large (`solim-widgets` ≈ 30k chars, `solim-lifecycle` ≈ 30k) hurting readability → Mitigation: accepted trade-off of domain consolidation; internal `## Source:` sections preserve navigation until dedupe shrinks them.
- [Risk] Accidental requirement drop during file moves → Mitigation: per-stage count check (source `### Requirement:` count equals merged count) recorded in tasks; `openspec validate --strict` per stage.
- [Risk] Deleting old dirs breaks in-flight changes referencing old spec paths → Mitigation: no active changes exist (`openspec list` empty); archive/ history untouched.
- [Risk] TBD purposes carried forward look unfinished → Mitigation: intentional per confirmation; follow-up task filed, not silently fixed here.
