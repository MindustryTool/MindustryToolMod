## ADDED Requirements

### Requirement: Raw reactive net-state signals
`Signals` SHALL expose raw mirrors of `Net` state as shared `Readable<Boolean>` values: `active()` reflecting `Vars.net.active()`, `server()` reflecting `Vars.net.server()`, and `client()` reflecting `Vars.net.client()`.

#### Scenario: Signals mirror Net on read
- **WHEN** `Vars.net.host(port)` has completed
- **THEN** `Signals.server().peek()` is true and `Signals.client().peek()` is false

#### Scenario: Client connection reflects immediately after refresh
- **WHEN** the client connects to a server and the refresh runs
- **THEN** `Signals.client().peek()` is true and `Signals.server().peek()` is false

#### Scenario: Disconnect returns to inactive
- **WHEN** the session disconnects or the server closes and the refresh runs
- **THEN** `Signals.active().peek()` is false with both `server()` and `client()` false

### Requirement: Derived net-state signals
`Signals` SHALL additionally expose derived values with exact documented predicates: `singlePlayer()` as `!active`, `localHosting()` as `server`, and `clientPlaying()` as `client && Vars.state.isGame()`. Raw signals SHALL stay menu-inclusive (no game-state folded in); game-awareness SHALL be composed at use sites.

#### Scenario: Menu counts as single-player
- **WHEN** no multiplayer session is active while sitting in the menu
- **THEN** `Signals.singlePlayer().peek()` is true

#### Scenario: Hosting lobby counts as local hosting
- **WHEN** the local server is up even before world play begins
- **THEN** `Signals.localHosting().peek()` is true

#### Scenario: Client in menu is not client-playing
- **WHEN** connected as a client but `Vars.state.isGame()` is false
- **THEN** `Signals.client().peek()` is true while `Signals.clientPlaying().peek()` is false

### Requirement: Event-plus-poll update driver
Net signals SHALL refresh on net/state lifecycle events (host, client connect, join/leave, state change, world load/reset, dispose) with a slow poll backstop so no transition path leaves them stale. All writes SHALL be posted to the app thread and SHALL set only on change so `Signal` dedupes equal values. Headless and dedicated-server behavior is explicitly out of scope and unspecified.

#### Scenario: Immediate refresh on host event
- **WHEN** a host lifecycle event fires
- **THEN** the signals recompute on the app thread without waiting for the poll

#### Scenario: Poll backstop corrects missed paths
- **WHEN** a transition occurs through a path with no event subscription
- **THEN** the next poll tick corrects the signals

#### Scenario: No emission without change
- **WHEN** a refresh computes values equal to the current ones
- **THEN** subscribers are not notified
