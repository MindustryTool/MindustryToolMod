## MODIFIED Requirements

### Requirement: Event-plus-poll update driver
Net signals SHALL refresh on net/state lifecycle events (host, client connect, join/leave, state change, world load/reset, dispose, resize). The system SHALL NOT run a periodic polling timer. Because the event list is relied upon to be exhaustive, the system SHALL additionally expose an explicit public refresh trigger that gameplay code can invoke when it detects a net transition that fired none of the subscribed events. All writes SHALL be posted to the app thread and SHALL set only on change so `Signal` dedupes equal values. Headless and dedicated-server behavior is explicitly out of scope and unspecified.

#### Scenario: Immediate refresh on host event
- **WHEN** a host lifecycle event fires
- **THEN** the signals recompute on the app thread immediately

#### Scenario: Explicit trigger corrects an event-less transition
- **WHEN** a net transition occurs through a path that fires no subscribed event and gameplay code calls `Signals.requestRefresh()`
- **THEN** the signals recompute on the app thread and reflect the new net state

#### Scenario: Refresh trigger is publicly reachable
- **WHEN** `Signals.requestRefresh()` is called from gameplay code
- **THEN** it is a public method that schedules the same refresh the event handlers schedule

#### Scenario: No periodic timer is registered
- **WHEN** the `Signals` class is loaded
- **THEN** no recurring `Timer` task is registered and no work is performed on a wall-clock interval

#### Scenario: No emission without change
- **WHEN** a refresh computes values equal to the current ones
- **THEN** subscribers are not notified