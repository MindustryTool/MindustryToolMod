## MODIFIED Requirements

### Requirement: HTTP Request Builder
The system SHALL provide a fluent `RequestBuilder` for constructing HTTP requests with configurable method, URL, headers, body, timeout, auth, and query parameters. Query parameters SHALL be accumulated via `.query()` overloads and assembled into the final URL at send time. Query parameter keys SHALL be required (throw `NullPointerException` if null). String values SHALL be skipped if null or empty. The `Map<String, String>` overload SHALL replace existing entries for each key present in the map. All other overloads SHALL append to the key's list.

#### Scenario: Single query parameter
- **WHEN** `request.get("/maps").query("page", 1).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?page=1`

#### Scenario: Multiple query parameters
- **WHEN** `request.get("/maps").query("page", 1).query("size", 20).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?page=1&size=20`

#### Scenario: Repeated query parameter
- **WHEN** `request.get("/maps").query("tags", "a").query("tags", "b").sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?tags=a&tags=b`

#### Scenario: List query parameter
- **WHEN** `request.get("/maps").query("tags", List.of("a", "b")).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?tags=a&tags=b`

#### Scenario: Map query parameter replaces
- **WHEN** `request.get("/maps").query("tags", "a").query(Map.of("tags", "b")).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?tags=b`

#### Scenario: Null key throws
- **WHEN** `request.get("/maps").query(null, "value")` is called
- **THEN** a `NullPointerException` SHALL be thrown

#### Scenario: Null String value is skipped
- **WHEN** `request.get("/maps").query("sort", null).sendAsync()` is called
- **THEN** the request URL SHALL be `/maps` (no `sort` parameter)

#### Scenario: Empty String value is skipped
- **WHEN** `request.get("/maps").query("sort", "").sendAsync()` is called
- **THEN** the request URL SHALL be `/maps` (no `sort` parameter)

#### Scenario: URL encoding
- **WHEN** `request.get("/maps").query("query", "hello world").sendAsync()` is called
- **THEN** the request URL SHALL contain `query=hello+world` or `query=hello%20world`

#### Scenario: Existing query string in URL
- **WHEN** `request.get("/maps?page=0").query("sort", "newest").sendAsync()` is called
- **THEN** the request URL SHALL be `/maps?page=0&sort=newest`
