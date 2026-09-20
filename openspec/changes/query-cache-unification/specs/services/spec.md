## ADDED Requirements

### Requirement: Github Uses Unified Cache Path
`Github` metadata getters SHALL use the unified `QueryCache` fetch policy so startup prefetch warming is effective and duplicate callers share one network request.

#### Scenario: Prefetched releases served without refetch
- **WHEN** `prefetchAll` has warmed `QueryKey.of("github", "releases")` and it is still fresh
- **THEN** `getReleases()` SHALL return the cached body without issuing a new HTTP request

### Requirement: Translation Direct Fetch
Translation calls SHALL NOT retain long-term `QueryCache` entries. `TranslationFeature.translate` and `MindustryTool.translate` SHALL call the provider directly on every invocation, guarding only against duplicate concurrent taps via UI disablement.

#### Scenario: Repeated phrase re-fetches
- **WHEN** the same phrase is translated twice in separate actions
- **THEN** two provider requests SHALL be issued and no `QueryKey.of("translation", ...)` entry SHALL be retained afterwards

#### Scenario: Empty text short-circuits
- **WHEN** `translate` is called with null or blank text
- **THEN** it SHALL return the input (or empty string) without a network request
