## MODIFIED Requirements

### Requirement: QueryView reconfiguration is scheduled, not executed inline
`QueryView` SHALL NOT invoke its view effect directly. Swapping a `data`, `loading`, or `error` factory after the view has been built SHALL schedule the view update through the reactive dispatcher, so that view construction never runs synchronously inside the caller's frame, inside another effect's execution, or inside a dispatcher flush that did not originate from it.

#### Scenario: Factory swap defers to the dispatcher
- **WHEN** `data(...)` is called on an already-built `QueryView`
- **THEN** the view update is enqueued for the next frame flush and is not executed inline

#### Scenario: View update runs exactly once per scheduled change
- **WHEN** multiple factory setters are called before the next flush
- **THEN** the view rebuilds at most once for that frame

#### Scenario: No re-entrant view rebuild during a flush
- **WHEN** a factory setter is invoked from inside another effect that is executing during a flush
- **THEN** the view update is scheduled rather than executed re-entrantly

### Requirement: QueryView default view strings are translatable
All user-visible strings rendered by `QueryView`'s built-in loading and error views SHALL resolve through the translation bundle. The default error message SHALL have a bundle key and SHALL NOT be a hardcoded literal. Bundle lookups SHALL NOT silently degrade to an English fallback in normal operation.

#### Scenario: Loading text resolves through the bundle
- **WHEN** the default loading view is rendered
- **THEN** its text is obtained from the bundle key for the loading state

#### Scenario: Default error message resolves through the bundle
- **WHEN** the default error view is rendered for a failure with no message
- **THEN** its text is obtained from a bundle key rather than a hardcoded literal

#### Scenario: Retry action label resolves through the bundle
- **WHEN** the default error view renders its retry action
- **THEN** the action label is obtained from a bundle key for the retry button

#### Scenario: Custom factories override the defaults
- **WHEN** a custom `loading`, `error`, or `data` factory is supplied
- **THEN** the default factories and their bundle keys are not used