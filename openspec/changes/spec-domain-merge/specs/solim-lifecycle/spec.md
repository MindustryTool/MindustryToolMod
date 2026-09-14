## ADDED Requirements

### Requirement: Merged solim-lifecycle concatenates lifecycle and ownership sources verbatim
The merged `openspec/specs/solim-lifecycle/spec.md` SHALL contain, in order, the full Purpose bodies and every `### Requirement` block verbatim from `solim-component` (anchor first), then `basecomponent-ui-separation`, `lifecycle-correctness`, `ownership-encapsulation`, `scoped-context-api`, `solim-automatic-ownership`, `solim-component-auto-attach`, `solim-core-split`, `solim-dynamic`, `solim-runtime-encapsulation`, `structural-reconciler`, `unified-ownership-api`, each under a `**Source: <name>**` marker inside the single `## Requirements` section; per-source `## Purpose`/`## Requirements` header lines are removed so all requirements parse, and all other text is verbatim with TBD purposes carried forward.

#### Scenario: Requirement inventory is preserved
- **WHEN** the `### Requirement:` headers are counted in all 12 source specs and in the merged file
- **THEN** the merged count equals the source total and every source requirement title appears under its `## Source:` section

#### Scenario: Merged spec validates and sources are removed
- **WHEN** `openspec validate --strict` runs after the merge and `openspec/specs/` is listed
- **THEN** validation passes and the 11 non-anchor source directories no longer exist
