## ADDED Requirements

### Requirement: Merged chat concatenates chat sources verbatim
The merged `openspec/specs/chat/spec.md` SHALL contain, in order, the full Purpose bodies and every `### Requirement` block verbatim from `chat-overlay` (anchor first), then `chat-feature`, `chat-input-rounded-border`, `chat-message-group-layout`, `chat-settings`, `optimistic-message-send`, each under a `**Source: <name>**` marker inside the single `## Requirements` section; per-source `## Purpose`/`## Requirements` header lines are removed so all requirements parse, and all other text is verbatim.

#### Scenario: Requirement inventory is preserved
- **WHEN** the `### Requirement:` headers are counted in all 6 source specs and in the merged file
- **THEN** the merged count equals the source total and every source requirement title appears under its `## Source:` section

#### Scenario: Merged spec validates and sources are removed
- **WHEN** `openspec validate --strict` runs after the merge and `openspec/specs/` is listed
- **THEN** validation passes and the 5 non-anchor source directories no longer exist
