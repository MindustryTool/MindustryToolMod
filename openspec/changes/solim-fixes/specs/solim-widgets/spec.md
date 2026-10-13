## MODIFIED Requirements

### Requirement: Reactive bindings read their source once per run
A reactive binding installed by a widget modifier SHALL evaluate its source exactly once per run. Where the binding guards a null result before applying a value, the guarded value SHALL be the same value that was tested, not a second independent read.

#### Scenario: Nullable source is read once
- **WHEN** a nullable reactive source drives a binding and the run applies a fallback for a `null` value
- **THEN** the source is read exactly once during that run

#### Scenario: Non-null source is read once
- **WHEN** a non-null reactive source drives a binding
- **THEN** the source is read exactly once during that run

#### Scenario: Computed source is not evaluated twice
- **WHEN** a lazily evaluated reactive source drives a binding
- **THEN** its supplier is invoked at most once per binding run

#### Scenario: Guard tests the applied value
- **WHEN** a binding reads a source and guards on the result
- **THEN** the value applied is the same instance that was tested for null

### Requirement: Widget display types have unambiguous names
Where two distinct widget display types would otherwise share a simple class name across packages in the same module, they SHALL be given distinct names so that imports, documentation, and tooling can refer to each unambiguously.

#### Scenario: Distinct types have distinct simple names
- **WHEN** the framework's widget display types are enumerated
- **THEN** no two distinct types share the same simple name

#### Scenario: Import resolution is unambiguous
- **WHEN** a caller imports a widget display type by its simple name
- **THEN** exactly one type in the framework matches that simple name, or the caller must qualify it

#### Scenario: Existing behaviour of each type is unchanged
- **WHEN** a renamed display type is used through its public facade
- **THEN** its rendering and reactive behaviour are identical to before the rename