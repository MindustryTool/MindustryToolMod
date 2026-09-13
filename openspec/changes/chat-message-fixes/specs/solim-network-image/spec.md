## ADDED Requirements

### Requirement: Network Request Timeout and Request Headers
The `NetworkImage` component SHALL configure HTTP image requests with an extended timeout of at least 30,000ms (30 seconds) and a standard User-Agent header (`MindustryTool/<version>` or browser-compatible identifier) to prevent CDN throttling and premature download aborts on large images or slower connections.

#### Scenario: Image fetch with extended timeout and user agent
- **WHEN** an image URL is requested from an external CDN host
- **THEN** the HTTP request is executed with a timeout of at least 30,000ms and sends a valid User-Agent request header
