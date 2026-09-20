## ADDED Requirements

### Requirement: Query-Cached Text Translation
The translation feature SHALL cache translation requests and deduplicate concurrent requests using `QueryCache` with deterministic keys derived from provider ID, target language, and sanitized source text.

#### Scenario: Identical translation requests return cached result
- **WHEN** multiple requests to translate the same text to the same target language are made with the same translation provider
- **THEN** the translation provider network API is invoked only once, and subsequent or concurrent callers receive the cached or in-flight result without making redundant network requests

#### Scenario: Different target languages or providers create distinct cache entries
- **WHEN** the same text is translated to different target languages or with different translation providers
- **THEN** distinct `QueryKey` instances are evaluated and independent translations are fetched and cached
