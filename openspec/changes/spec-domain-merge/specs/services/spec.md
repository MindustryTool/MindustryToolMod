## ADDED Requirements

### Requirement: Merged services concatenates backend service sources verbatim
The merged `openspec/specs/services/spec.md` SHALL contain, in order, the full Purpose bodies and every `### Requirement` block verbatim from `http-client` (anchor first), then `api-models`, `auth-service`, `auth-session-signal`, `github-service`, `mindustrytool-api`, `request-query-builder`, `update-service`, each under a `**Source: <name>**` marker inside the single `## Requirements` section; per-source `## Purpose`/`## Requirements` header lines are removed so all requirements parse, and all other text is verbatim with TBD purposes carried forward.

#### Scenario: Requirement inventory is preserved
- **WHEN** the `### Requirement:` headers are counted in all 8 source specs and in the merged file
- **THEN** the merged count equals the source total and every source requirement title appears under its `## Source:` section

#### Scenario: Merged spec validates and sources are removed
- **WHEN** `openspec validate --strict` runs after the merge and `openspec/specs/` is listed
- **THEN** validation passes and the 7 non-anchor source directories no longer exist
