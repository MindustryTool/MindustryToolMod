## ADDED Requirements

### Requirement: Merged browsers concatenates browser sources verbatim
The merged `openspec/specs/browsers/spec.md` SHALL contain, in order, the full Purpose bodies and every `### Requirement` block verbatim from `browser-common` (anchor first), then `map-browser`, `schematic-browser`, each under a `**Source: <name>**` marker inside the single `## Requirements` section; per-source `## Purpose`/`## Requirements` header lines are removed so all requirements parse, and all other text is verbatim with TBD purposes carried forward; loader integration scenarios stay in their browser files and the `animated-loader` component contract SHALL NOT be duplicated here.

#### Scenario: Requirement inventory is preserved
- **WHEN** the `### Requirement:` headers are counted in all 3 source specs and in the merged file
- **THEN** the merged count equals the source total and every source requirement title appears under its `## Source:` section

#### Scenario: Merged spec validates and sources are removed
- **WHEN** `openspec validate --strict` runs after the merge and `openspec/specs/` is listed
- **THEN** validation passes and the 2 non-anchor source directories no longer exist
