## MODIFIED Requirements

### Requirement: State Rendering and Transitions
`QueryView<T>` SHALL observe `query.loading()`, `query.error()`, `query.data()`, and `query.fetching()` to display the active state across all lifecycle transitions.

#### Scenario: Initial state
- **WHEN** `QueryView` is created before the query completes its initial fetch
- **THEN** it SHALL render the loading state

#### Scenario: Loading state
- **WHEN** `query.loading().get()` is true
- **THEN** `QueryView` SHALL render the component provided by `.loading(Supplier<Component>)`, or the default centered spinner if none is provided

#### Scenario: Success state
- **WHEN** `query.data().get()` is non-null and no error is present
- **THEN** `QueryView` SHALL render the component produced by `.data(Function<T, Component>)` or `.data(BiFunction<T, Boolean, Component>)`

#### Scenario: Error state
- **WHEN** `query.error().get()` is non-null and `query.data().get()` is null
- **THEN** `QueryView` SHALL render the component produced by `.error(Function<Throwable, Component>)`, or the default error view if none is provided

#### Scenario: Transition loading to success
- **WHEN** `query` successfully completes its initial load
- **THEN** `QueryView` SHALL unmount the loading component and mount the data component

#### Scenario: Transition loading to error
- **WHEN** `query` fails its initial load
- **THEN** `QueryView` SHALL unmount the loading component and mount the error component

#### Scenario: Transition success to refetch to fetching
- **WHEN** `query.refetch()` is called after a successful initial load
- **THEN** `QueryView` SHALL replace the data component with the loading spinner until the refetch completes, and pass `isFetching = true` to `.data((data, isFetching) -> ...)` where applicable

#### Scenario: Transition success to refetch to success
- **WHEN** a background refetch completes successfully with updated data
- **THEN** `QueryView` SHALL update the data component with the new data and `isFetching = false`

#### Scenario: Transition success to refetch to error
- **WHEN** a background refetch fails while previous data is already displayed
- **THEN** `QueryView` SHALL retain the previous data component on screen without flashing to an error screen

#### Scenario: Transition error to retry to success
- **WHEN** an initial error occurs, and `query.refetch()` is triggered via the retry action
- **THEN** `QueryView` SHALL show loading or fetching, and on success, mount the data component
