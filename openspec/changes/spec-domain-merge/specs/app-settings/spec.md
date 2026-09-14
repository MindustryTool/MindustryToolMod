## ADDED Requirements

### Requirement: Merged app-settings concatenates settings sources verbatim
The merged `openspec/specs/app-settings/spec.md` SHALL contain, in order, the full Purpose bodies and every `### Requirement` block verbatim from `feature-settings-dialog` (anchor first), then `feature-development-flag`, `general-settings-dialog`, `quick-access`, each under a `**Source: <name>**` marker inside the single `## Requirements` section; per-source `## Purpose`/`## Requirements` header lines are removed so all requirements parse, and all other text is verbatim; reactive config primitives (`config-value-signal`, `contextual-config-value`, `orientation-signal`) are single-homed in `solim-reactivity` and SHALL be listed as related, not duplicated here.

#### Scenario: Requirement inventory is preserved
- **WHEN** the `### Requirement:` headers are counted in all 4 source specs and in the merged file
- **THEN** the merged count equals the source total and every source requirement title appears under its `## Source:` section

#### Scenario: Merged spec validates and sources are removed
- **WHEN** `openspec validate --strict` runs after the merge and `openspec/specs/` is listed
- **THEN** validation passes and the 3 non-anchor source directories no longer exist
