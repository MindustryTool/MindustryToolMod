# request-retry Specification

## Purpose
Specifies HTTP request retry strategies, exponential backoff algorithms, method idempotency rules, and override policies for `mindustrytool.services.Request`.
## Requirements
### Requirement: Default Retry on HTTP 5xx Status
The HTTP client `Request` SHALL automatically retry requests when receiving an HTTP response with status code >= 500 up to 3 times with exponential backoff before failing exceptionally.

#### Scenario: Server returns 500 then succeeds
- **WHEN** a GET request receives an HTTP 500 response on the initial attempt and an HTTP 200 response on the first retry
- **THEN** `Request` SHALL wait 200ms, retry the request, and complete the future successfully with the 200 response

#### Scenario: Server continuously returns 503 until retry exhaustion
- **WHEN** a GET request receives HTTP 503 on the initial attempt and all 3 retries
- **THEN** `Request` SHALL attempt 4 requests total (1 initial + 3 retries with 200ms, 400ms, 800ms delays) and complete the future exceptionally with `HttpException` (status code 503)

### Requirement: No Retry on Client Errors and Network Exceptions
The default retry strategy SHALL NOT retry client error responses (HTTP 400..499) or non-HTTP network exceptions.

#### Scenario: Server returns HTTP 404
- **WHEN** a GET request receives an HTTP 404 response
- **THEN** `Request` SHALL NOT retry and SHALL complete the future exceptionally with `HttpException` (status code 404) immediately

#### Scenario: Connection reset or network failure
- **WHEN** a GET request fails due to an `IOException` (e.g. connection refused or network unreachable)
- **THEN** `Request` SHALL NOT retry and SHALL complete the future exceptionally immediately

### Requirement: Method Idempotency Protection
The default retry strategy SHALL only retry idempotent HTTP methods (`GET`, `PUT`, `DELETE`, `HEAD`) by default, and SHALL NOT retry `POST` requests unless explicitly opted in.

#### Scenario: POST request fails with 500 without explicit retry opt-in
- **WHEN** a standard POST request receives an HTTP 500 response
- **THEN** `Request` SHALL NOT retry and SHALL complete exceptionally immediately

#### Scenario: POST request with explicit retry opt-in
- **WHEN** a POST request configured with `.retry(true)` receives an HTTP 500 response on the initial attempt and HTTP 200 on retry
- **THEN** `Request` SHALL retry the POST request with the exponential backoff delay and complete successfully

### Requirement: Retry Overrides and Customization
Callers SHALL be able to disable retries, override retry settings per-request, or configure a custom `RetryStrategy`.

#### Scenario: Request configured with noRetry
- **WHEN** a GET request configured with `.noRetry()` receives an HTTP 500 response
- **THEN** `Request` SHALL NOT retry and SHALL complete exceptionally immediately

#### Scenario: Custom RetryStrategy provided
- **WHEN** a request is configured with a custom `RetryStrategy` returning a specific delay
- **THEN** `Request` SHALL use the custom strategy's decision and delay

