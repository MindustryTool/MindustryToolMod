## ADDED Requirements

### Requirement: Merged solim-layout concatenates layout and modifier sources verbatim
The merged `openspec/specs/solim-layout/spec.md` SHALL contain, in order, the full Purpose bodies and every `### Requirement` block verbatim from `solim-layout` (anchor first), then `element-config-mixin`, `grid-item-context`, `modifier-clarity`, `pending-cell-config`, `solim-shared-modifiers`, `solim-units`, `table-config-mixin`, `wrap-flow-layout`, each under a `**Source: <name>**` marker inside the single `## Requirements` section; per-source `## Purpose`/`## Requirements` header lines are removed so all requirements parse, and all other text is verbatim, preserving known duplications (e.g. gap semantics) for follow-up dedupe.

#### Scenario: Requirement inventory is preserved
- **WHEN** the `### Requirement:` headers are counted in all 9 source specs and in the merged file
- **THEN** the merged count equals the source total and every source requirement title appears under its `## Source:` section

#### Scenario: Merged spec validates and sources are removed
- **WHEN** `openspec validate --strict` runs after the merge and `openspec/specs/` is listed
- **THEN** validation passes and the 8 non-anchor source directories no longer exist
