# test-isolation Specification

## Purpose

Audit rule for choosing `ArcTestEnv` vs `SolimEnv`, background-sync guard in tests, and verification procedure for flaky teardown. Created by archiving change test-isolation-fix.

## Requirements

### Requirement: Pure-logic test base class
Tests that use no solim reactive APIs SHALL extend `ArcTestEnv` instead of `SolimEnv`.

#### Scenario: RequestTest uses ArcTestEnv
- **WHEN** `RequestTest` runs in the full `:mod:test` suite
- **THEN** no `SolimEnv` teardown assertion executes for it and its HTTP assertions determine pass/fail

#### Scenario: Signal tests keep strict teardown
- **WHEN** a test imports `solim.reactive`/`solim.runtime` or calls `flushEffects`/`createSignal`/`listen`
- **THEN** it extends `SolimEnv` (or `MindustryTestEnv`) and the `SignalDispatcher` empty-queue assertion still applies

---

### Requirement: Prompt cancellation of incidental background requests
Tests that incidentally start real network requests SHALL cancel them via `service.stop()` immediately after firing, before asserting, so no late `Request-Worker` completion can touch solim state during a later test's teardown window.

#### Scenario: Sync tests cancel before asserting
- **WHEN** a `ChatServiceWatchdogTest` sync test fires `syncActiveChannelSilently`
- **THEN** it calls `service.stop()` before asserting the synchronous loading flags, making the assertions independent of network speed

#### Scenario: Reconnect test cancels before asserting
- **WHEN** `testCheckConnectionAndReconnectCancelsStalledStream` fires the reconnect's stream and catch-up requests
- **THEN** it calls `service.stop()` before asserting, so those requests never complete outside the owning test

#### Scenario: Localhost test servers still work
- **WHEN** `RequestTest` starts its `127.0.0.1` `HttpServer`
- **THEN** all 31 tests execute against localhost unaffected by the guard
