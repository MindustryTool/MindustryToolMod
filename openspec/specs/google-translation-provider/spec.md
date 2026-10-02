# google-translation-provider Specification

## Purpose
Specifies the keyless Google Web Translation provider (`translate_a/single`), including request throttling, in-memory LRU caching, User-Agent header spoofing, and resilient error parsing.

## Requirements

### Requirement: Keyless Translation via Google Web API
The system SHALL provide a translation provider that utilizes the Google Web Translation API without requiring user-provided API keys.

#### Scenario: Translating text successfully
- **WHEN** the translation provider receives a valid translation request
- **THEN** it makes an HTTP GET request to `translate.googleapis.com` with `client=gtx`, the target language, and the URL-encoded text.
- **AND** it extracts the translated text from the nested JSON array response.

### Requirement: Request Throttling
The provider SHALL limit the frequency of outgoing HTTP requests to avoid IP-based rate limiting.

#### Scenario: Rapid consecutive requests
- **WHEN** multiple translation requests are made within a small time window (e.g., 500ms)
- **THEN** subsequent requests are delayed or queued to ensure the minimum time interval between requests is respected.

### Requirement: Translation Caching
The provider SHALL cache recent translation results in memory to avoid duplicate network requests for the same text and target language.

#### Scenario: Requesting translation for previously translated text
- **WHEN** a translation request is made for text that exists in the cache for the target language
- **THEN** the cached translation is returned immediately without making an HTTP request.

### Requirement: User-Agent Spoofing
The provider SHALL send a standard web browser User-Agent header in its HTTP requests to bypass basic anti-bot blocks.

#### Scenario: API request headers
- **WHEN** the provider sends an HTTP request
- **THEN** the request includes a `User-Agent` header mimicking a standard desktop browser.

### Requirement: Graceful Error Handling
The provider SHALL handle errors gracefully, falling back to the original text or providing a clear error message when the API structure changes or rate limits are hit.

#### Scenario: Hitting Google rate limits (429)
- **WHEN** the Google API returns an HTTP 429 status code
- **THEN** the provider surfaces a clear error indicating the rate limit was reached.

#### Scenario: Unexpected JSON structure
- **WHEN** the Google API returns a 200 OK but the JSON structure does not match the expected nested array
- **THEN** the provider returns the original text instead of crashing.
