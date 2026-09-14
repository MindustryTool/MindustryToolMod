## ADDED Requirements

### Requirement: Merged solim-widgets concatenates widget and overlay sources verbatim
The merged `openspec/specs/solim-widgets/spec.md` SHALL contain, in order, the full Purpose bodies and every `### Requirement` block verbatim from `solim-widgets` (anchor first), then `animated-loader`, `solim-arc-interop-facade`, `solim-declarative-ui`, `solim-dialog`, `solim-hud`, `solim-input-extensions`, `solim-network-image`, `solim-popup-menu`, `solim-scroll-pagination`, `solim-tabs`, `solim-virtual-list`, each under a `**Source: <name>**` marker inside the single `## Requirements` section; per-source `## Purpose`/`## Requirements` header lines are removed so all requirements parse, and all other text is verbatim; `animated-loader` is single-homed here and SHALL NOT be duplicated into `browsers`.

#### Scenario: Requirement inventory is preserved
- **WHEN** the `### Requirement:` headers are counted in all 12 source specs and in the merged file
- **THEN** the merged count equals the source total and every source requirement title appears under its `## Source:` section

#### Scenario: Merged spec validates and sources are removed
- **WHEN** `openspec validate --strict` runs after the merge and `openspec/specs/` is listed
- **THEN** validation passes and the 11 non-anchor source directories no longer exist
