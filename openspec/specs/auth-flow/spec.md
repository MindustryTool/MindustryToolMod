# auth-flow Specification

## Purpose
TBD - created by archiving change fix-login-flow-error. Update Purpose after archive.
## Requirements
### Requirement: Main Thread Post-Fetch Dispatch
The authentication provider SHALL dispatch all post-fetch state persistence, event dispatching, token polling initialization, and platform interactions onto Mindustry's main thread via `Core.app.post`.

#### Scenario: Login URI fetch succeeds
- **WHEN** `MindustryTool.getLoginUri()` successfully returns a `LoginUriResponse`
- **THEN** persisting `KEY_LOGIN_ID`, setting `KEY_LOGIN_EXPIRY`, firing `LoginUriEvent`, starting token polling, and opening the browser SHALL execute inside a `Core.app.post` runnable

### Requirement: Fault-Tolerant Browser Launch
The authentication provider SHALL isolate system browser invocation so that failures to open the URI do not abort active token polling or reject the login future.

#### Scenario: System browser fails to open or throws
- **WHEN** `Core.app.openURI(loginUrl)` returns false or throws an exception
- **THEN** the system SHALL log a warning, copy `loginUrl` to the clipboard, show the `auth.login.browser-fallback` notice, and continue token polling without rejecting `loginFuture`

### Requirement: Diagnostic Error Logging and Root Cause Presentation
The authentication service and overlay SHALL record full failure traces and present the unwrapped root cause in user-facing failure prompts.

#### Scenario: Login flow encounters an unhandled exception
- **WHEN** an unhandled exception occurs while starting the login flow
- **THEN** the error SHALL be logged via `Log.err` with its full throwable and stack trace, and `AuthOverlay` SHALL unwrap the throwable down to its root cause when presenting failure details to the player

