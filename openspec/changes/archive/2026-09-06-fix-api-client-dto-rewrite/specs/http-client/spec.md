## ADDED Requirements

### Requirement: Generic async HTTP helpers in Request

`Request` SHALL provide static async helpers for Java 17 `HttpClient` that are the sole HTTP boundary for the rewrite. Callers in `mindustrytool.services` MUST NOT construct `HttpClient` or `HttpRequest` directly.

#### Scenario: Unauthenticated GET returns response
- **WHEN** `Request.get(url)` is called
- **THEN** it builds a `GET` `HttpRequest` with timeout `TIMEOUT_MS`, sends via shared `HttpClient`, and returns `CompletableFuture<HttpResponse<String>>` using `BodyHandlers.ofString()`

#### Scenario: GET with custom timeout
- **WHEN** `Request.get(url, timeoutMs)` is called
- **THEN** the request uses the supplied timeout in `Duration.ofMillis(timeoutMs)`

#### Scenario: GET bytes for binary data
- **WHEN** `Request.getBytes(url)` is called
- **THEN** it returns `CompletableFuture<HttpResponse<byte[]>>` via `BodyHandlers.ofByteArray()`

#### Scenario: POST JSON
- **WHEN** `Request.post(url, jsonBody)` is called
- **THEN** it sends a `POST` with header `Content-Type: application/json` and body `BodyPublishers.ofString(jsonBody)`

#### Scenario: PUT and DELETE helpers
- **WHEN** `Request.put(url, jsonBody)` or `Request.delete(url)` is called
- **THEN** they send the corresponding method with JSON header for PUT and return `CompletableFuture<HttpResponse<String>>`

### Requirement: Authenticated helpers auto-refresh token

`Request` SHALL provide `authGet`, `authPost`, `authPut`, `authDelete`, and `authUpload` that refresh the auth token before sending and attach `Authorization: Bearer <token>` when present. `Request` MUST NOT import `old.*`; it SHALL depend on a new token provider in `mindustrytool.*`.

#### Scenario: authGet refreshes and attaches token
- **WHEN** `Request.authGet(url)` is called and a token provider is configured
- **THEN** it first completes `refreshIfNeeded()`, then builds a `GET` request with `Authorization` header if `getToken()` returns non-null, and sends via `HttpClient`

#### Scenario: authPost with JSON and auth
- **WHEN** `Request.authPost(url, jsonBody)` is called
- **THEN** it refreshes, attaches token, sets `Content-Type: application/json`, and posts the body

#### Scenario: No token still sends unauthenticated
- **WHEN** `Request.authGet` is called and the token provider returns null
- **THEN** the request is sent without `Authorization` header and still returns a future

### Requirement: Multipart upload helper

`Request` SHALL provide `authUpload(url, fileBytes, fileName, hash)` that builds a `multipart/form-data` body with `hash` and `file` parts and validates `200`/`201` responses.

#### Scenario: Successful upload
- **WHEN** `authUpload` is called with file bytes
- **THEN** it builds a boundary `----MindustryTool<millis>`, constructs multipart body, posts with `Content-Type: multipart/form-data; boundary=...` and auth header, and completes normally when status is 200 or 201

#### Scenario: Upload failure surfaces exception
- **WHEN** the upload response status is not 200/201
- **THEN** the future completes exceptionally with `RuntimeException` containing HTTP code and body

### Requirement: Import hygiene for Request

`Request` SHALL use explicit imports for all `java.net.http` and `java.time` types (e.g. `import java.net.http.HttpResponse.BodyHandlers`, `import java.net.http.HttpRequest`, `import java.time.Duration`) and MUST NOT use fully-qualified inline class names inside method bodies.

#### Scenario: No fully-qualified Http types in method bodies
- **WHEN** `src/mindustrytool/services/Request.java` is inspected
- **THEN** it contains `import java.net.http.HttpRequest` etc. and bodies reference `HttpRequest`, `BodyHandlers`, `BodyPublishers`, `Duration` without `java.net.http.*` prefixes
