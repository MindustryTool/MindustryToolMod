## ADDED Requirements

### Requirement: Merged solim-reactivity concatenates reactive sources verbatim
The merged `openspec/specs/solim-reactivity/spec.md` SHALL contain, in order, the full Purpose bodies and every `### Requirement` block verbatim from `solim-reactivity` (anchor first), then `automatic-effect-ownership`, `config-value-signal`, `contextual-config-value`, `orientation-signal`, `signal-callback-cleanup`, `solim-binding`, `solim-property-bindings`, `solim-signal-dispatcher`, `two-way-binding`, each under a `**Source: <name>**` marker inside the single `## Requirements` section; per-source `## Purpose`/`## Requirements` header lines are removed so all requirements parse, and all other text is verbatim with TBD purposes carried forward.

#### Scenario: Requirement inventory is preserved
- **WHEN** the `### Requirement:` headers are counted in all 10 source specs and in the merged file
- **THEN** the merged count equals the source total and every source requirement title appears under its `## Source:` section

#### Scenario: Merged spec validates and sources are removed
- **WHEN** `openspec validate --strict` runs after the merge and `openspec/specs/` is listed
- **THEN** validation passes and the 9 non-anchor source directories no longer exist
